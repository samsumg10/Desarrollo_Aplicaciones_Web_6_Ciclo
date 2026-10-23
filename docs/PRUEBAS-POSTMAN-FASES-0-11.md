# Pruebas en Postman — Teleticket (Fases 0 a 11)

Guía para la presentación: cada prueba dice **qué envías**, **qué debe responder**, **a qué fase y microservicio
pertenece** y **para qué sirve** (qué problema o regla del informe demuestra). Todo sale del código y de los
documentos de `docs/fases/`; los resultados "esperados" son los que ya se verificaron al cerrar cada fase.

---

## 0. Preparación (hazlo antes de exponer)

### 0.1 Levantar todo

```powershell
cd infra ; docker compose up -d ; cd ..      # Docker Desktop tarda ~2 min
.\arrancar-todo.ps1
.\verificar-servicios.ps1                    # debe decir 9/9 UP
.\verificar-infra.ps1
```

> Si compilaste algo: `.\detener-todo.ps1` primero y compila con `-o`. `libs\mensajeria-comun` se instala antes que los servicios.

### 0.2 Variables de la colección

Crea una colección **Teleticket** y define estas variables (pestaña *Variables* → *Current value*):

| Variable | Valor inicial | Se llena sola con… |
|---|---|---|
| `gw` | `http://localhost:8090` | — (gateway) |
| `u1` | `00000000-0000-0000-0000-000000000001` | — (comprador Ana) |
| `u2` | `00000000-0000-0000-0000-000000000002` | — (comprador Luis) |
| `u3` | `00000000-0000-0000-0000-000000000003` | — (comprador Rosa) |
| `eventoId`, `zonaGeneral`, `zonaPalco` | vacío | T-18, T-19, T-20 |
| `turno1`, `turno2`, `turno3` | vacío | T-30, T-31, T-32 |
| `pase1`, `pase2`, `pase3` | vacío | T-35, T-36, T-37 |
| `orden1`, `orden2` | vacío | T-46 (u1) y sección 7 (u2) |
| `ordenSaga`, `zonaSaga` | vacío | T-65 y el segundo evento de la sección 9 |

Puertos directos (para las pruebas "internas"): usuarios 8081 · eventos 8082 · cola 8083 · inventario 8084 · ordenes 8085 · pagos 8086 ·
tickets 8087 · notificaciones 8088. **El gateway es el 8090, no el 8080.**

### 0.3 Cómo guardar variables y validar (copia y pega en la pestaña *Tests*)

```javascript
// Validar el código HTTP
pm.test("Status 201", () => pm.response.to.have.status(201));
// Guardar un campo en una variable de la colección
pm.collectionVariables.set("eventoId", pm.response.json().id);
```

Para el evento (T-10), en la pestaña **Pre-request Script** (genera fechas válidas sin editar nada a mano):

```javascript
// La venta abre en 2 minutos: da tiempo al sorteo y a la sala de espera
pm.collectionVariables.set("inicioVenta", new Date(Date.now() + 2*60*1000).toISOString());
pm.collectionVariables.set("fechaEvento", new Date(Date.now() + 30*24*3600*1000).toISOString());
```

### 0.4 Reglas de oro

- `Content-Type: application/json` en todo `POST`/`PUT` con cuerpo.
- La **identidad** va en la cabecera `X-Usuario-Id` (todavía no hay login/JWT; está documentado como pendiente).
- Los pases de compra **vencen a los 10 minutos** (`COLA_PASE_MINUTOS`). Ejecuta las secciones 5 a 8 seguidas.
- Los servicios internos (`8083`, `8084`, `8085`, `8086`) se llaman por su **puerto directo**; por el gateway deben dar `404` (eso es un éxito).

---

## 1. Mapa de pruebas

| Sección | Fase(s) | Microservicio | Tema | Pruebas |
|---|---|---|---|---|
| 2 | 0, 1, 2 | gateway + todos | Salud, enrutamiento, seguridad por ausencia de ruta, rastreo | T-01 a T-08 |
| 3 | 4 | `ms-usuarios` | Registro y perfil | T-09 a T-17 |
| 4 | 3, 5, 6 | `ms-eventos`, `ms-inventario` | Catálogo, reglas congeladas, stock por mensaje | T-18 a T-29 |
| 5 | 7 | `ms-cola` | Sala de espera, sorteo único, pase de compra | T-30 a T-40 |
| 6 | 8 | `ms-ordenes` (+ cola, eventos, inventario) | Crear orden con el pase | T-41 a T-51 |
| 7 | 9 | `ms-ordenes` + `ms-pagos` | Pago, rechazo, idempotencia | T-52 a T-59 |
| 8 | 10 | `ms-tickets` + `ms-ordenes` | Emisión de entradas firmadas | T-60 a T-63 |
| 9 | 11 | `ms-tickets`, `ms-ordenes`, `ms-pagos`, `ms-inventario` | Saga de reembolso | T-64 a T-71 |
| 10 | — | — | Verificación con consola (lo que Postman no ve) | — |
| 11 | — | — | Guion sugerido de 10 minutos y qué NO está implementado | — |

