# Fase 8 — `ms-ordenes`: crear la orden con Feign

**Estado:** ✅ Completada · **Fecha:** 23/10/2026
**Cubre:** CU-05 (completo) · **Implementa:** RN-03, RN-05 · **Resuelve:** PRB-03 (segunda barrera)

## 1. En una frase

> Con un pase válido, **una sola petición** consume el pase en `ms-cola`, lee el precio en `ms-eventos`,
> reserva el stock en `ms-inventario` y deja la orden `RESERVADA` en `ms-ordenes`: cuatro servicios, tres
> bases de datos movidas. Y si algo falla a medio camino, **el comprador no pierde su turno**.

## 2. Qué se creó

| Archivo (`ms-ordenes/src/main/...`) | Para qué |
|---|---|
| `resources/schema.sql` | `orden` (`UNIQUE(pase_codigo)`, `UNIQUE(codigo)`, `CHECK total = precio × cantidad`) y `orden_historial` |
| `entity/Orden`, `OrdenHistorial`, `EstadoOrden` | Sin setters; el total se calcula al crear |
| `client/ColaClient`, `EventoClient`, `InventarioClient` | Las tres interfaces Feign. `client/dto/*` son copias de los contratos |
| `client/ClienteErrorDecoder` | Convierte los errores de otro servicio en excepciones propias |
| `config/FeignConfig` | Activa Feign y reenvía el `X-Request-Id` a cada llamada |
| `service/OrdenService` | La orquestación y su compensación |
| `exception/*` | `PaseNoValido`, `SinStock`, `ServicioNoDisponible` (503), `PaseRequerido` (400)… y el manejador |
| `controller/OrdenController` · `controller/internal/OrdenInternalController` | API pública e interna |
| `pom.xml` · `application.properties` | JPA, PostgreSQL, validación, AMQP, `mensajeria-comun`, **OpenFeign** · `DB_NAME=db_ordenes`, URLs `MS_*_URL`, **timeouts** |

**Cambios en `ms-cola`** (para poder compensar): `POST /internal/pases/{codigo}/devolver` (`USADO → VIGENTE` si es del
dueño y no venció; si no, `409 PASE_NO_DEVOLVIBLE`) y `consumir` acepta un `eventoId` **opcional** (sin él funciona como en la Fase 7).

| Método | Ruta | Acceso | Qué hace |
|---|---|---|---|
| `POST` | `/api/ordenes` | público (`X-Usuario-Id`, `X-Pase-Compra`) | `201` orden `RESERVADA`. `409` pase/stock/máximo/venta, `503` servicio caído |
| `GET` | `/api/ordenes/{id}` · `/api/ordenes/mias` | público | Mi orden (la ajena da `404`) · mis órdenes, la última primero |
| `GET` | `/internal/ordenes/{id}` | 🔒 `:8085` | Orden completa con historial (la usará `ms-reclamos`) |

**El `PaseCompraFilter` del gateway ya estaba activo** (es un `@Component` desde la Fase 2): sin cambios, y el Paso 1 da `400 PASE_REQUERIDO`.

## 3. Cómo lo compruebo

> **Antes:** `.\detener-todo.ps1`, compilar `ms-cola` y `ms-ordenes`, `.\arrancar-todo.ps1`. Necesitas **un pase**:
> publica un evento con venta en 2 min, mete compradores en la sala y espera el `HABILITADO` (Fase 7). Con `$env:COLA_CICLO_SEGUNDOS="3"`
> antes de arrancar la espera se acorta. Define `$USUARIO`, `$PASE`, `$EVENTO` y `$ZONA`.

