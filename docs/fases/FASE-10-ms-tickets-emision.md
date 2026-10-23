# Fase 10 — `ms-tickets`: emisión asíncrona con firma HMAC

**Estado:** ✅ Completada · **Fecha:** 23/10/2026
**Cubre:** CU-07 · **Implementa:** RN-14 (condiciones en la entrada) · **Resuelve:** PRB-05, PRB-06

## 1. En una frase

> Al pagarse una orden, `ms-tickets` emite **solo** las entradas, cada una con un código UUID y una **firma HMAC-SHA256**, y la orden
> pasa sola a `EMITIDA`. Si `ms-tickets` está apagado, **la compra no se entera**: el mensaje espera en la cola y se emite al volver.

## 2. Qué se creó

| Archivo | Para qué |
|---|---|
| `ms-teletickets/resources/schema.sql` | `ticket` (`UNIQUE(codigo)`, `UNIQUE(orden_id, numero)`), `validacion` (vacía hasta la Fase 14) y `mensaje_procesado` |
| `ms-teletickets/entity/Ticket`, `EstadoTicket`, `MensajeProcesado` | Sin setters; las entradas se crean con SQL propio |
| `ms-teletickets/repository/TicketRepository` | `emitir` = `INSERT … ON CONFLICT (orden_id, numero) DO NOTHING`: la idempotencia |
| `ms-teletickets/service/FirmaService` | `firma = HMAC-SHA256(codigo + eventoId, clave)`; `esValida` compara en tiempo constante (la usará la Fase 14) |
| `ms-teletickets/service/EmisionService` | Condiciones → entradas → `tickets.emitidos` → anotar el mensaje |
| `ms-teletickets/listener/OrdenPagadaListener` | Ack manual y **3 reintentos con espera 1 s, 2 s, 4 s** |
| `ms-teletickets/client/EventoClient` · `config/*` | Feign a `GET /internal/eventos/{id}/condiciones`; declara `tickets.orden-pagada` y `ordenes.tickets-emitidos` |
| `ms-teletickets/pom.xml` · `application.properties` | JPA, PostgreSQL, AMQP, OpenFeign, `mensajeria-comun` · `DB_NAME=db_tickets` (DEC-01) · `teleticket.tickets.clave-firma` |
| `ms-ordenes/service/EmisionService` · `listener/TicketsEmitidosListener` | Consume `tickets.emitidos`: `PAGADA → EMITIDA` con su fila en `orden_historial` |
| `ms-ordenes/config/RabbitConfig` · `application.properties` | Declara también `tickets.orden-pagada` y `ordenes.tickets-emitidos`; ack manual |

No hay endpoints públicos nuevos: la emisión es 100 % por mensajes.

## 3. Cómo lo compruebo

> **Antes:** `.\detener-todo.ps1`, compilar `ms-teletickets` y `ms-ordenes` (`.\mvnw.cmd -o clean package -DskipTests`), `.\arrancar-todo.ps1`.
> Recorrido de compra de las fases 7 a 9; luego **no hagas nada más**.

```powershell
docker exec teleticket-postgres psql -U teleticket -d db_tickets -c "SELECT numero, codigo, substring(firma,1,16) AS firma, estado FROM ticket;"
docker exec teleticket-postgres psql -U teleticket -d db_ordenes -c "SELECT codigo, estado FROM orden;"
```

| Prueba | Resultado |
|---|---|
| **Camino feliz**: orden de 2 entradas pagada | ✅ 2 entradas `EMITIDO` con código y firma de 64 caracteres; orden `EMITIDA` en menos de 3 s |
| `orden_historial` | ✅ `→ RESERVADA → PENDIENTE_PAGO → PAGADA → EMITIDA`, la última con «2 entradas emitidas con firma digital» |
| **Firma (PRB-05)**: recalculada con otro programa (`HMACSHA256` de .NET) y la misma clave | ✅ coincide byte a byte con las dos firmas guardadas |
| **Condiciones (PRB-06)** | ✅ `condiciones_ingreso` de cada entrada es el texto que se puso al crear el evento |
| **`orden.pagada` repetido** a mano desde la API de RabbitMQ (con `eventoId` de mensaje nuevo) | ✅ «ya tenía sus 2 entradas»; la orden sigue con **2** entradas |
| Mismo mensaje (mismo `eventoId`) publicado dos veces | ✅ la segunda se descarta por `mensaje_procesado` |
| **Prueba de fuego**: `ms-tickets` apagado, compra y pago completos | ✅ pago `200`, orden `PAGADA`; `tickets.orden-pagada` con **1 mensaje listo y 0 consumidores** |
| Reencender `ms-tickets` | ✅ la orden pasa a `EMITIDA` con sus 2 entradas, a los 11 s de lanzar el proceso (casi todo es el arranque de Spring); la cola queda en 0 |
| **Reintentos**: `ms-eventos` apagado y un `orden.pagada` nuevo | ✅ avisos a 1 s, 2 s y 4 s, y luego `EMISION FALLIDA` (ERROR) sin crear entradas ni dejar el mensaje colgado |
| No regresión | ✅ sala, pase, orden y pago (fases 7 a 9) funcionan igual; los 8 servicios en `UP`; sin `ERROR` inesperados en los logs |