---

## 2. Fases 0, 1 y 2 — Cimientos, infraestructura y API Gateway

> **Fase 0:** los 9 servicios arrancan. **Fase 1:** PostgreSQL (una instancia, 9 bases), RabbitMQ y MailHog en Docker.
> **Fase 2:** el gateway enruta y filtra.

| ID | Request | Esperado | Para qué sirve |
|---|---|---|---|
| **T-01** | `GET {{gw}}/api/eventos/ping` | `200` `{"estado":"ok","servicio":"ms-eventos","puerto":8082,…}` | **Enrutamiento.** Pides al 8090 y responde el 8082: el cliente solo conoce un puerto |
| **T-02** | `GET {{gw}}/api/usuarios/ping`, `/api/cola/ping`, `/api/ordenes/ping`, `/api/tickets/ping` | `200`, cada uno con su `servicio` y `puerto` | Las 5 rutas públicas existen (usuarios, eventos, cola, ordenes, tickets) |
| **T-03** | `GET {{gw}}/api/inventario/ping`, `/api/pagos/ping`, `/api/notificaciones/ping` | **`404`** los tres | **PRB-03.** Inventario, pagos y notificaciones **no tienen ruta**: nadie puede reservar ni cobrar desde fuera, así que no se puede saltar la cola. El `404` es el éxito |
| **T-04** | `GET http://localhost:8084/ping` (puerto directo) | `200` `ms-inventario` | Contraste con T-03: el servicio **sí está vivo**, solo que oculto detrás del gateway |
| **T-05** | `GET {{gw}}/api/eventos/ping` y mira la cabecera de la respuesta | Cabecera `X-Request-Id: <uuid>` distinto en cada envío | **Trazabilidad.** El gateway pone un id por petición que viaja por todos los servicios y mensajes |
| **T-06** | Igual que T-05 enviando la cabecera `X-Request-Id: mi-trazador-12345` | La respuesta devuelve `mi-trazador-12345` | El gateway respeta el id del cliente (útil para seguir una compra de punta a punta en los logs) |
| **T-07** | `POST {{gw}}/api/ordenes` cuerpo `{}` **sin** `X-Pase-Compra` | **`400`** `{"error":"PASE_REQUERIDO","mensaje":"Para comprar necesitas un pase de compra valido…"}` | **Primera barrera contra PRB-03:** el gateway rechaza una compra sin pase antes de que llegue al negocio |
| **T-08** | `POST {{gw}}/api/ordenes/abc/pagar` cuerpo `{}` **sin** pase | **No** es `PASE_REQUERIDO` (da otro error: 401/400/404 del servicio) | El filtro solo aplica a *crear* la orden; pagar una orden existente no necesita pase |

Extra de la Fase 1: abre `http://localhost:15672` (guest/guest) para mostrar RabbitMQ, y `http://localhost:8025` para MailHog (aún vacío: las notificaciones no se implementaron).

---

## 3. Fase 4 — `ms-usuarios` (CU-01)

Ruta pública `/api/usuarios`. Hay 6 usuarios sembrados (contraseña `Teleticket123`): `…0001` a `…0003` compradores, `…0004` operador, `…0005` controlador, `…0006` atención.

| ID | Request | Esperado | Para qué sirve |
|---|---|---|---|
| **T-09** | `POST {{gw}}/api/usuarios` · Pre-request: `pm.collectionVariables.set("dni", String(Math.floor(10000000+Math.random()*89999999))); pm.collectionVariables.set("mail","t"+Date.now()+"@correo.com");` · Body `{"dni":"{{dni}}","nombres":"Daniel Fuentes","email":"{{mail}}","password":"Secreta123"}` | `201`, trae `id, dni, nombres, email, rol, creadoEn`; `rol":"COMPRADOR"`; **sin contraseña** | **CU-01** Registrarse. Demuestra que la respuesta nunca devuelve la contraseña |
| **T-10a** | Repite T-09 (mismo DNI, mismo correo) | `409` `DNI_DUPLICADO` | Un DNI = una cuenta |
| **T-10b** | T-09 con el correo en mayúsculas y **otro** DNI | `409` `EMAIL_DUPLICADO` | El correo se normaliza a minúsculas; no se duplica por capitalización |
| **T-11** | Body `{"dni":"123","nombres":"","email":"x","password":"corta"}` | `400` `DATOS_INVALIDOS` con los 4 campos | Validaciones de entrada con mensaje por campo |
| **T-12** | Body `{roto` | `400` `JSON_INVALIDO` | El servicio no se cae con basura |
| **T-13** | Body de T-09 más `"rol":"OPERADOR"` | `201` pero `rol":"COMPRADOR"` | **Seguridad:** el cliente no puede elegir su rol (si pudiera, cualquiera publicaría eventos) |
| **T-14** | `GET {{gw}}/api/usuarios/me` con `X-Usuario-Id: {{u1}}` | `200` perfil de Ana | Identidad por cabecera |
| **T-15** | `GET {{gw}}/api/usuarios/me` **sin** cabecera | `401` `NO_AUTENTICADO` | **RN-04** Sin sesión no hay acceso |
| **T-16** | `GET {{gw}}/api/usuarios/me` con `X-Usuario-Id: abc` | `401` | Cabecera mal formada también se rechaza |
| **T-17** | `GET http://localhost:8081/internal/usuarios/{{u1}}/contacto` y luego la misma ruta por `{{gw}}` | `200` `{nombres,email}` por el 8081 · **`404`** por el gateway | API interna oculta (la usará notificaciones, Fase 13 no implementada) |

