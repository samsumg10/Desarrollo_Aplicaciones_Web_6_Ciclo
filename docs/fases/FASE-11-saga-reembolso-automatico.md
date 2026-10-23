# Fase 11 — La saga: reembolso automático

**Estado:** ✅ Completada · **Fecha:** 23/10/2026
**Cubre:** OBJ 3 · **Implementa:** RN-08 · **Resuelve:** PRB-04 («la web cobra pero no emite», 2017)

## 1. En una frase

> Si la emisión de entradas falla tras sus reintentos, el sistema **reembolsa solo**: el mensaje cae en una cola de mensajes muertos
> y una cadena de 6 mensajes devuelve el dinero, repone el stock y anula las entradas. En la prueba, de `PAGADA` a `REEMBOLSADA`
> pasaron **8,5 s** sin que nadie interviniera.

## 2. Qué se creó

La cadena: `tickets.orden-pagada` → (4 intentos) → **DLQ** → `emision.fallida` → `reembolso.solicitado` → `pago.reembolsado` → `orden.reembolsada`.

| Dónde | Archivo | Para qué |
|---|---|---|
| `libs/mensajeria-comun` | `ColasCompartidas` · `Mensajeria` | `tickets.orden-pagada` **con DLX**, construida en un solo sitio; constante de la DLQ. `PagoReembolsado` gana `motivo` |
| `ms-tickets` | `RabbitConfig` | La cola con DLX, la DLQ `tickets.orden-pagada.dlq` y las colas de `emision.fallida` y `orden.reembolsada` |
| `ms-tickets` | `EmisionFallidaListener` | Lee la DLQ y publica `emision.fallida` |
| `ms-tickets` | `AnulacionService` · `OrdenReembolsadaListener` | `orden.reembolsada` → las `EMITIDO` de la orden pasan a `ANULADO` |
| `ms-tickets` | `EmisionService` · `application.properties` | **Interruptor** `teleticket.tickets.simular-fallo-emision` (`TICKETS_SIMULAR_FALLO`), en `false` por defecto |
| `ms-ordenes` | `ReembolsoService` · `EmisionFallidaListener` · `PagoReembolsadoListener` | `emision.fallida` → `reembolso.solicitado`; `pago.reembolsado` → orden `REEMBOLSADA` + `orden.reembolsada` |
| `ms-ordenes` | `EstadoOrden.puedePasarA` · `EmisionService` | La tabla de transiciones en un solo sitio; `REEMBOLSADA` y `EXPIRADA` son finales |
| `ms-pagos` | `ReembolsoService` · `ReembolsoSolicitadoListener` · `RabbitConfig` · `MensajeInvalidoException` | Primer consumidor del servicio: crea la fila de `reembolso`, pasa el pago a `REEMBOLSADO` y publica `pago.reembolsado` |
| `ms-pagos` | `PagoRepository` · `application.properties` | `registrarReembolso` (`ON CONFLICT (pago_id)`), `marcarReembolsado`; confirms, ack manual |
| `ms-inventario` | `DevolucionService` · `OrdenReembolsadaListener` · `RabbitConfig` | `orden.reembolsada` → reserva `LIBERADA` y `stock_zona.disponible` sube, **en una transacción** |
| `ms-inventario` | `ReservaRepository.liberar` · `StockZonaRepository.reponer` | Las dos consultas atómicas |

No hay endpoints nuevos ni tablas nuevas: todo es por mensajes (`reembolso` ya existía desde la Fase 9).

## 3. Cómo lo compruebo

> **Antes:** reinstalar la librería (`cd libs\mensajeria-comun ; .\mvnw.cmd -o clean install`), `.\detener-todo.ps1`, compilar los 4 servicios,
> **borrar la cola vieja** `docker exec teleticket-rabbitmq rabbitmqctl delete_queue tickets.orden-pagada` (ver Problema 1) y `.\arrancar-todo.ps1`.
> Para la demo, en la consola desde la que arrancas `ms-tickets`: `$env:TICKETS_SIMULAR_FALLO = "true"`.

```powershell
docker exec teleticket-postgres psql -U teleticket -d db_ordenes -c "SELECT estado_anterior, estado_nuevo, motivo, fecha FROM orden_historial WHERE orden_id='<id>' ORDER BY fecha, id;"
docker exec teleticket-postgres psql -U teleticket -d db_pagos -c "SELECT p.estado, p.monto, r.monto FROM pago p LEFT JOIN reembolso r ON r.pago_id=p.id;"
docker exec teleticket-postgres psql -U teleticket -d db_inventario -c "SELECT zona_id, disponible FROM stock_zona;"
docker exec teleticket-postgres psql -U teleticket -d db_tickets -c "SELECT numero, estado FROM ticket WHERE orden_id='<id>';"
```