## 4. Decisiones y problemas

**Decisión 1 — Los reintentos los hace el listener, no RabbitMQ.** Un `requeue` devuelve el mensaje al instante y lo martillaría contra un
`ms-eventos` que quizá está arrancando. El bucle espera `1000 << intento` ms (1 s, 2 s, 4 s) y es configurable (`teleticket.tickets.reintentos`,
`espera-inicial-ms`). «3 reintentos» se entiende como el intento inicial **más** 3 más, o sea 4 ejecuciones. Un `MensajeInvalidoException` no se reintenta.

**Decisión 2 — `mensaje_procesado` se anota *después* de publicar, no con las entradas.** Si se anotara junto con ellas y el servicio se cayera
antes de publicar `tickets.emitidos`, la reentrega vería el mensaje como «ya hecho» y la orden quedaría `PAGADA` para siempre. Anotándolo al
final, la reentrega encuentra las entradas hechas (no crea otras, lo impide `UNIQUE(orden_id, numero)`) y **completa el anuncio**.

**Decisión 3 — Costo de la decisión 2.** Un `orden.pagada` republicado con un `eventoId` *nuevo* vuelve a anunciar `tickets.emitidos` (no
duplica entradas). `ms-ordenes` lo tolera (`PAGADA → EMITIDA` es un `UPDATE` condicional). Si en la Fase 13 `ms-notificaciones` enviara un
correo por anuncio, deberá deduplicar por `ordenId`.

**Decisión 4 — Las condiciones se piden *antes* de abrir la transacción.** Es una llamada HTTP; no debe retener una conexión de base de datos
(mismo criterio que `OrdenService`). `X-Request-Id` viaja como parámetro del Feign con el `correlationId` del mensaje, porque no hay petición HTTP de entrada.

**Decisión 5 — Cada extremo declara la cola del otro.** `ms-ordenes` declara `tickets.orden-pagada` y `ms-tickets` declara `ordenes.tickets-emitidos`.
Con `template.mandatory=true`, sin eso un `orden.pagada` publicado antes de que `ms-tickets` arrancara por primera vez **fallaría**, y la prueba de fuego no
valdría. Los parámetros deben ser idénticos en los dos sitios.

**Decisión 6 — Clave de firma.** Va en `application.properties` (`${TICKETS_CLAVE_FIRMA:…}`) y el servicio **no arranca** si mide menos de 32 caracteres.
El valor por defecto es solo de desarrollo; en producción se pasa por variable de entorno.

**Decisión 7 — Se añadió `usuario_id` a `ticket`** (el plan no lo lista): la Fase 14 necesita «mis entradas» y `tickets.emitidos` lo publica.

**Problema — Las estadísticas de RabbitMQ van con retraso.** La API de gestión mostró `messages: 0` en la cola 2 s después del pago; con `rabbitmqctl list_queues`
(o 5 s después) aparece el 1 real. Para esta prueba usa `rabbitmqctl`.

## 5. Qué sigue sin funcionar

| No funciona | Llega en |
|---|---|
| Agotados los reintentos el mensaje **se descarta**: la orden queda `PAGADA` sin entradas y sin reembolso. Solo queda un `ERROR` en el log | Fase 11 (DLQ + saga) |
| Las 3 órdenes `PAGADA` de las pruebas de la Fase 9 no tienen entradas: se pagaron antes de que existiera `ms-tickets` y su mensaje no tenía cola | — (datos de prueba) |
| No hay `GET` para ver las entradas, ni nominación, ni descarga, ni validación en la puerta (`validacion` existe, vacía) | Fase 14 |
| `ms-tickets` no anula entradas de órdenes reembolsadas | Fase 11 |
| No se envía el correo con la entrada | Fase 13 |
| Hueco conocido (Fase 9): si el broker no confirma `orden.pagada`, no se emiten entradas | — (outbox, fuera del plan) |

## 6. Relación con el informe

| Requisito | Cómo se cumple |
|---|---|
| **CU-07** Emisión de e-tickets | ✅ automática y asíncrona al recibir `orden.pagada` |
| **PRB-05** Entradas falsas | ✅ `firma = HMAC-SHA256(codigo + eventoId, clave)`, verificable sin consultar la base |
| **PRB-06** Información contradictoria | ✅ las condiciones del evento se copian a cada entrada |
| **Sección 6** Mensajería | ✅ `orden.pagada` consumido con ack manual; `tickets.emitidos` publicado con *publisher confirms* |
| **Sección 7.1** — `ticket`, `validacion` | ✅ `UNIQUE(codigo)`, `UNIQUE(orden_id, numero)` |
| **Sección 8.5** Estados de la orden | ✅ `PAGADA → EMITIDA` |
| **DEC-01** | ✅ `DB_NAME=db_tickets` |

**Siguiente:** Fase 14 (orden acordado: 10 → 14), con la validación en la puerta usando `FirmaService.esValida`.