> T-09 a T-13: la contraseña se guarda como hash BCrypt (`$2a$10$…`, 60 caracteres). Se ve en consola (sección 10).

---

## 4. Fases 3, 5 y 6 — `ms-eventos` y `ms-inventario`

> **Fase 3:** catálogo, zonas y reglas congeladas. **Fase 5:** al publicar, `ms-eventos` publica `evento.publicado` en
> RabbitMQ (librería `mensajeria-comun`). **Fase 6:** `ms-inventario` consume ese mensaje y crea el stock por zona.
> Las tres se prueban juntas porque forman una sola cadena.

Cabecera sugerida en las llamadas de operador: `X-Usuario-Id: 00000000-0000-0000-0000-000000000004`.

| ID | Request | Esperado | Para qué sirve |
|---|---|---|---|
| **T-18** | `POST {{gw}}/api/eventos` · Pre-request de 0.3 · Body `{"nombre":"Concierto Demo 2026","fecha":"{{fechaEvento}}","recinto":"Estadio Nacional","inicioVenta":"{{inicioVenta}}","maxPorCliente":4,"condicionesIngreso":"Entrada digital. Presentar DNI fisico del asistente nominado."}` · Tests: guardar `eventoId` | `201`, `estado":"BORRADOR"`, cabecera `Location` | **CU-14** Configurar evento. Nace en BORRADOR |
| **T-19** | `POST {{gw}}/api/eventos/{{eventoId}}/zonas` Body `{"nombre":"General","precio":120.00,"aforo":500}` · guardar `zonaGeneral` (= `id`) | `201` | **CU-14** Zonas con precio y aforo |
| **T-20** | Igual con `{"nombre":"Palco","precio":450.00,"aforo":3}` · guardar `zonaPalco` | `201` | Zona pequeña para probar `SIN_STOCK` en T-46 |
| **T-21** | Repite T-19 | `409` `ZONA_DUPLICADA` | No hay dos zonas con el mismo nombre |
| **T-22** | `POST {{gw}}/api/eventos` con `"nombre":""` y `"maxPorCliente":99` | `400` `DATOS_INVALIDOS` con `campos` (nombre, maxPorCliente, condicionesIngreso si falta) | Validación por campo |
| **T-23** | `GET {{gw}}/api/eventos` | `200`, el evento **no** aparece (sigue en BORRADOR) | El público solo ve eventos EN_VENTA |
| **T-24** | `GET {{gw}}/api/eventos?incluirBorradores=true` | `200`, aparece el evento | Vista del operador |
| **T-25** | `PUT {{gw}}/api/eventos/{{eventoId}}` con otra `inicioVenta` | `200`, la hora cambió | Antes de publicar **sí** se puede modificar |
| **T-26** | `PUT {{gw}}/api/eventos/{{eventoId}}/publicar` (cabecera `X-Usuario-Id` del operador) | `200`, `estado":"EN_VENTA"`, `publicadoEn`, `aforoTotal":503`. Publícalo **justo después** de T-20, para que la venta abra ~2 min después | **CU-15** Publicar. Además dispara `evento.publicado` hacia `ms-cola` y `ms-inventario` (Fases 5/7) |
| **T-27** | Repite el mismo `PUT` de T-25 (cambio de hora) | **`409`** `EVENTO_CONGELADO` | **RN-01 / PRB-02: la prueba más importante.** El mismo cambio es válido antes y prohibido después. En 2017 reordenaron la cola desde una hora distinta a la anunciada; aquí es imposible |
| **T-28** | `POST {{gw}}/api/eventos/{{eventoId}}/zonas` (zona nueva) con el evento ya EN_VENTA | `409` | Tampoco se agregan zonas después de publicar |
| **T-29a** | `GET {{gw}}/api/eventos/{{eventoId}}` | `200`, evento con sus zonas (`id, nombre, precio, aforo`), reglas y condiciones | **CU-02** Consultar detalle |
| **T-29b** | `GET http://localhost:8082/internal/eventos/{{eventoId}}/zonas/{{zonaGeneral}}` | `200` `{precio, aforo, maxPorCliente, ventaAbierta}` | API interna que usará `ms-ordenes` (precio y límite). `ventaAbierta` pasa a `true` al llegar la hora |
| **T-29c** | Misma ruta por `{{gw}}/internal/eventos/…` | `404` | La interna no se alcanza desde fuera |
| **T-29d** | `GET http://localhost:8084/internal/zonas/{{zonaGeneral}}/disponible` | `200` con el número **`500`** (para `{{zonaPalco}}`: `3`) | **Fases 5+6:** el stock existe sin que nadie lo creara a mano → el mensaje de RabbitMQ funcionó |

