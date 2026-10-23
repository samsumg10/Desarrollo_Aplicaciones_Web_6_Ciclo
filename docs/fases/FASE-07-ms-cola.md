# Fase 7 — `ms-cola`: sala de espera, sorteo y pases

**Estado:** ✅ Completada · **Fecha:** 23/10/2026
**Cubre:** CU-03, CU-04 · **Implementa:** RN-02, RN-03, RN-04 · **Resuelve:** PRB-01, PRB-02, PRB-03

## 1. En una frase

> La sala de espera funciona de punta a punta: la gente entra, a la hora anunciada se hace **un único
> sorteo** que no se vuelve a tocar, cada 10 s pasa un grupo con su **pase de compra de un solo uso**,
> y un pase consumido (o vencido, o ajeno) no sirve de nuevo. Es lo opuesto a lo que pasó en 2017.

## 2. Qué se creó

Mismo patrón que las fases 3, 4 y 6, más dos tareas programadas. **Ningún Feign**: `ms-cola` no llama a nadie.

| Archivo (`ms-cola/src/main/...`) | Para qué |
|---|---|
| `resources/schema.sql` | `venta_evento`, `turno` (`UNIQUE(evento_id, usuario_id)` y `UNIQUE(evento_id, posicion)`), `pase_compra`, `mensaje_procesado` |
| `entity/*` | `VentaEvento`, `Turno`, `PaseCompra`, `MensajeProcesado` y los enums de estado. Sin setters sueltos |
| `repository/*` | Las consultas atómicas: `reclamarSorteo`, `sortear`, `tomarPosicionTardia`, `siguientesEnCola`, `consumir` |
| `service/EventoVentaService` | Consumidor de `evento.publicado`: guarda la copia local (idempotente) |
| `service/IngresoService` · `TurnoService` | Entrar a la sala · ver mi turno |
| `service/SorteoService` · `AdmisionService` | El sorteo único · cada ciclo habilita a un grupo y crea sus pases |
| `service/PaseService` | Consumir un pase |
| `scheduler/TareasProgramadas` | Dos `@Scheduled`: sorteo (cada 1 s mira si llegó la hora) y admisión (cada `ciclo-segundos`) |
| `listener/EventoPublicadoListener` · `config/*` | `ack` manual, como en ms-inventario · colas, parámetros (`ColaProperties`) |
| `controller/ColaController` · `controller/internal/PaseInternalController` | API pública e interna |
| `pom.xml` · `application.properties` | JPA, PostgreSQL, AMQP, `mensajeria-comun` · `DB_NAME=db_cola` (DEC-01) y parámetros de la sala |

| Método | Ruta | Acceso | Qué hace |
|---|---|---|---|
| `POST` | `/api/cola/eventos/{eventoId}/ingresar` | público (`X-Usuario-Id`) | `201` turno nuevo, `200` el mismo si ya lo tenías, `401` sin cabecera |
| `GET` | `/api/cola/turnos/{turnoId}` | público (solo su dueño) | Estado, posición, cuántos van delante, espera estimada y pase |
| `POST` | `/internal/pases/{codigo}/consumir` | 🔒 solo `:8083` | Cuerpo `{"usuarioId":"…"}`. `200` o `409 PASE_NO_VALIDO` |

**Parámetros** (`application.properties`; variables de entorno entre paréntesis): `teleticket.cola.capacidad-por-ciclo`
(`COLA_CAPACIDAD_POR_CICLO`, **20**), `ciclo-segundos` (`COLA_CICLO_SEGUNDOS`, **10**), `pase-minutos`
(`COLA_PASE_MINUTOS`, **10**). Estados del turno: `EN_SALA → EN_COLA → HABILITADO → EXPIRADO`.

## 3. Cómo lo compruebo

> **Antes:** `.\detener-todo.ps1`, compilar `ms-cola`, y en **la misma consola** donde arrancas:
> `$env:COLA_CAPACIDAD_POR_CICLO = "2" ; .\arrancar-todo.ps1` (las ventanas heredan la variable).

**Paso 1 — Evento con venta en 2 minutos** (crea, agrega zona y publica; JSON en archivos, regla 5):

