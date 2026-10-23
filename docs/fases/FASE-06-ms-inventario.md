# Fase 6 — `ms-inventario`: stock y reservas

**Estado:** ✅ Completada (junto con la Fase 5, Bloque B) · **Fecha:** 23/10/2026
**Cubre:** CU-05 (la parte de stock) · **Resuelve:** PRB-01 y PRB-04 en su raíz (nunca más entradas que el aforo) · **Ley 32415**

## 1. En una frase

> El primer **consumidor** de mensajes. Al publicarse un evento, `ms-inventario` crea solo el stock de
> cada zona, y ofrece las reservas con una consulta que **no puede sobrevender**: 150 compradores
> simultáneos por 100 entradas dieron exactamente 100 reservas y 50 rechazos.

## 2. Qué se creó

Mismo patrón que las Fases 3 y 4 (entidad → repositorio → servicio → controlador), más el listener.

| Archivo (`ms-inventario/src/main/...`) | Para qué |
|---|---|
| `resources/schema.sql` | `stock_zona` (`CHECK disponible >= 0` y `<= total`), `reserva` (`UNIQUE(orden_id)`), `mensaje_procesado` (`PK(evento_id)`) |
| `entity/StockZona`, `Reserva`, `EstadoReserva`, `MensajeProcesado` | **Sin setters**: el stock y el estado solo cambian con consultas atómicas |
| `repository/StockZonaRepository` | `descontar` (anti-sobreventa) y `crearSiNoExiste` |
| `repository/ReservaRepository`, `MensajeProcesadoRepository` | `cambiarEstado` condicional y `registrar` con `ON CONFLICT` |
| `service/StockService` | Lógica del consumidor: valida, registra el mensaje y crea el stock, **en una transacción** |
| `service/ReservaService` | Reservar, `en-pago`, `activar`, `disponible` |
| `listener/EventoPublicadoListener` | `@RabbitListener` con `ack` manual |
| `config/RabbitConfig` | Declara `inventario.evento-publicado` y su *binding* |
| `controller/internal/InventarioInternalController` | Los 4 endpoints internos |
| `exception/*`, `dto/*` | Mismo formato JSON de error que `ms-eventos` |
| `pom.xml` · `application.properties` | JPA, PostgreSQL, validación, AMQP, `mensajeria-comun` · `DB_NAME=db_inventario` (DEC-01), `ack` manual |

| Método | Ruta (🔒 solo puerto 8084) | Qué hace |
|---|---|---|
| `POST` | `/internal/reservas` | Reserva N entradas → `201` `ACTIVA` (vence en 10 min). `409 SIN_STOCK` si no alcanza |
| `PUT` | `/internal/reservas/{id}/en-pago` | `ACTIVA → EN_PAGO`. `409 RESERVA_VENCIDA` si ya venció |
| `PUT` | `/internal/reservas/{id}/activar` | `EN_PAGO → ACTIVA` (pago rechazado) |
| `GET` | `/internal/zonas/{zonaId}/disponible` | Un número: cuántas quedan |

## 3. Cómo lo compruebo

> **Antes:** `.\detener-todo.ps1`, compilar `ms-inventario`, `.\arrancar-todo.ps1 ms-inventario`.
> Si **ya hay mensajes** acumulados de la Fase 5, desaparecerán de la cola al arrancar.

**Paso 1 — El mensaje se consume solo.** Con `ms-inventario` apagado, publica un evento (Fase 5, Paso 2):
la cola `inventario.evento-publicado` tiene 1. Arranca el servicio y mira:

```powershell
docker exec teleticket-rabbitmq rabbitmqctl list_queues name messages consumers
```
→ `inventario.evento-publicado 0 1` (vacía, con 1 consumidor) y `cola.evento-publicado 1 0` (sigue esperando a `ms-cola`).

**Paso 2 — El stock se creó solo:**

```powershell
docker exec teleticket-postgres psql -U teleticket -d db_inventario -c "SELECT zona_id, total, disponible FROM stock_zona ORDER BY total;"
```
→ VIP 100/100 y General 500/500. Copia el `zona_id` del VIP: `$VIP = "<id>"`.