> Si T-29d responde `404` o `0`, el mensaje `evento.publicado` no llegó: revisa RabbitMQ (sección 10) o que `ms-inventario` esté arriba.

Pruebas del propio inventario (Fase 6, puerto directo, solo para mostrar atomicidad; **usan una zona distinta a la de la compra** para no ensuciar el stock — crea una zona "Tribuna" aforo 100 antes de publicar si quieres usarlas):

| ID | Request | Esperado | Para qué sirve |
|---|---|---|---|
| **T-29e** | `POST http://localhost:8084/internal/reservas` `{"ordenId":"aaaaaaaa-0000-0000-0000-000000000001","zonaId":"<tribuna>","cantidad":2}` | `201` `estado":"ACTIVA"`; el `disponible` baja 2 | Reserva atómica de stock |
| **T-29f** | Mismo `POST` con `"cantidad":200` y otra `ordenId` | `409` `SIN_STOCK`; el disponible **no** queda negativo | **Nunca se sobrevende** (se probó con 150 peticiones simultáneas sobre 100 entradas: 100 × `201`, 50 × `409`) |

---

## 5. Fase 7 — `ms-cola`: sala de espera, sorteo único y pase (CU-03, CU-04)

> Empieza tras T-26. Con capacidad por defecto (20 por ciclo, ciclo de 10 s) los tres compradores pasan en el primer ciclo.
> Para ver el sorteo repartido por grupos, arranca `ms-cola` con `$env:COLA_CAPACIDAD_POR_CICLO = "2"`.

| ID | Request | Esperado | Para qué sirve |
|---|---|---|---|
| **T-30** | `POST {{gw}}/api/cola/eventos/{{eventoId}}/ingresar` con `X-Usuario-Id: {{u1}}` · guardar `turno1` = `turnoId` | `201`, `estado":"EN_SALA"`, `posicion: null` | **CU-03** Entrar a la sala **antes** de la hora de venta |
| **T-31** | Igual con `{{u2}}` → `turno2` | `201` | Más compradores en la sala |
| **T-32** | Igual con `{{u3}}` → `turno3` | `201` | Idem |
| **T-33** | Repite T-30 | **`200`** con el **mismo** `turnoId` | **Idempotente:** F5 o doble clic no crea un segundo turno (probado con 60 ingresos simultáneos del mismo usuario: 1 sola fila) |
| **T-34a** | `POST …/ingresar` **sin** `X-Usuario-Id` | `401` | **RN-04** Sin sesión no hay turno |
| **T-34b** | `POST {{gw}}/api/cola/eventos/00000000-0000-0000-0000-0000000000ff/ingresar` con `{{u1}}` | `404` | Evento sin sala de espera |
| **T-35** | Pasada la `inicioVenta` (≈ 2 min): `GET {{gw}}/api/cola/turnos/{{turno1}}` con `{{u1}}` | `estado":"EN_COLA"` con `posicion` (1 a 3), `turnosDelante`, `esperaEstimadaSegundos`; **segundos después** `estado":"HABILITADO"` con `pase.codigo`. Guarda `pase1 = pase.codigo` | **CU-04** Esperar y recibir el pase. **PRB-02:** las posiciones se asignan por **un único sorteo** a la hora anunciada (RN-01) |
| **T-36** | Igual con `turno2` y `u2` → `pase2` | Ídem | — |
| **T-37** | Igual con `turno3` y `u3` → `pase3` | Ídem | — |
| **T-38** | Repite T-35 unos segundos después | **La misma `posicion`** | **El sorteo no se repite ni se reordena** (PRB-02) |
| **T-39** | `GET {{gw}}/api/cola/turnos/{{turno1}}` con `X-Usuario-Id: {{u2}}` | `404` | Un turno solo lo ve su dueño (ni siquiera se revela que existe) |
| **T-40** | Pruebas del **pase** (usa `pase3`/`u3`, que no usaremos para comprar): ⒜ `POST http://localhost:8083/internal/pases/{{pase3}}/consumir` body `{"usuarioId":"{{u1}}"}` → `409 PASE_NO_VALIDO` (pase ajeno). ⒝ con `{"usuarioId":"{{u3}}"}` → `200 USADO`. ⒞ repetir ⒝ → `409`. ⒟ por `{{gw}}/internal/pases/…` → `404` | Ver cada paso | **PRB-03 / RN-03:** el pase es un UUID ligado a una persona y un evento, de **un solo uso** (probado con 100 consumos simultáneos del mismo pase: 1 × `200`, 99 × `409`) |