```powershell
$venta = (Get-Date).ToUniversalTime().AddMinutes(2).ToString("yyyy-MM-ddTHH:mm:ssZ")
$fecha = (Get-Date).ToUniversalTime().AddDays(30).ToString("yyyy-MM-ddTHH:mm:ssZ")
"{`"nombre`":`"Concierto Fase 7`",`"fecha`":`"$fecha`",`"recinto`":`"Estadio Nacional`",`"inicioVenta`":`"$venta`",`"maxPorCliente`":4,`"condicionesIngreso`":`"Entrada digital. Presentar DNI fisico.`"}" | Set-Content evento.json
'{"nombre":"General","precio":120.00,"aforo":500}' | Set-Content general.json
$EVENTO = (curl.exe -s -X POST http://localhost:8090/api/eventos -H "Content-Type: application/json" -d "@evento.json" | ConvertFrom-Json).id
curl.exe -s -X POST "http://localhost:8090/api/eventos/$EVENTO/zonas" -H "Content-Type: application/json" -d "@general.json"
curl.exe -s -X PUT "http://localhost:8090/api/eventos/$EVENTO/publicar"
```

**Pasos 2 a 4 — 5 compradores entran; reingreso; sin sesión:** el bucle del plan
(`1..5 | ForEach-Object { … /ingresar … }`) → 5 turnos `EN_SALA` con `posicion: null`. Repetir al usuario 1 →
**`200` con el mismo `turnoId`**. Sin `X-Usuario-Id` → **`401`**.

**Paso 5 — El sorteo** (pasada la hora de venta):

```powershell
docker exec teleticket-postgres psql -U teleticket -d db_cola -c "SELECT right(usuario_id::text,1) AS usuario, posicion, estado FROM turno ORDER BY posicion;"
```
→ posiciones 1 a 5 **al azar**, todos `EN_COLA`. Repite el `SELECT` 20 s después: **mismas posiciones**.

**Pasos 6 a 9 — Admisión y pase:** `GET /api/cola/turnos/<turnoId>` con `-H "X-Usuario-Id: <usuarioId>"`. La posición 1
primero da `EN_COLA` (`turnosDelante: 0`, `esperaEstimadaSegundos: 10`) y en el ciclo siguiente `HABILITADO` con
`pase.codigo`. `rabbitmqctl list_queues name messages` muestra `notificaciones.todas` creciendo (los `turno.habilitado`).
Consumir con `"{`"usuarioId`":`"$usuarioId`"}" | Set-Content consumir.json` y
`curl.exe -s -X POST "http://localhost:8083/internal/pases/$codigo/consumir" -H "Content-Type: application/json" -d "@consumir.json"`:
1.ª vez **`200 USADO`**, 2.ª **`409`**, pase de otro usuario **`409`**.

**Resultado real** (con un script de pruebas de punta a punta y, aparte, los Pasos 1–9 a mano en PowerShell):

| Verificación | Resultado |
|---|---|
| Copia local por mensaje, sin llamar a `ms-eventos` | ✅ `venta_evento` creada al publicar |
| 5 ingresos · reingreso · sin cabecera · cabecera inválida · evento sin sala · turno ajeno | ✅ `201` · `200` mismo turno · `401` · `401` · `404` · `404` |
| Sorteo a la hora anunciada | ✅ a **0,57 s** de la hora; permutación de 1..5; `sorteo_en` registrado |
| Posiciones tras varios ciclos | ✅ idénticas: el sorteo no se repite |
| Admisión con capacidad 2 | ✅ grupos `[1,2] [3,4] [5]`, un ciclo cada **10,1 s** |
| `turno.habilitado` en RabbitMQ | ✅ un mensaje por turno habilitado |
| Pase: ajeno · 1.ª vez · 2.ª vez · inventado · sin `usuarioId` · por el gateway | ✅ `409` · `200` · `409` · `409` · `400` · `404` |
| **100 consumos simultáneos del mismo pase** | ✅ **1 × `200`, 99 × `409`** |
| Pase vencido (expiración adelantada en la base) | ✅ `409` al consumir; en el ciclo siguiente pase `VENCIDO` y turno `EXPIRADO` |
| Llegada tardía (RN-02) | ✅ posición 6 = máx.+1, nace `EN_COLA` |
| **100 llegadas tardías simultáneas** | ✅ posiciones 7..106, sin repetir ni saltos; `ultima_posicion` = máx. |
| **60 ingresos simultáneos del mismo usuario** | ✅ 1 × `201`, 59 × `200`, **1 fila** en la base |
| **Tormenta de 806 ingresos que cruza la hora del sorteo** | ✅ nadie `EN_SALA` después; posiciones 1..846 todas distintas; **14 rezagados** ubicados |
| El orden de llegada no coincide con el de posiciones | ✅ el sorteo es aleatorio |
| RabbitMQ caído | ✅ la admisión sigue (3 turnos `HABILITADO`, ingreso `201`); solo se pierde el aviso |
| No regresión | ✅ 9/9 servicios, `verificar-infra.ps1` correcto, stock de la Fase 6 se crea, `/api/usuarios/me` `200`, test de contexto de `ms-cola` pasa |

## 4. Decisiones y problemas

**Decisión 1 — El sorteo es un solo `UPDATE`.** `row_number() OVER (ORDER BY random())` numera a todos los
`EN_SALA` en una operación atómica: nunca queda la cola a medio ordenar. `random()` de PostgreSQL no es
criptográfico; para un sorteo de turnos basta, y todo queda auditable en la tabla.

**Decisión 2 — La unicidad del sorteo la da la base.** `UPDATE … SET REALIZADO WHERE estado_sorteo='PENDIENTE'`
lo gana un solo intento (el otro afecta 0 filas). Va en la **misma transacción** que el reparto: si algo falla,
el evento sigue `PENDIENTE` y se reintenta entero. Sirve igual con dos hilos o dos réplicas.