```powershell
"{`"eventoId`":`"$EVENTO`",`"zonaId`":`"$ZONA`",`"cantidad`":2}" | Set-Content orden.json
curl.exe -s -X POST http://localhost:8090/api/ordenes -H "Content-Type: application/json" -H "X-Usuario-Id: $USUARIO" -H "X-Pase-Compra: $PASE" -d "@orden.json"
```
→ `201`, orden `RESERVADA`, `total` = precio × 2, `segundosParaPagar` ≈ 600. Los pasos del plan:

| Paso | Qué haces | Resultado |
|---|---|---|
| 1 | `POST /api/ordenes` sin `X-Pase-Compra` (cuerpo `{}` en un archivo) | `400` desde el gateway |
| 2 | Con `X-Pase-Compra: 99999999-9999-9999-9999-999999999999` | `409 PASE_NO_VALIDO` |
| 3-4 | Con un pase real; luego los 3 `SELECT` (`db_cola`, `db_inventario`, `db_ordenes`) | pase `USADO`, `disponible` bajó en 2, orden `RESERVADA` |
| 5 | Repetir el Paso 3 con el mismo pase | `409` |
| 6 | Pedir 10 con máximo 4 | `409 «Maximo 4 entradas por comprador»` |
| 7 | Apagar `ms-inventario` y comprar | `503` en menos de 1 s; al encenderlo, **el mismo pase** compra |
| 8 | `SELECT * FROM orden_historial;` | una fila por orden: `NULL → RESERVADA` |

**Resultado real** (script de punta a punta con más de 50 comprobaciones, más estos pasos a mano en PowerShell):

| Verificación | Resultado |
|---|---|
| Sin pase · pase inventado · sin formato UUID · sin `X-Usuario-Id` · cuerpo incompleto | ✅ `400` · `409` · `409` · `401` · `400`; **ningún intento gastó el pase** |
| Pase real | ✅ `201` `RESERVADA`, total `2 × 450 = 900.00`, 599 s para pagar |
| Efecto en 3 bases | ✅ pase `USADO`, stock `100 → 98`, orden y 1 fila de historial |
| Reusar el pase | ✅ `409`, stock sin cambio, una sola orden |
| RN-05: 10 con máximo 4 | ✅ `409 MAXIMO_EXCEDIDO` y el **pase sigue `VIGENTE`**; con ese pase, 4 sí compra |
| Pase de otra persona · de otro evento · zona inexistente · venta aún cerrada | ✅ `409` · `409` · `404` · `409 VENTA_CERRADA`; el pase no se gastó en ninguno |
| **Sin stock** (4 de un Palco con 3) | ✅ `409 SIN_STOCK`, el pase **volvió a `VIGENTE`**; con él se compraron 3. Con el Palco agotado, otro `409` y pase intacto |
| **20 peticiones simultáneas con el mismo pase** | ✅ 1 × `201`, 19 × `409`; una orden; stock bajó **una** vez; pase sigue `USADO` |
| `ms-inventario` apagado | ✅ `503` en **0,15 s**, pase `VIGENTE`; encendido, el mismo pase da `201` |
| **`ms-inventario` colgado** (acepta y no responde) | ✅ `503` en **3,69 s** (timeout de 3 s), pase `VIGENTE` |
| `ms-eventos` apagado (falla **antes** de consumir) · `ms-cola` apagado | ✅ `503` en 0,68 s / 0,51 s, pase `VIGENTE` |
| `X-Request-Id` | ✅ el que pone el gateway llegó a `ms-cola` y volvió igual (se ve en el log del decodificador) |
| `GET` de la orden propia / ajena / `mias` / sin cabecera | ✅ `200` · `404` · `200` · `401` |
| Interna por `:8085` / por el gateway | ✅ `200` con historial y pase / `404` |
| `devolver`: pase no usado · ajeno · del dueño · ya vencido | ✅ `409` · `409` · `200` · `409` |
| No regresión | ✅ 9/9 servicios, el stock de la Fase 6 y la sala de la Fase 7 siguen, `consumir` sin `eventoId` igual que antes, tests de `ms-cola` y `ms-ordenes` pasan |

## 4. Decisiones y problemas

**Decisión 1 — La «nota honesta» del plan: compensar con `devolver`, no reservar antes.** Reservar el stock *antes* de
consumir el pase dejaría a cualquiera apartar entradas **sin pase válido** (un ataque de retención de stock). Se eligió el
endpoint `devolver`. Pero no se confía solo en él: se reduce cuánto hay que compensar.

**Decisión 2 — Primero lo que solo lee.** Orden real: ① `ms-eventos` (lee) → ② consumir el pase → ③ reservar → ④ guardar. El plan
ponía el pase primero; aquí un error de negocio (zona inexistente, venta cerrada, **RN-05**) se detecta *antes* de gastar nada. Solo
se compensa lo que sí cambió algo (②) y falla después.

**Decisión 3 — El `eventoId` viaja en `consumir`.** Comprobar «¿este pase es de este evento?» *después* de consumirlo ya lo habría
gastado. Se verifica **dentro del mismo `UPDATE`**.

**Decisión 4 — `ordenId` derivado del pase.** Reintentar con el mismo pase da el mismo `ordenId`, y la reserva de `ms-inventario`
(`UNIQUE(orden_id)`) devuelve la misma sin descontar dos veces. Así, un reintento tras un timeout **no deja stock huérfano**.

**Decisión 5 — Reusar un pase es `409`, no «devuelve la misma orden».** Lo pide el plan (Paso 5) y evita un riesgo: si ya hay una
orden con ese pase, una compensación posterior **no debe** reabrirlo. La comprobación va primero. Si una respuesta se pierde
tras crearse la orden, el comprador la encuentra en `GET /api/ordenes/mias`.

**Decisión 6 — Sin transacción de base de datos alrededor de las llamadas HTTP.** Tres llamadas dentro de una transacción
agotarían el pool de conexiones en un pico. Solo la escritura local (orden + historial) va en una transacción corta
(`TransactionTemplate`).

**Decisión 7 — El tipo de excepción decide qué hacer.** El decodificador traduce: `404` → no encontrado; `409` → `PaseNoValido`,
`SinStock` o `ReglaNegocio` con el mismo código; el resto → `ServicioNoDisponible`. Un servicio que **responde «no»** no consumió
nada; uno que **no responde** puede haberlo hecho, y entonces se intenta devolver.

**Decisión 8 — Timeouts de 1 s (conexión) y 3 s (lectura), sin reintentos.** Feign no reintenta por defecto, que es lo correcto
con `POST`. Medido: servicio apagado 0,15 s; colgado 3,69 s.

**Decisión 9 — RN-05 se compara contra la orden actual.** Como cada persona tiene un solo pase por evento (un turno, un pase), no
puede haber órdenes anteriores del mismo evento, así que no hace falta sumarlas.

**Observación — AMQP sin uso todavía.** El plan lo incluye; `ms-ordenes` no publica ni consume nada hasta la Fase 9.

## 5. Qué sigue sin funcionar

| No funciona | Llega en |
|---|---|
| No se puede **pagar**: la orden se queda `RESERVADA` (y no existe `POST /pagar`) | Fase 9 |
| Las reservas **no vencen**: una orden sin pagar retiene el stock para siempre | Fase 12 (descartada) |
| Hueco conocido: si falla el guardado local y además `devolver`, o `ms-cola` cae justo tras consumir, el pase queda gastado. Se registra un `ERROR` en el log para revisarlo a mano | — |
| Hueco conocido: si tras un fallo el comprador reintenta con **otra cantidad**, la reserva anterior queda huérfana (`409 ORDEN_YA_TIENE_RESERVA`) | Fase 12 (descartada) |
| `X-Usuario-Id` lo escribe cualquiera | Spring Security |
| Los estados distintos de `RESERVADA` aún no tienen cómo alcanzarse | Fases 9–11 |

## 6. Relación con el informe

| Requisito | Cómo se cumple |
|---|---|
| **CU-05** Elegir zona y cantidad, reservar | ✅ `POST /api/ordenes` |
| **RN-03** Sin pase no se compra | ✅ Gateway (`400`) + `ms-ordenes` por Feign contra `ms-cola` (`409`) |
| **RN-05** Máximo por comprador | ✅ `409 MAXIMO_EXCEDIDO`, sin gastar el pase |
| **PRB-03** Saltarse la cola | Las **dos barreras** del diseño, más `eventoId` y dueño verificados en el mismo `UPDATE` |
| **Sección 5** Comunicación síncrona (Feign), timeouts, errores claros | ✅ 3 clientes, 1 s / 3 s, excepciones propias, `X-Request-Id` propagado |
| **Sección 9.4** Flujo de creación de la orden | ✅ con su compensación |
| **Sección 7.1** — `orden` (+ `orden_historial`) | ✅ con `UNIQUE(pase_codigo)` |
| **DEC-01** | ✅ `DB_NAME=db_ordenes` |

**Siguiente:** Fase 9 — `ms-pagos` y el pago de la orden (`POST /api/ordenes/{id}/pagar`).