> **Nota de uso:** después de ⒝ el pase 3 quedó gastado. Es a propósito: u3 solo sirve para la demo del pase.
> Si luego quieres comprar con u3, repite el flujo con otro evento.

---

## 6. Fase 8 — `ms-ordenes` + Feign (CU-05)

`ms-ordenes` llama por Feign a `ms-cola` (consumir pase), `ms-eventos` (precio y límite) y `ms-inventario` (reservar). Usa **u1** y `pase1`.
Todas las requests de crear orden llevan `X-Usuario-Id: {{u1}}` y `X-Pase-Compra: {{pase1}}`; body
`{"eventoId":"{{eventoId}}","zonaId":"{{zonaGeneral}}","cantidad":2}`.

| ID | Request | Esperado | Para qué sirve |
|---|---|---|---|
| **T-41** | `POST {{gw}}/api/ordenes` **sin** `X-Pase-Compra` | `400` `PASE_REQUERIDO` (lo da el gateway) | Barrera 1 de PRB-03 |
| **T-42** | Con `X-Pase-Compra: 99999999-9999-9999-9999-999999999999` | `409` `PASE_NO_VALIDO` | **Barrera 2 (la real):** `ms-ordenes` valida contra `ms-cola`. Inventar un pase no sirve |
| **T-43** | Con pase real pero **sin** `X-Usuario-Id` | `401` | Sin sesión |
| **T-44** | `cantidad":10` (el máximo del evento es 4) | `409` `MAXIMO_EXCEDIDO` | **RN-05** Máximo por comprador. **El pase NO se gasta** |
| **T-45** | `zonaId` = `{{zonaPalco}}`, `cantidad":4` (el Palco tiene 3) | `409` `SIN_STOCK` | No se vende lo que no hay. **El pase vuelve a `VIGENTE`** (compensación) |
| **T-46** | Con `{{zonaGeneral}}` y `cantidad":2` · Tests: guardar `orden1 = id` | **`201`** orden `RESERVADA`, `total":240.00` (2 × 120), `segundosParaPagar` ≈ 600 | **CU-05** Crear la orden: consume el pase + reserva el stock + calcula el total. T-44 y T-45 prueban que los errores anteriores no habían quemado el pase |
| **T-47** | Repite T-46 con el mismo pase | `409` `PASE_NO_VALIDO` | Un pase, una compra |
| **T-48** | `GET http://localhost:8084/internal/zonas/{{zonaGeneral}}/disponible` | `498` (500 − 2) | El stock bajó de verdad |
| **T-49** | `GET {{gw}}/api/ordenes/{{orden1}}` con `{{u1}}` | `200` | Ver mi orden |
| **T-50** | Igual con `X-Usuario-Id: {{u2}}` | `404` | Una orden ajena responde igual que una inexistente |
| **T-51** | `GET {{gw}}/api/ordenes/mias` con `{{u1}}` · y `GET http://localhost:8085/internal/ordenes/{{orden1}}` | Lista con la orden · el interno trae `historial` (`null → RESERVADA`) y `paseCodigo`/`reservaId` | Historial de estados (auditoría); la interna no está en el gateway |

**Resiliencia (opcional, muy vistoso):** con un pase vigente de otro usuario/evento, cierra la ventana de `ms-inventario` y repite una compra →
**`503` `SERVICIO_NO_DISPONIBLE` en menos de 1 s** (timeouts de 1 s / 3 s) y el pase vuelve a `VIGENTE`; al reiniciar el servicio, **el mismo pase** compra.
No lo hagas con `pase1` si aún no has pagado.

---

## 7. Fase 9 — `ms-pagos` (CU-06)

`ms-pagos` no tiene API pública: se paga por `POST /api/ordenes/{id}/pagar`. Usaremos **u2** con una orden propia: repite la sección 6
(T-46) con `X-Usuario-Id: {{u2}}`, `X-Pase-Compra: {{pase2}}`, `cantidad":2`, y guarda `orden2`. Las tarjetas de prueba:

| Tarjeta | Comportamiento |
|---|---|
| termina en **`0000`** | Rechazada |
| termina en **`9999`** | Aprobada, pero tarda 5 s |
| cualquier otra (ej. `4111111111111111`) | Aprobada |

