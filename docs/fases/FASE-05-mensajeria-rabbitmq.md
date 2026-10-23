# Fase 5 — Librería común y el primer mensaje de RabbitMQ

**Estado:** ✅ Completada (junto con la Fase 6, Bloque B) · **Fecha:** 23/10/2026
**Cubre:** CU-15 (publicar) · **Entregable N.º 5** del informe · **Resuelve:** PRB-02 (RN-01)

## 1. En una frase

> Al publicar un evento, `ms-eventos` deja una carta en RabbitMQ y esa carta llega a **dos buzones
> distintos**. Ya se puede **ver** un mensaje viajando entre servicios, que es la comunicación
> asíncrona de la que depende la mitad de la nota.

## 2. Qué se creó

**`libs/mensajeria-comun/`** — módulo Maven independiente (`mvnw install` lo deja en `~/.m2`):

| Archivo (`com/teleticket/mensajeria/`) | Para qué |
|---|---|
| `Mensajeria.java` | Nombres de exchanges, routing keys y colas **en un solo sitio** (las 14 colas del diseño) |
| `SobreMensaje<T>.java` | El sobre: `eventoId` (id **del mensaje**), `tipo`, `fecha`, `correlationId`, `datos` |
| `PublicadorEventos.java` | Interfaz con un único método `publicar(routingKey, mensaje)`. Cambiar a Kafka = otra implementación |
| `PublicadorRabbitMQ.java` | Implementación con *publisher confirms*, 3 intentos y detección de mensaje no enrutable |
| `MensajeriaAutoConfiguration.java` | Se activa sola en cualquier servicio que la tenga: exchanges, conversor JSON, publicador |
| `mensajes/*.java` | Los 11 DTO (`EventoPublicado`, `OrdenPagada`, …) + `ZonaPublicada` |
| `PublicacionFallidaException.java` | El broker no confirmó ni tras los reintentos |

**Cambios en `ms-eventos`** (todo *solo agrega*; los endpoints de la Fase 3 no cambian):

| Archivo | Qué |
|---|---|
| `pom.xml` | `spring-boot-starter-amqp` + `mensajeria-comun` |
| `application.properties` | Conexión a RabbitMQ (`RABBIT_HOST/PORT/USER/PASSWORD`), confirms, `mandatory`, timeout de 3 s |
| `config/RabbitConfig.java` | Declara `cola.evento-publicado` e `inventario.evento-publicado` y sus *bindings* |
| `service/EventoService.java` | `publicar(id, requestId)` arma y publica `evento.publicado` |
| `controller/EventoController.java` | Pasa el `X-Request-Id` como `correlationId` |
| `exception/ManejadorErrores.java` | `503 BROKER_NO_DISPONIBLE`; y `400 JSON_INVALIDO` (cierra el pendiente de la Fase 4) |

**Topología:** `teleticket.exchange` y `teleticket.dlx` (*topic*, durables). Dos *bindings* a la misma
routing key `evento.publicado`: un mensaje se copia en las dos colas (*publish/subscribe*).

## 3. Cómo lo compruebo

> **Antes:** `cd libs\mensajeria-comun ; .\mvnw.cmd -o clean install`, luego `.\detener-todo.ps1`,
> compilar `ms-eventos` y `.\arrancar-todo.ps1 ms-eventos,ms-api-gateway`.
> **Ojo:** el evento de la Fase 3 ya estaba `EN_VENTA` antes de existir el mensaje, así que
> `PUT /publicar` daría `409 YA_PUBLICADO`. Hay que **crear un evento nuevo**.

**Paso 1 — Las colas existen vacías** (apenas arranca `ms-eventos`, sin publicar nada):

```powershell
docker exec teleticket-rabbitmq rabbitmqctl list_queues name messages
```
→ `cola.evento-publicado 0` e `inventario.evento-publicado 0`. También en http://localhost:15672 → *Queues*.

**Paso 2 — Crea y publica un evento** (guarda los JSON en archivos, regla 5):

```powershell
'{"nombre":"Concierto Bloque B","fecha":"2026-12-20T20:00:00Z","recinto":"Estadio Nacional","inicioVenta":"2026-11-15T10:00:00Z","maxPorCliente":4,"condicionesIngreso":"Entrada digital. Presentar DNI fisico."}' | Set-Content evento.json
'{"nombre":"VIP","precio":450.00,"aforo":100}'     | Set-Content vip.json
'{"nombre":"General","precio":120.00,"aforo":500}' | Set-Content general.json
$EID = (curl.exe -s -X POST http://localhost:8090/api/eventos -H "Content-Type: application/json" -d "@evento.json" | ConvertFrom-Json).id
curl.exe -s -X POST "http://localhost:8090/api/eventos/$EID/zonas" -H "Content-Type: application/json" -d "@vip.json"
curl.exe -s -X POST "http://localhost:8090/api/eventos/$EID/zonas" -H "Content-Type: application/json" -d "@general.json"
curl.exe -s -X PUT "http://localhost:8090/api/eventos/$EID/publicar"
```
→ `200`, estado `EN_VENTA`.

**Paso 3 — Refresca la consola:** las **dos colas tienen 1 mensaje**. Un `PUT`, dos cartas, dos buzones.