**Pasos 3 a 5 — Reservar, bajar el stock, no sobrevender:**

```powershell
"{`"ordenId`":`"aaaaaaaa-0000-0000-0000-000000000001`",`"zonaId`":`"$VIP`",`"cantidad`":2}" | Set-Content r1.json
curl.exe -s -X POST http://localhost:8084/internal/reservas -H "Content-Type: application/json" -d "@r1.json"   # 201 ACTIVA
curl.exe -s http://localhost:8084/internal/zonas/$VIP/disponible                                               # 98
"{`"ordenId`":`"aaaaaaaa-0000-0000-0000-000000000002`",`"zonaId`":`"$VIP`",`"cantidad`":200}" | Set-Content r2.json
curl.exe -s -X POST http://localhost:8084/internal/reservas -H "Content-Type: application/json" -d "@r2.json"   # 409 Sin stock suficiente
curl.exe -s http://localhost:8084/internal/zonas/$VIP/disponible                                               # sigue en 98, no -102
```

**Paso 6 — Idempotencia.** En la consola (*Exchanges → teleticket.exchange → Publish message*, routing
key `evento.publicado`) pega **el mismo JSON** que leíste en la Fase 5. → el stock **no** se duplica.
Repite el `SELECT` del Paso 2: sigue igual (y el VIP sigue en 98, no vuelve a 100).

**Resultado real:**

| Verificación | Resultado |
|---|---|
| Al arrancar, la cola se vacía y queda 1 consumidor | ✅ `0 / 1`; `cola.evento-publicado` conserva su mensaje |
| Stock creado por mensaje | ✅ VIP 100/100 y General 500/500 |
| Reservar 2 → disponible | ✅ `201 ACTIVA` → `98` |
| Reservar 200 con 98 disponibles | ✅ `409 SIN_STOCK` y sigue en `98` |
| **150 peticiones simultáneas × 1 entrada, stock 100** | ✅ **100 × `201`, 50 × `409`**, disponible `0`, 100 filas en `reserva` |
| **200 peticiones × 3 entradas, stock 500** | ✅ 166 × `201`, 34 × `409`, quedan **2** (498 vendidas, nunca negativo) |
| Mismo mensaje reenviado (mismo `eventoId`) | ✅ descartado; `mensaje_procesado` no crece |
| Mismo evento en un mensaje **nuevo** (otro `eventoId`) | ✅ «ya tenía stock, sin cambios»; el VIP **no** vuelve a 100 |
| Mensaje sin zonas | ✅ se rechaza con `ERROR` en el log y no vuelve a la cola (sin bucle) |
| PostgreSQL caído mientras llega un evento | ✅ el mensaje queda **sin confirmar** (`unacked=1`, no perdido); al volver la base se procesa (3 → 5 zonas). No llegó a ejercitarse el `nack`: la base volvió antes de que Hikari agotara su espera |
| Reintentar la misma orden (mismos datos / otra cantidad) | ✅ misma reserva sin descontar otra vez / `409 ORDEN_YA_TIENE_RESERVA` |
| `en-pago` y `activar`, repetidos | ✅ idempotentes; reserva inexistente `404`; vencida `409 RESERVA_VENCIDA` |
| JSON roto · UUID mal escrito · cantidad 0 | ✅ `400 JSON_INVALIDO` · `400 PARAMETRO_INVALIDO` · `400 DATOS_INVALIDOS` |
| Gateway: `/api/inventario/**` y `/internal/zonas/<id existente>/disponible` | ✅ `404` (con zona que sí existe: solo puede ser «sin ruta») |
| No regresión | ✅ Fases 3 y 4 responden, `9/9` servicios, `verificar-infra.ps1` correcto, tests de ambos módulos pasan |

## 4. Decisiones y problemas

**Decisión 1 — Nunca `save()` con el stock.** Con un id asignado a mano, `save()` hace un *merge* y
**sobrescribiría** el stock con el aforo: devolvería al inventario entradas ya vendidas. Por eso la
creación es `INSERT … ON CONFLICT DO NOTHING`. Se probó: reenviar el evento no resucita las 3 reservadas.

**Decisión 2 — Registrar el mensaje y guardar el trabajo, en la misma transacción.** Si la marca
`mensaje_procesado` se guardara aparte, un fallo a medias dejaría un mensaje «procesado» sin stock y
nadie lo volvería a crear. En una transacción, o se guarda todo o el mensaje se puede reintentar entero.
`registrar` es un solo `INSERT … ON CONFLICT`: «¿ya lo vi?» y «anótalo» no tienen hueco entre sí.

**Decisión 3 — `ack` manual y orden fijo:** guardar → `basicAck`. Si el servicio cae entre ambos,
RabbitMQ reenvía y `mensaje_procesado` lo descarta. Los errores se dividen en dos: **inválido** (sin
zonas, sin id) → `reject` sin reencolar, porque reintentar no lo arregla; **pasajero** (base caída) →
`nack` con reencolado.

**Decisión 4 — Todo cambio de estado es un `UPDATE … WHERE estado = X`.** Misma idea que el descuento:
no hay hueco entre «mirar el estado» y «cambiarlo» por el que se cuele el vencimiento (Fase 12).
`en-pago` y `activar` son idempotentes (si ya está en el destino, responde `200`) para que `ms-ordenes`
pueda repetir una llamada de Feign sin saber si llegó.

**Decisión 5 — `POST /reservas` idempotente por orden.** `UNIQUE(orden_id)` más una comprobación previa:
mismos datos devuelven la misma reserva; otra zona o cantidad es `409`. Dos peticiones simultáneas de
la misma orden las resuelve la restricción, y el stock descontado se devuelve por el *rollback*.

**Decisión 6 — Corregido el `500` por JSON roto de `ms-eventos`** (pendiente anotado en la Fase 4),
y `ms-inventario` nace con ese manejador y el de ruta inexistente.

**Observación — La base caída no provoca un fallo inmediato.** El listener se queda esperando una
conexión (Hikari espera 30 s por defecto antes de fallar) y el mensaje sigue **sin confirmar**, no
perdido. Qué pasa si la espera se agota (el `nack` con reencolado) está escrito pero **no se ejercitó**.

## 5. Qué sigue sin funcionar

| No funciona | Llega en |
|---|---|
| Las reservas **no vencen**: sin el mensaje con TTL (`inventario.reserva-espera`) una reserva sin pagar retiene el stock **para siempre**. Es la Fase 12, hoy descartada del orden acordado | Fase 12 (descartada) |
| Nadie consume `orden.pagada` (→ `CONFIRMADA`) ni `orden.reembolsada` (→ devuelve el stock) | Fase 9 · Fase 11 |
| Un fallo **persistente** (no inválido) reencolaría sin límite ni DLQ (no probado) | Fase 10 (patrón DLQ) |
| `GET /api/eventos` aún no muestra cuántas quedan. La Fase 3 lo anticipó para esta fase, pero el plan de la Fase 6 no lo incluye y `ms-inventario` no tiene API pública | Sin fase asignada en el plan |
| El evento de la Fase 3 (publicado antes del mensaje) **no tiene stock**: reservar en él da `404` | — (usa uno nuevo) |

## 6. Relación con el informe

| Requisito | Cómo se cumple |
|---|---|
| **CU-05** Reservar entradas (stock) | ✅ `POST /internal/reservas`, 10 min de vigencia |
| **Sección 7** Reservar sin sobreventa | ✅ `UPDATE … WHERE disponible >= :cantidad`, sin bloqueos manuales; probado con carga concurrente |
| **Sección 6.6** Idempotencia y confirmación manual | ✅ `mensaje_procesado` + `ack` tras guardar |
| **Ley 32415** — no emitir más que el aforo | ✅ `CHECK (disponible >= 0 AND disponible <= total)` como última defensa |
| **Sección 8.4** — API interna sin ruta en el gateway | ✅ los 4 endpoints, solo por el puerto 8084 |
| **DEC-01** | ✅ `DB_NAME=db_inventario` |

**Siguiente:** Fase 7 — `ms-cola`: sala de espera, sorteo y pases.