Todas las requests: `POST {{gw}}/api/ordenes/{{orden2}}/pagar`, `X-Usuario-Id: {{u2}}`, `Content-Type: application/json`.

| ID | Body | Esperado | Para qué sirve |
|---|---|---|---|
| **T-52** | `{"numeroTarjeta":"4111111111110000","titular":"LUIS TEST"}` | **`402`** `PAGO_RECHAZADO`; la orden sigue `RESERVADA` | Rechazo claro; el comprador **conserva su reserva** y puede intentar con otra tarjeta |
| **T-53** | `{"numeroTarjeta":"4111111111119999","titular":"LUIS TEST"}` | **`503`** `PAGO_NO_CONFIRMADO` a los ~3 s; la orden queda `PENDIENTE_PAGO` | **Tolerancia a fallos:** la pasarela tarda más que el timeout de Feign. **No se sabe si cobró**, así que no se marca ni pagada ni fallida |
| **T-54** | Repite T-53 de inmediato (antes de 5 s) | `409` `PAGO_EN_CURSO` | **No se lanza un segundo cobro** mientras el primero sigue en curso |
| **T-55** | Espera ~6 s y envía `{"numeroTarjeta":"4111111111119999","titular":"LUIS TEST"}` (la misma) | `200`, `estado":"PAGADA"` (según FASE-09: el `503` pide reintentar con la misma tarjeta) | Reintentar con la misma tarjeta resuelve el pago pendiente. **Esta es la única prueba de la guía cuyo resultado final no quedó registrado en los documentos: ejecútala una vez antes de exponer** |
| **T-56** | Repite T-55 | `200` `PAGADA`, **sin cobrar otra vez** | **Idempotencia:** un pago aprobado es definitivo (la clave es la orden) |
| **T-57** | `GET {{gw}}/api/ordenes/{{orden2}}` | `estado` en `PAGADA` y, en pocos segundos, `EMITIDA` (ver sección 8) | Ciclo completo de la orden |
| **T-58** | `GET http://localhost:8085/internal/ordenes/{{orden2}}` | `historial`: `null → RESERVADA → PENDIENTE_PAGO → RESERVADA → PENDIENTE_PAGO → PAGADA (→ EMITIDA)` con motivo en cada paso | **Auditoría completa**, incluidos el rechazo y el reintento |
| **T-59** | `POST http://localhost:8086/internal/pagos` `{"ordenId":"…","monto":240.00,"numeroTarjeta":"4111111111111111","titular":"X"}` y la misma ruta por `{{gw}}/internal/pagos` | `201`/`200` por el 8086 · **`404`** por el gateway | Nadie puede pedir un cobro por su cuenta |

Si quieres ver el camino feliz más corto, paga una orden con `4111111111111111` y obtienes `200 PAGADA` directo.

---

## 8. Fase 10 — `ms-tickets`: emisión de entradas (CU-07)

Tras pagar (T-55), `ms-ordenes` publica `orden.pagada`; `ms-tickets` crea las entradas **con firma HMAC-SHA256** y las
condiciones de ingreso del evento, y publica `tickets.emitidos`; `ms-ordenes` pasa la orden a `EMITIDA`.
**No hay endpoints públicos nuevos** (llegan en la Fase 14, no implementada): la evidencia se ve en la orden y en la base.

| ID | Request | Esperado | Para qué sirve |
|---|---|---|---|
| **T-60** | `GET {{gw}}/api/ordenes/{{orden2}}` con `{{u2}}` — espera 3 s tras el pago | `estado":"EMITIDA"` | Emisión **asíncrona**: el pago no espera a la emisión, se coordinan por RabbitMQ |
| **T-61** | `GET http://localhost:8085/internal/ordenes/{{orden2}}` | Último cambio: `PAGADA → EMITIDA`, motivo «2 entradas emitidas con firma digital» | Trazabilidad del hecho |
| **T-62** | `GET {{gw}}/api/tickets/ping` | `200` | `ms-tickets` está enrutado |
| **T-63** | RabbitMQ API (ver sección 10) o `docker exec … SELECT numero, codigo, firma, estado FROM ticket` | 2 entradas `EMITIDO`, firma de 64 caracteres | **PRB-05** (entrada falsificable): la firma se recalcula con la clave secreta; se verificó con otro programa (.NET) y coincide byte a byte. **PRB-06:** cada entrada lleva las condiciones de ingreso tal como se publicaron |

**Prueba de fuego (mensajería duradera), opcional:** con `ms-tickets` **apagado**, compra y paga: el pago responde `200`, la orden queda `PAGADA` y
`tickets.orden-pagada` conserva 1 mensaje. Al encender `ms-tickets`, la orden pasa sola a `EMITIDA`. Demuestra que **un servicio caído no pierde ventas**.

