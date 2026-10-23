# Fase 9 — `ms-pagos` y el pago de la orden

**Estado:** ✅ Completada · **Fecha:** 23/10/2026
**Cubre:** CU-06 · **Implementa:** idempotencia del cobro · **Resuelve:** doble cobro

## 1. En una frase

> `POST /api/ordenes/{id}/pagar` cobra la orden en `ms-pagos` y, **pase lo que pase —reintentos, timeouts, dos clics a la vez—,
> nunca se cobra dos veces**. Si la tarjeta se rechaza, el comprador **conserva su reserva** y puede probar con otra.

## 2. Qué se creó

| Archivo | Para qué |
|---|---|
| `ms-pagos/resources/schema.sql` | `pago` (`idempotency_key UNIQUE`, solo los 4 últimos dígitos de la tarjeta) y `reembolso` (`UNIQUE(pago_id)`) |
| `ms-pagos/service/PagoService` | El cobro idempotente en 3 pasos: reservar la clave → pasarela → anotar el resultado |
| `ms-pagos/repository/PagoRepository` | `iniciar` (`INSERT … ON CONFLICT DO NOTHING`), `reintentar` y `resolver` (`UPDATE` condicionales) |
| `ms-pagos/pasarela/PasarelaSimulada` | Termina en `0000` rechaza · `9999` aprueba tras 5 s · otra aprueba |
| `ms-pagos/controller/internal/PagoInternalController` | `POST /internal/pagos` (`201` cobro nuevo, `200` pago ya existente). Sin ruta en el gateway |
| `ms-ordenes/client/PagoClient` (+ `dto/`) | Feign hacia `ms-pagos`. `InventarioClient` gana `enPago` y `activar` |
| `ms-ordenes/service/OrdenService.pagar` · `OrdenController` | `POST /api/ordenes/{id}/pagar`. `OrdenRepository.cambiarEstado` (`UPDATE` condicional) |
| `ms-ordenes/exception/PagoRechazado` (402) · `PagoNoConfirmado` (503) | Los dos finales distintos de un pago que no se completa |
| `ms-ordenes/config/RabbitConfig` · `application.properties` | Declara `inventario.orden-pagada`; *publisher confirms*; `MS_PAGOS_URL` |
| `ms-inventario/service/ConfirmacionService` · `listener/OrdenPagadaListener` | Consume `orden.pagada` y deja la reserva `CONFIRMADA` |

## 3. Cómo lo compruebo

> **Antes:** `.\detener-todo.ps1`, compilar `ms-pagos`, `ms-ordenes` y `ms-inventario`, `.\arrancar-todo.ps1`. Necesitas una orden
> `RESERVADA` (flujo de la Fase 8). `$ORDEN` y `$USUARIO` son los de esa orden; el cuerpo va en archivos (regla 5):
> `{"numeroTarjeta":"4111111111110000","titular":"DANIEL FUENTES"}`.

```powershell
curl.exe -s -X POST "http://localhost:8090/api/ordenes/$ORDEN/pagar" -H "Content-Type: application/json" -H "X-Usuario-Id: $USUARIO" -d "@mala.json"
docker exec teleticket-postgres psql -U teleticket -d db_pagos -c "SELECT orden_id, monto, estado, intentos FROM pago;"
```

| Prueba | Resultado |
|---|---|
| Cuerpo inválido · orden de otra persona | ✅ `400 DATOS_INVALIDOS` con los campos · `404` |
| **Tarjeta `…0000`** | ✅ `402 PAGO_RECHAZADO`; orden **`RESERVADA`**, reserva **`ACTIVA`**, pago `RECHAZADO` |
| **Otra tarjeta, misma orden** | ✅ `200`, orden `PAGADA`. El pago pasa a `APROBADO` con `intentos = 2`: **la misma fila** |
| **El mismo comando otra vez (la clave de la fase)** | ✅ `200` en 13 ms, sin tocar la pasarela; `pago` sigue con **1 fila**, la misma referencia |
| `orden_historial` | ✅ `→ RESERVADA → PENDIENTE_PAGO → RESERVADA → PENDIENTE_PAGO → PAGADA`, cada paso con su motivo |
| **RabbitMQ** | ✅ `orden.pagada` publicado y consumido; reserva **`CONFIRMADA`**; la cola `inventario.orden-pagada` queda en 0 |
| **Tarjeta `…9999`** (tarda 5 s, Feign espera 3) | ✅ `503 PAGO_NO_CONFIRMADO` a los **3,09 s**. Estado: orden **`PENDIENTE_PAGO`**, reserva **`EN_PAGO`**, pago `PENDIENTE` |
| Reintento a los 0,5 s (la pasarela sigue cobrando) | ✅ `409 PAGO_EN_CURSO`: no se lanza un segundo cobro |
| Reintento tras esperar que la pasarela termine | ✅ `200`, orden `PAGADA`; el pago es el que la pasarela ya había aprobado; **1 fila** |
| **Dos pagos simultáneos** de la misma orden | ✅ ambos `200`; **1 fila** en `pago`; historial con **una** sola transición a `PAGADA`; **un** `orden.pagada` |
| No regresión | ✅ crear órdenes (Fase 8), sala y pase (Fase 7) y reservas (Fase 6) siguen igual; ningún `ERROR` inesperado en los logs |

## 4. Decisiones y problemas