| Prueba | Resultado |
|---|---|
| Topología al arrancar | ✅ `tickets.orden-pagada` con `x-dead-letter-exchange=teleticket.dlx`; la DLQ y las 5 colas nuevas, todas con consumidor |
| **Camino feliz** (interruptor apagado) | ✅ orden `EMITIDA`, 2 entradas `EMITIDO` |
| **Saga**: interruptor encendido, orden de 3 entradas (S/ 360) pagada | ✅ `PAGADA 12:51:05.6` → `REEMBOLSADA 12:51:14.1` = **8,5 s** (1 + 2 + 4 s de esperas y el resto de saltos) |
| `orden_historial` | ✅ `→ RESERVADA → PENDIENTE_PAGO → PAGADA → REEMBOLSADA`, la última con «No se pudieron emitir las entradas tras 4 intentos (S/ 360.00 devueltos)» |
| Dinero | ✅ pago `REEMBOLSADO`; 1 fila en `reembolso` por **360.00**, el monto cobrado |
| Stock | ✅ `48 → 45` al comprar, **`45 → 48`** al reembolsar; reserva `LIBERADA` |
| Entradas | ✅ las 3 quedaron guardadas y pasaron a `ANULADO` (el fallo simulado ocurre *después* de guardarlas) |
| Colas al terminar | ✅ todas en 0, incluida la DLQ |
| `reembolso.solicitado` repetido | ✅ sigue **1** fila en `reembolso`; el stock se queda en 48 |
| `tickets.emitidos` tardío de la orden `REEMBOLSADA` | ✅ no la resucita; sigue `REEMBOLSADA`, historial sin filas nuevas |
| `emision.fallida` de una orden ya `REEMBOLSADA` | ✅ ignorado con aviso |
| **Interruptor apagado** y nueva compra | ✅ orden `EMITIDA`; 9/9 servicios `UP`; `verificar-infra.ps1` correcto |

## 4. Decisiones y problemas

**Decisión 1 — La orden pasa a `REEMBOLSADA` solo cuando `ms-pagos` confirma.** Al recibir `emision.fallida` la orden sigue `PAGADA`
y solo se pide el reembolso. Así no existe un estado «reembolso pedido» que alguien deba resolver a mano, y el historial queda como pide el plan.

**Decisión 2 — Cada paso se puede repetir.** No se agregó ninguna tabla de mensajes procesados: `ms-pagos` usa `UNIQUE(pago_id)`, `ms-inventario`
solo repone si `liberar` cambió una fila, `ms-tickets` solo anula las `EMITIDO` y `ms-ordenes` usa `UPDATE` condicional. Además, un
`pago.reembolsado` repetido **vuelve a publicar** `orden.reembolsada`: si el servicio cayó entre cambiar la orden y anunciarlo, la
reentrega completa el anuncio (mismo criterio que la Decisión 2 de la Fase 10). Costo: un duplicado manda dos avisos, que los consumidores toleran.

**Decisión 3 — `REEMBOLSADA` se admite desde `PAGADA` y desde `EMITIDA`.** Si el dinero ya se devolvió, la orden debe reflejarlo aunque un
`tickets.emitidos` tardío la hubiera dejado `EMITIDA`; las entradas se anulan igual. `puedePasarA` documenta la tabla completa.

**Decisión 4 — El interruptor falla *después* de guardar las entradas** y antes de anunciarlas. Es una caída realista y permite ver la
anulación en la misma demo. Variable `TICKETS_SIMULAR_FALLO`, para no editar el `.properties`.

**Decisión 5 — La cola con argumentos se construye en la librería** (`ColasCompartidas`). `ms-tickets` y `ms-ordenes` la declaran; con dos
copias escritas a mano bastaba un argumento distinto para dar `PRECONDITION_FAILED`.

**Decisión 6 — `PagoReembolsado` ganó `motivo`.** Sin él, el historial de la orden no podía decir por qué se reembolsó. El cambio es compatible.

**Problema 1 — RabbitMQ no deja redeclarar una cola con otros argumentos.** `tickets.orden-pagada` existía sin DLX (Fase 10) y `ms-tickets`
no arrancaba. Se borró (estaba vacía) con `rabbitmqctl delete_queue`. En un entorno con mensajes pendientes hay que vaciarla antes.

**Problema 2 — El `motivo` dice «4 intentos»** porque «3 reintentos» es el intento inicial más 3 (Decisión 1 de la Fase 10).

## 5. Qué sigue sin funcionar

| No funciona | Llega en |
|---|---|
| Un `orden.pagada` republicado a mano para una orden ya reembolsada crearía entradas nuevas: `ms-tickets` no recuerda las órdenes reembolsadas | — (fuera del plan) |
| La pasarela es simulada y no tiene operación de devolución: el reembolso queda solo registrado | — |
| Pagos huérfanos (`PENDIENTE` si `ms-pagos` cae a mitad de un cobro): la Fase 11 **no** los resuelve | — (hueco de la Fase 9) |
| Sin la Fase 12 las reservas sin pagar no vencen y retienen el stock | Fase 12 (descartada) |
| No se avisa al comprador del reembolso | Fase 13 |
| No se probó apagar `ms-pagos` a mitad de la saga (por diseño, el mensaje espera en su cola, como en la Fase 10) | — |

## 6. Relación con el informe

| Requisito | Cómo se cumple |
|---|---|
| **RN-08** Pago aprobado sin entrada se reembolsa | ✅ DLQ + cadena de 6 mensajes, automática |
| **PRB-04** Cobro sin entrada (2017) | ✅ la misma falla termina en reembolso en ~8 s |
| **OBJ 3** Ninguna orden pagada sin entrada ni reembolso | ✅ demostrado con el interruptor; sin él nunca se activa |
| **Sección 6** Mensajería | ✅ DLX/DLQ, ack manual, *publisher confirms*, consumidores idempotentes |
| **Sección 8.5** Estados de la orden | ✅ `PAGADA → REEMBOLSADA`; finales sin salida |

**Siguiente:** Fase 14 (nominación, descarga y validación), luego 16 y 15.