---

## 9. Fase 11 — Saga de reembolso automático (RN-08, PRB-04)

Si la emisión falla tras sus reintentos (1, 2 y 4 s), el mensaje cae en una **DLQ** y arranca una cadena de 6 mensajes:
`emision.fallida → reembolso.solicitado → pago.reembolsado → orden.reembolsada`. Resultado: la orden pasa a `REEMBOLSADA`, el stock se repone y las
entradas se anulan. En la prueba, de `PAGADA` a `REEMBOLSADA` pasaron **8,5 s** sin intervención humana.

**Montaje (una vez):** detén `ms-tickets`, y en **la consola desde la que lo arrancas**:

```powershell
$env:TICKETS_SIMULAR_FALLO = "true"
```

Arranca `ms-tickets` (el interruptor hace que falle **después** de guardar las entradas, para ver también su anulación). Necesitas un pase vigente en un evento
nuevo: crea un **segundo evento** repitiendo T-18 a T-26 (zona General aforo 50, `maxPorCliente` 4), mete a u1 en la sala (T-30) y espera el pase (T-35).

| ID | Request | Esperado | Para qué sirve |
|---|---|---|---|
| **T-64** | `GET http://localhost:8084/internal/zonas/{{zonaSaga}}/disponible` | `50` | Stock inicial |
| **T-65** | Crear orden de 3 entradas con el pase de u1 (como T-46, `cantidad":3`) → `ordenSaga` | `201` `RESERVADA`, total 360.00 (si el precio es 120) | — |
| **T-66** | `GET http://localhost:8084/internal/zonas/{{zonaSaga}}/disponible` | `47` | El stock bajó 3 |
| **T-67** | `POST {{gw}}/api/ordenes/{{ordenSaga}}/pagar` `{"numeroTarjeta":"4111111111111111","titular":"DANIEL FUENTES"}` | `200` `PAGADA` | **Se cobró** |
| **T-68** | Inmediatamente: `GET {{gw}}/api/ordenes/{{ordenSaga}}` | `estado":"PAGADA"` (momento crítico: pagó y **no** hay entrada) | Aquí está el problema de **PRB-04** (2017): *cobran y no emiten* |
| **T-69** | Espera ~9 s y repite T-68 | **`estado":"REEMBOLSADA"`** | **La saga resolvió sola el problema:** el dinero volvió sin que nadie interviniera |
| **T-70** | `GET http://localhost:8085/internal/ordenes/{{ordenSaga}}` | `historial`: `RESERVADA → PENDIENTE_PAGO → PAGADA → REEMBOLSADA`, último motivo «No se pudieron emitir las entradas tras 4 intentos (S/ 360.00 devueltos)» | Auditoría de la saga |
| **T-71** | `GET http://localhost:8084/internal/zonas/{{zonaSaga}}/disponible` | **`50`** otra vez | **Stock repuesto** (compensación) |

Qué mostrar como evidencia complementaria (consola, sección 10): pago `REEMBOLSADO` con 1 fila en `reembolso` por el monto cobrado; las 3 entradas en `ANULADO`; todas las colas en 0, incluida la DLQ.

**Al terminar:** vuelve a arrancar `ms-tickets` sin la variable (o ciérrala) para que el camino feliz siga funcionando.

---

## 10. Lo que Postman NO ve y cómo mostrarlo

### 10.1 RabbitMQ desde Postman (API de gestión)

`GET http://localhost:15672/api/queues` con *Authorization → Basic Auth* `guest` / `guest`. Para una cola concreta:
`GET http://localhost:15672/api/queues/%2F/tickets.orden-pagada`. Campos útiles: `messages`, `consumers`.
> Las estadísticas de la API tienen **retraso** de unos segundos. Para datos exactos usa la consola:

```powershell
docker exec teleticket-rabbitmq rabbitmqctl list_queues name messages consumers
```

### 10.2 Bases de datos (una instancia, 9 bases — DEC-01)

```powershell
docker exec teleticket-postgres psql -U teleticket -d db_usuarios   -c "SELECT email, password_hash FROM usuario;"
docker exec teleticket-postgres psql -U teleticket -d db_inventario -c "SELECT zona_id, total, disponible FROM stock_zona;"
docker exec teleticket-postgres psql -U teleticket -d db_cola       -c "SELECT right(usuario_id::text,1) u, posicion, estado FROM turno ORDER BY posicion;"
docker exec teleticket-postgres psql -U teleticket -d db_pagos      -c "SELECT p.estado, p.monto, r.monto FROM pago p LEFT JOIN reembolso r ON r.pago_id=p.id;"
docker exec teleticket-postgres psql -U teleticket -d db_tickets    -c "SELECT numero, codigo, substring(firma,1,16) firma, estado FROM ticket;"
```