**Decisión 1 — La clave se reserva *antes* de cobrar.** El plan dice «busca si existe; si no, cobra». Con eso, dos peticiones
simultáneas no encuentran nada y **las dos cobran** (la `UNIQUE` solo avisaría al guardar, con el dinero ya tomado). Por eso el
primer paso es un `INSERT … ON CONFLICT DO NOTHING` del pago en `PENDIENTE`: solo una petición lo logra y esa es la que va a la
pasarela. Cobrar queda **fuera** de toda transacción (un `9999` tarda 5 s).

**Decisión 2 — Un rechazo no gasta la clave.** Con `UNIQUE(idempotency_key)` y una fila `RECHAZADO` por orden, «otra tarjeta» habría
chocado con ella para siempre. Un rechazo se puede reintentar: la **misma fila** vuelve a `PENDIENTE` (`UPDATE … WHERE estado='RECHAZADO'`,
`intentos + 1`). Sigue habiendo **un pago por orden**. Un `APROBADO` es definitivo: repetir devuelve ese pago aunque la tarjeta sea otra.

**Decisión 3 — Si `ms-pagos` no responde, la orden NO vuelve a `RESERVADA`.** El cobro pudo haberse hecho. Devolverla a `RESERVADA`
dejaría «sin pagar» a alguien que ya pagó. Se queda en `PENDIENTE_PAGO` y el `503` dice que reintente con **la misma** tarjeta.
Por eso `pagar` acepta `RESERVADA` **y** `PENDIENTE_PAGO`: reintentar es seguro precisamente porque `ms-pagos` es idempotente.
Es lo que el plan pedía observar en la Prueba 5.

**Decisión 4 — Una orden ya `PAGADA` responde `200` con la orden**, sin llamar a nadie. Así el «mismo comando otra vez» es idempotente
también en `ms-ordenes`. La idempotencia de `ms-pagos` cubre el caso difícil: el reintento que llega mientras la orden no está `PAGADA`.

**Decisión 5 — Los cambios de estado son `UPDATE` condicionales** (`cambiarEstado(id, desde, hacia)`, igual que la reserva). Con dos
pagos simultáneos, solo quien consigue `PENDIENTE_PAGO → PAGADA` publica `orden.pagada`: no hay mensajes duplicados.

**Decisión 6 — `402` para el rechazo, `503` para «no sé».** Son finales distintos y el comprador debe actuar distinto: con `402` prueba
otra tarjeta; con `503` espera y reintenta la misma. Un rechazo viaja como pago `RECHAZADO` con `201/200`, no como error HTTP.

**Decisión 7 — Se publica después de guardar `PAGADA`**, para que quien reciba el mensaje (la Fase 10 consultará la orden) la encuentre
pagada. `ms-ordenes` declara también la cola `inventario.orden-pagada` (como `ms-eventos` con `evento.publicado`): con
`template.mandatory=true`, un mensaje sin cola enlazada falla, y no se podría cobrar si `ms-inventario` nunca arrancó.

**Decisión 8 — Solo se guardan los 4 últimos dígitos.** El número completo viaja de `ms-ordenes` a `ms-pagos` y no se escribe en
ningún log ni tabla.

**Problema — `CHAR(4)` y Hibernate.** `validate` espera `varchar` y `bpchar` no coincide; se usó `VARCHAR(4)`.

## 5. Qué sigue sin funcionar

| No funciona | Llega en |
|---|---|
| Hueco conocido: si el broker no confirma `orden.pagada` tras guardar `PAGADA`, el pago está hecho pero la reserva no se confirma ni se emiten entradas. Se deja un `ERROR` en el log. Lo cierra el patrón *outbox*, fuera del plan | — |
| Hueco conocido: si `ms-pagos` se cae entre reservar la clave y anotar el resultado, el pago queda `PENDIENTE` y los reintentos reciben `PAGO_EN_CURSO` | Fase 11 |
| No se emiten entradas al pagar (`orden.pagada` solo lo consume `ms-inventario`) | Fase 10 |
| No hay reembolso (`reembolso` existe, vacía) | Fase 11 |
| Una orden `PENDIENTE_PAGO` o `RESERVADA` sin pagar sigue reteniendo el stock: las reservas **no vencen** | Fase 12 (descartada) |
| `X-Usuario-Id` lo escribe cualquiera | Spring Security |

## 6. Relación con el informe

| Requisito | Cómo se cumple |
|---|---|
| **CU-06** Pagar la orden | ✅ `POST /api/ordenes/{id}/pagar`, con reintento ante rechazo |
| **Sección 5** Comunicación síncrona y errores claros | ✅ Feign a `ms-pagos` y `ms-inventario`; `402` / `503` / `409` con códigos estables |
| **Sección 6** Mensajería: `orden.pagada` | ✅ publicado con *publisher confirms*, consumido con ack manual y `mensaje_procesado` |
| **Sección 7.1** — `pago`, `reembolso` | ✅ `UNIQUE(idempotency_key)`, `UNIQUE(pago_id)` |
| **Sección 8.4** Estados de la reserva | ✅ `ACTIVA → EN_PAGO → CONFIRMADA`, y `EN_PAGO → ACTIVA` si se rechaza |
| **Sección 8.5** Estados de la orden | ✅ `RESERVADA → PENDIENTE_PAGO → PAGADA` y vuelta a `RESERVADA` |
| **DEC-01** | ✅ `DB_NAME=db_pagos` |

**Siguiente:** Fase 10 — `ms-tickets`: emisión asíncrona con firma HMAC.