**Decisión 3 — Contador `ultima_posicion` en vez de `max(posicion)+1`.** Un `max()` leído y luego usado daría la
misma posición a dos llegadas simultáneas. El contador sube con un `UPDATE` que serializa la fila. Se probó con 100 a la vez.

**Decisión 4 — La carrera «ingreso vs. sorteo» se resuelve sin bloqueos en la ruta caliente.** Quien ingresa
mientras corre el sorteo puede quedar `EN_SALA`. En vez de bloquear a todos los que entran (la avalancha), una
pasada del sorteo recoge a esos rezagados y los ubica al final por orden de llegada. **Ocurrió de verdad** en la
prueba (14 personas) y salió bien. `UNIQUE(evento_id, posicion)` es la red de seguridad.

**Decisión 5 — El aviso `turno.habilitado` es *best effort*.** Se publica **después** de guardar y un fallo no
detiene la sala: `ms-cola` no puede depender del broker para dejar pasar gente. A diferencia de la Fase 5, aquí
se prefiere perder el correo antes que frenar la admisión; el comprador siempre ve su pase con `GET`. Al primer
fallo se abandonan los avisos del ciclo, para no bloquear la tarea reintentando uno por uno.

**Decisión 6 — Turno + pase en una transacción, y `FOR UPDATE SKIP LOCKED`.** Nunca hay un turno `HABILITADO`
sin pase, y dos réplicas nunca habilitan al mismo turno.

**Decisión 7 — `TransactionTemplate` en el ingreso.** La violación de `UNIQUE(evento_id, usuario_id)` hay que
atraparla *después* de que termine la transacción; con `@Transactional` la excepción sale demasiado tarde.

**Decisión 8 — Un mismo error para todo pase inválido,** y `404` para el turno de otro. Quien prueba códigos al
azar no puede distinguir «no existe» de «es de otro».

**Decisión 9 — Se agregó `EXPIRADO`** (no estaba en el plan): las reglas de la cola dicen que si el pase vence el
turno pasa al siguiente, y sin ese estado un pase vencido dejaría el turno `HABILITADO` para siempre.

**Decisión 10 — Dos hilos de tareas** (`spring.task.scheduling.pool.size=2`): con uno, una admisión lenta
retrasaría el sorteo a la hora exacta.

**Problema/observación — Ruido en el log.** Cada ingreso duplicado simultáneo imprime un `WARN` de Hibernate
(`duplicate key … uq_turno_usuario`). Es el choque esperado, ya resuelto; no es un fallo.

## 5. Qué sigue sin funcionar

| No funciona | Llega en |
|---|---|
| Nadie consume el pase todavía: el gateway ya exige la cabecera `X-Pase-Compra`, pero `ms-ordenes` aún no la valida contra `ms-cola` | Fase 8 |
| `notificaciones.todas` (declarada aquí) **no tiene consumidor** y crece sin límite; la Fase 13 está descartada | Fase 13 (descartada) |
| `X-Usuario-Id` lo escribe cualquiera: no hay login | Spring Security |
| La capacidad por ciclo se toma de la configuración al llegar `evento.publicado`; no se cambia por evento | — |
| El multi-réplica está razonado (`SKIP LOCKED`, `UPDATE` condicional) pero **no probado** | Fase 15 |
| El vencimiento de pases se marca cada ciclo (10 s); el consumo ya lo rechaza al instante por `expira_en > now()` | — |

## 6. Relación con el informe

| Requisito | Cómo se cumple |
|---|---|
| **CU-03** Entrar a la sala de espera | ✅ `POST /ingresar`, idempotente |
| **CU-04** Esperar turno y recibir el pase | ✅ `GET /turnos/{id}` y admisión por ciclos |
| **RN-02** Llegada tardía = `max+1` | ✅ contador atómico, 100 a la vez sin repetir |
| **RN-03** Sin pase no se compra | ✅ un pase, una compra: `UPDATE` condicional (el uso desde `ms-ordenes` llega en la Fase 8) |
| **RN-04** Sesión requerida | ✅ `401` sin `X-Usuario-Id`; el reingreso devuelve el mismo turno |
| **PRB-01** Colapso en picos | `ms-cola` absorbe la avalancha sin llamar a nadie y solo deja pasar `capacidad` cada 10 s |
| **PRB-02** Cola injusta | Sorteo único a la hora anunciada (a 0,57 s), posiciones guardadas y auditables |
| **PRB-03** Saltarse la cola | Pase UUID, ligado a persona y evento, que vence y se usa una vez |
| **Sección 7.1** del diseño — 3 tablas de `db_cola` | ✅ con las restricciones indicadas |
| **DEC-01** | ✅ `DB_NAME=db_cola` |

**Siguiente:** Fases 8+9 — `ms-ordenes` con Feign (consume el pase y reserva el stock) y `ms-pagos`.