(Conexión: PostgreSQL de Docker en el puerto **5433**, no el 5432 de Windows.)

---

## 11. Guion sugerido para 10 minutos y límites honestos

### 11.1 Orden recomendado (cada bloque tiene su "frase para decir")

| Min | Pruebas | Qué dices |
|---|---|---|
| 1 | T-01, T-03, T-07 | "Un solo punto de entrada. Inventario y pagos **no existen** hacia afuera. Sin pase no se compra" |
| 2 | T-09, T-13, T-15 | "Registro sin elegir rol; sin sesión, 401" |
| 3 | T-18 a T-27 | "Mismo cambio de hora: antes funciona, después **409**. Imposible repetir lo de 2017" |
| 4 | T-29d | "El stock apareció solo por mensajería" |
| 5–6 | T-30 a T-38 | "Sala de espera, sorteo único, pase de un solo uso" |
| 7 | T-41 a T-47 | "Pase inventado no sirve; los errores no gastan el pase; cae `201` solo cuando todo es válido" |
| 8 | T-52 a T-56 | "Tarjeta rechazada, pasarela lenta, idempotencia: no se cobra dos veces" |
| 9 | T-60 a T-63 | "Entradas firmadas y emitidas de forma asíncrona" |
| 10 | T-64 a T-71 | "**La saga**: cobró y no emitió → reembolso solo en 8 s" |

### 11.2 Qué NO está implementado (dilo antes de que pregunten)

| Tema | Estado |
|---|---|
| Login real / JWT (Spring Security) | **Pendiente.** La identidad es la cabecera `X-Usuario-Id`, documentado desde el diseño |
| **Fase 12** vencimiento de reservas | Descartada: una reserva sin pagar **no vence** y retiene el stock |
| **Fase 13** notificaciones por correo | Descartada: `ms-notificaciones` solo responde `ping`; MailHog queda vacío |
| **Fase 14** nominar, descargar y validar entradas | **Es la siguiente fase** (no hay endpoints `/api/tickets/...` más que `ping`) |
| Fases 15–17 (Docker de servicios, pruebas de carga, `ms-reclamos`) | No hechas. Las pruebas de concurrencia que se citaron (100 consumos del mismo pase, 150 reservas sobre 100 entradas, etc.) se hicieron con scripts propios y están documentadas en `docs/fases/` |
| Pasarela de pagos | **Simulada** (tarjeta `…0000` rechaza, `…9999` tarda 5 s); el reembolso solo queda registrado |

### 11.3 Si algo falla en vivo

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| `Connection refused` | Servicio caído | `.\verificar-servicios.ps1`; `.\arrancar-todo.ps1` |
| T-29d responde `0`/`404` | Falta el mensaje de RabbitMQ | Mira la consola `15672` (colas) y que `ms-inventario` esté arriba |
| T-35 se queda en `EN_SALA` | Aún no es la hora de venta | Espera al `inicioVenta`; el sorteo corre a esa hora exacta |
| `PASE_NO_VALIDO` con un pase recién emitido | Venció (10 min) o ya se usó | Evento nuevo y repite la sala de espera |
| T-69 no llega a `REEMBOLSADA` | `ms-tickets` sin `TICKETS_SIMULAR_FALLO=true` | Reinícialo con la variable en **su** consola |
| `ms-tickets` no arranca tras tocar RabbitMQ | Cola con argumentos distintos | `docker exec teleticket-rabbitmq rabbitmqctl delete_queue tickets.orden-pagada` y reinicia |

---

## 12. Relación con el informe

| Requisito | Pruebas |
|---|---|
| CU-01 Registro | T-09 a T-17 |
| CU-02 Consultar eventos | T-23, T-24, T-29a |
| CU-03/CU-04 Sala de espera y pase | T-30 a T-40 |
| CU-05 Crear orden | T-41 a T-51 |
| CU-06 Pagar | T-52 a T-59 |
| CU-07 Emisión de entradas | T-60 a T-63 |
| CU-14/CU-15 Configurar y publicar | T-18 a T-28 |
| RN-01 Hora de venta no se toca | T-27 |
| RN-02 Llegada tardía = último + 1 | (probado con scripts de carga, FASE-07) |
| RN-03 Sin pase no se compra | T-07, T-41, T-42, T-47 |
| RN-04 Sesión requerida | T-15, T-34a, T-43 |
| RN-05 Máximo por comprador | T-44 |
| RN-08 Pago sin entrada se reembolsa | T-64 a T-71 |
| PRB-02 Cola injusta | T-27, T-35, T-38 |
| PRB-03 Saltarse la cola | T-03, T-07, T-40, T-42, T-59 |
| PRB-04 Cobro sin entrada | T-64 a T-71 |
| PRB-05/06 Entrada falsificable / condiciones | T-63 |