**Paso 4 — Léelo:** *cola.evento-publicado* → *Get messages* con **Requeue: Yes** (para que siga ahí).
El JSON trae los 5 campos (`eventoId`, `tipo`, `fecha`, `correlationId`, `datos`) y en `datos` el
evento con sus zonas, aforos, hora de venta y reglas. El `correlationId` es el `X-Request-Id` del gateway.

**Paso 5 — RN-01:** repite el `PUT /api/eventos/$EID` cambiando `inicioVenta`. → **`409 EVENTO_CONGELADO`**.

**Paso 6 — Con RabbitMQ caído solo falla `/publicar`:** crea otro evento con una zona,
`docker stop teleticket-rabbitmq`, y publícalo → **`503 BROKER_NO_DISPONIBLE`** en ~1 s; el evento
**sigue en `BORRADOR`**, y `GET /api/eventos` responde `200`. Con `docker start teleticket-rabbitmq`
(espera a que la consola responda: `healthy` puede ser el estado anterior) el mismo `PUT` da `200`.

**Resultado real:**

| Verificación | Resultado |
|---|---|
| Colas y exchanges tras arrancar `ms-eventos`, sin publicar | ✅ 2 colas con 0 mensajes, 2 exchanges, 2 bindings |
| `PUT /publicar` | ✅ `200` · 1 mensaje en **cada** cola |
| Formato del mensaje | ✅ los 5 campos; fechas ISO-8601 UTC; `content_type: application/json`; `message_id` = `eventoId` |
| RN-01 tras publicar / publicar dos veces | ✅ `409 EVENTO_CONGELADO` / `409 YA_PUBLICADO` |
| Broker caído | ✅ `503` en 0,99 s, evento sigue `BORRADOR`, catálogo `200` |
| Tras `docker start` | ✅ `200`; el mensaje anterior **sobrevivió** al reinicio (colas durables, mensajes persistentes) |
| JSON roto en `POST /api/eventos` | ✅ `400 JSON_INVALIDO` (antes `500`) |
| No regresión | ✅ 9/9 servicios, infraestructura intacta, registro de usuarios `201` |

## 4. Decisiones y problemas

**Decisión 1 — El mensaje se publica como último paso, antes del commit.** Si RabbitMQ no confirma,
se lanza excepción, la transacción se deshace y el evento sigue en `BORRADOR`: nunca queda un evento
`EN_VENTA` del que nadie se enteró. El caso inverso (el mensaje sale y el commit falla justo después)
es posible pero muy raro; cerrarlo del todo exige el patrón *outbox*, fuera del alcance. Por eso se usa
`saveAndFlush`: la base valida el cambio **antes** de avisar a nadie.

**Decisión 2 — Se detecta el mensaje que nadie recibe.** Si el exchange existe pero ninguna cola
está enlazada a la routing key, RabbitMQ **confirma igual y descarta**. Con `publisher-returns` y
`mandatory`, el publicador lo ve y falla (sin reintentar: no es un fallo pasajero). Es la trampa más
silenciosa de RabbitMQ.

**Decisión 3 — Las colas las declara también `ms-eventos`.** Lo normal es que cada consumidor declare
la suya; se declaran aquí porque hasta las Fases 6 y 7 nadie las crea, y sin ellas no habría nada que
ver. Declarar dos veces la misma cola no da error **si los parámetros son idénticos**.

**Decisión 4 — Declarar al arrancar, no al primer envío.** `RabbitAdmin` declara al abrir la primera
conexión, y un servicio que solo publica no la abre hasta su primer mensaje. La librería lo fuerza al
arrancar (sin abortar si el broker está caído).

**Decisión 5 — Jackson 3.** El conversor es `JacksonJsonMessageConverter` (no el `Jackson2…` de los
tutoriales) y reutiliza el `JsonMapper` de Boot, para que las fechas salgan igual que en el REST.

**Observación — `eventoId` no es el evento.** Es el id **del mensaje** (así lo llama el informe). El
id del evento del catálogo va en `datos.id`. El `precio` sale como `450.0`: Jackson no conserva ceros.

## 5. Qué sigue sin funcionar

| No funciona | Llega en |
|---|---|
| Nadie consume `cola.evento-publicado`: los mensajes se acumulan | Fase 7 (`ms-cola`) |
| Los eventos publicados **antes** de esta fase (el de la Fase 3) nunca emitieron mensaje: no tendrán stock | — (crea uno nuevo) |
| Tabla `mensaje_procesado` | Fase 6 (`ms-inventario`) y Fase 7. Ningún servicio con base de datos consume todavía |
| DLQ y reintentos con espera creciente | Fase 10 (`ms-tickets`) |
| Los otros 10 DTO son contratos provisionales: se afinan al construir cada servicio que los usa | Fases 7–11 y 14 |

## 6. Relación con el informe

| Requisito | Cómo se cumple |
|---|---|
| **CU-15** Publicar reglas y horario | ✅ `PUT /publicar` valida zonas, congela y **avisa** con `evento.publicado` |
| **RN-01** Hora y reglas no cambian | ✅ `409 EVENTO_CONGELADO` (desde la Fase 3), ahora con el aviso al resto del sistema |
| **Sección 6.2** Topología | ✅ Exchange *topic*, formato del sobre, nombres `<servicio>.<routing-key>` |
| **Sección 6.6** Publisher confirms | ✅ `PublicadorRabbitMQ` |
| **Sección 11** Kafka a futuro | ✅ Todo pasa por `PublicadorEventos` |
| **Entregable N.º 5** — librería común | ✅ `libs/mensajeria-comun` |
