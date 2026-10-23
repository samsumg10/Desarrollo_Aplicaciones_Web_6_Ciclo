# Plan de implementación — Teleticket

**Fecha:** 21 de octubre de 2026
**Punto de partida:** [ESTADO-ACTUAL.md](ESTADO-ACTUAL.md) — 9 proyectos Spring Boot vacíos
**Meta:** cumplir el 100 % de lo que exige `Proyecto_Investigacion_Teleticket_reducido.docx`

---

## Cómo está pensado este plan

Está dividido en **19 fases (Fase 0 a Fase 18)**, diseñadas con cuatro reglas estrictas:

### Regla 1 — Ninguna fase deja el proyecto inoperativo

Cada fase **solo agrega**. Nunca se rompe lo que ya funcionaba. Al terminar cualquier fase, todo
lo construido en las fases anteriores sigue funcionando igual. Por eso cada fase incluye una
sección **«Prueba de no regresión»**: una lista corta de comprobaciones de fases pasadas que
debes repetir antes de dar la fase por cerrada.

Cuando una fase **sí** necesita modificar algo existente (por ejemplo, agregar una dependencia a
un `pom.xml` que ya funcionaba), se indica explícitamente y se explica por qué es seguro.

### Regla 2 — Todo se puede probar a mano

Cada fase termina con una sección **«Cómo probarlo tú mismo»** con comandos listos para copiar y
pegar en PowerShell, y con lo que **deberías ver** en pantalla. No necesitas saber escribir
pruebas automáticas para verificar que una fase funciona.

Las herramientas de verificación manual son cuatro, todas visuales:

| Herramienta | Dónde | Para qué |
|---|---|---|
| **PowerShell + `curl.exe`** | terminal | Llamar a los endpoints |
| **Consola de RabbitMQ** | http://localhost:15672 (user `guest` / pass `guest`) | Ver mensajes y colas con tus propios ojos |
| **MailHog** | http://localhost:8025 | Ver los correos que envía el sistema |
| **`psql` o DBeaver** | terminal | Mirar las tablas y confirmar qué se guardó |

> ⚠️ **Dos avisos importantes sobre PowerShell en Windows**
>
> **1. Usa siempre `curl.exe`, no `curl`.** En PowerShell, `curl` **no es** el curl de verdad:
> es un alias de `Invoke-WebRequest` y acepta parámetros distintos. En todo este plan se usa
> **`curl.exe`** (con el `.exe`), que sí entiende la sintaxis `-X POST -H "..." -d "..."`.
>
> **2. Las comillas del JSON van escapadas.** Verás el cuerpo de las peticiones escrito así:
> `-d '{\"nombre\":\"Concierto\"}'`. Los `\` **no son un error**: PowerShell pasa el texto tal
> cual al programa, y Windows convierte `\"` en `"` antes de que curl lo reciba. Sin esas barras,
> curl recibe un JSON roto y el servidor responde `400`.
>
> Si prefieres evitar el problema por completo, usa **Postman** (la colección de la Fase 16) o
> guarda el JSON en un archivo y pásalo con `-d "@cuerpo.json"`.

### Regla 3 — El front-end va al final

Las fases 0 a 16 construyen y validan **todo el backend**. El front-end es la **Fase 18**, para
cuando ya tengas claro qué tecnología usar. Mientras tanto, Postman y `curl.exe` hacen de
cliente, que es exactamente como se prueban los microservicios en la industria.

### Regla 4 — Cada fase terminada produce su propio documento

Al cerrar cada fase se crea un archivo `docs/fases/FASE-XX-nombre.md` que explica, en lenguaje
sencillo, **qué se creó** y **qué puedes hacer ahora que antes no podías**. La plantilla está al
final de este documento (sección «Plantilla del documento de fase»).

---

## Mapa general de las fases

| Fase | Nombre | Qué desbloquea | Días est. |
|---|---|---|---|
| **0** ✅ | Cimientos: los 9 servicios arrancan y responden | Ver los 9 vivos | **hecha** |
| **1** ✅ | Infraestructura local con Docker | PostgreSQL + RabbitMQ + MailHog | **hecha** |
| **2** ✅ | API Gateway con enrutamiento real | Un solo puerto de entrada | **hecha** |
| **3** ✅ | `ms-eventos`: catálogo de eventos y zonas | CU-02, CU-14 | **hecha** |
| **4** ✅ | `ms-usuarios`: registro y roles | CU-01 | **hecha** |
| **5** ✅ | Librería común + primer mensaje RabbitMQ | CU-15 | **hecha** |
| **6** ✅ | `ms-inventario`: stock y reservas | CU-05 (parte) | **hecha** |
| **7** ✅ | `ms-cola`: sala de espera, sorteo y pases | CU-03, CU-04 | **hecha** |
| **8** ✅ | `ms-ordenes`: crear orden con Feign | CU-05 (completo) | **hecha** |
| **9** | `ms-pagos` + pagar la orden | CU-06 | 2 |
| **10** | `ms-tickets`: emisión asíncrona firmada | CU-07 | 3 |
| **11** | Saga: reembolso automático | RN-08 | 2–3 |
| **12** | Vencimiento de reservas (TTL + DLX) | RN-06 | 2 |
| **13** | `ms-notificaciones`: correos | CU-10 | 2 |
| **14** | Nominación, descarga y validación | CU-08, CU-09, CU-11 | 2–3 |
| **15** | Dockerización completa | Entregable 6 | 2 |
| **16** | Postman + pruebas de carga | Entregable 7, OBJ 3 | 2–3 |
| **17** | `ms-reclamos` *(opcional)* | CU-12, CU-13 | 2 |
| **18** | **Front-end** | Cliente web | a definir |

**Hito OBJ 2 (30/11/2026):** los 12 casos de uso «Must» quedan cubiertos **al cerrar la Fase 14**.
**Hito OBJ 3 (diciembre 2026):** se mide en la **Fase 16**.

### Dependencias entre fases

```text
F0 ──> F1 ──> F2 ──> F3 ──> F5 ──> F6 ──> F7 ──> F8 ──> F9 ──> F10 ──> F11 ──> F12
                      │                                                         │
                      └──> F4 ─────────────────────────────────> F13 <──────────┘
                                                                  │
                                                        F14 ──> F15 ──> F16 ──> F17 ──> F18
```

Las fases 3 y 4 son independientes entre sí: puedes hacerlas en cualquier orden.

---

## Decisiones técnicas que aplican a todo el plan

Estas decisiones se toman **una sola vez** aquí y se respetan en todas las fases.

| Tema | Decisión | Motivo |
|---|---|---|
| **Spring Boot** | 4.1.1 (el que ya tienen los `pom.xml`) | No tocar lo que ya está |
| **Spring Cloud** | **2025.1.3** | La 2025.1.2 fue la que añadió compatibilidad con Boot 4.1, y la 2025.1.3 ya está descargada en tu `~/.m2` |
| **Base de datos** | PostgreSQL 16 en Docker, **una instancia con 9 bases**, conexión por **variables de entorno** | Lo dice el diseño: *database per service*, pero una sola instancia en desarrollo. Las variables dejan preparado el salto a un contenedor por servicio o a Kubernetes sin tocar código. Motivo completo en [DEC-01](docs/decisiones/DEC-01-bases-de-datos.md) |
| **Esquema de BD** | `schema.sql` por servicio + `spring.sql.init.mode=always` | Más simple de entender que Flyway/Liquibase, y suficiente para el curso |
| **JPA** | `spring.jpa.hibernate.ddl-auto=validate` | Las tablas las crea el `schema.sql`; Hibernate solo comprueba que coincidan |
| **Nombre del servicio de tickets** | carpeta `ms-teletickets`, `spring.application.name=ms-tickets`, ruta `/api/tickets/**` | Alinea con la documentación sin renombrar carpetas |
| **Nombre del gateway** | carpeta `ms-api-gateway`, `spring.application.name=api-gateway` | Igual que arriba |
| **Puerto del gateway** | **8090**, no 8080 | ⚠️ **Desviación del diseño.** En esta máquina el 8080 lo ocupa el contenedor Docker de otro proyecto del curso (`efsrt-iv-proyecto-app-1`). Decidido en la Fase 1 para no tener que apagar ese proyecto cada vez. Motivo completo en [FASE-01](docs/fases/FASE-01-infraestructura.md) |
| **Puerto de PostgreSQL** | **5433**, no 5432 | El 5432 lo ocupa el PostgreSQL 16 instalado en Windows. Dentro de Docker sigue siendo 5432 |
| **Identidad del usuario** | cabecera `X-Usuario-Id`, sin Spring Security | Lo dice la sección 4.3 del diseño. Security llega después y **no obliga a cambiar los servicios** |
| **Formato de fechas** | ISO-8601 UTC (`2026-11-15T19:00:00Z`) | Evita líos de zona horaria |
| **Montos** | `NUMERIC(10,2)` en BD, `BigDecimal` en Java | Nunca usar `double` para dinero |
| **IDs de negocio** | UUID | Lo dice la sección 12.3 del diseño |
| **Paquetes Java** | se mantienen los actuales (`com.teleticket.teletickets`, etc.) | Renombrarlos no aporta nada |

### Estructura de carpetas al final del plan

```text
eclipse-workspace/
├── ms-api-gateway/        ms-ordenes/
├── ms-usuarios/           ms-pagos/
├── ms-eventos/            ms-teletickets/
├── ms-cola/               ms-notificaciones/
├── ms-inventario/         ms-reclamos/        (Fase 17, opcional)
│
├── libs/mensajeria-comun/          ← Fase 5
├── infra/docker-compose.yml        ← Fase 1
├── infra/init-db/01-crear-bases.sql
├── pruebas/
│   ├── teleticket.postman_collection.json   ← Fase 16
│   └── carga/apertura-venta.js              ← Fase 16
├── frontend/                       ← Fase 18
└── docs/fases/
    ├── FASE-00-cimientos.md
    ├── FASE-01-infraestructura.md
    └── …                            ← un .md por fase terminada
```

### Estructura interna de cada microservicio

Para que los 8 servicios se vean iguales y sea fácil moverse entre ellos:

```text
src/main/java/com/teleticket/<servicio>/
├── Ms<Servicio>Application.java
├── config/            ← configuración (RabbitMQ, Feign, CORS…)
├── controller/        ← los endpoints públicos  /api/**
├── controller/internal/ ← los endpoints internos /internal/**
├── service/           ← la lógica de negocio
├── repository/        ← acceso a la base de datos
├── entity/            ← las tablas como clases Java
├── dto/               ← objetos de entrada y salida de la API
├── messaging/         ← publicadores y consumidores de RabbitMQ
├── client/            ← clientes Feign hacia otros servicios
└── exception/         ← errores de negocio y su manejo
```

---

# FASE 0 — Cimientos: que los 9 servicios arranquen y respondan

> **Hoy los servicios arrancan y se apagan en un segundo.** Al terminar esta fase, los 9 estarán
> vivos, cada uno en su puerto, y podrás preguntarle a cada uno «¿estás bien?» y que te responda.

### Objetivo

Convertir los 9 esqueletos en **9 aplicaciones web que se quedan encendidas**, cada una en su
puerto, con un endpoint de salud (`/actuator/health`) que confirma que están funcionando.

### Qué se construye

1. **En los 9 `pom.xml`:** agregar `spring-boot-starter-webmvc` y `spring-boot-starter-actuator`,
   y declarar el bloque `<dependencyManagement>` con `spring-cloud-dependencies` **2025.1.3**
   (aún sin usar ninguna librería de Spring Cloud: solo se deja preparado).
2. **En los 9 `application.properties`:** asignar el puerto y exponer el endpoint de salud.

   | Servicio | Puerto | `spring.application.name` |
   |---|---|---|
   | ms-api-gateway | 8090 | `api-gateway` |
   | ms-usuarios | 8081 | `ms-usuarios` |
   | ms-eventos | 8082 | `ms-eventos` |
   | ms-cola | 8083 | `ms-cola` |
   | ms-inventario | 8084 | `ms-inventario` |
   | ms-ordenes | 8085 | `ms-ordenes` |
   | ms-pagos | 8086 | `ms-pagos` |
   | ms-teletickets | 8087 | `ms-tickets` |
   | ms-notificaciones | 8088 | `ms-notificaciones` |

3. **Un `PingController` temporal** en cada servicio: `GET /ping` devuelve
   `{"servicio":"ms-eventos","estado":"ok"}`. Sirve para ver de inmediato a quién le estás
   pegando. *(Se puede borrar en la Fase 16; hasta entonces es muy útil.)*
4. **Ampliar el `.gitignore` de la raíz** como red de seguridad para las carpetas que se crearán
   en fases siguientes (`libs/`, `pruebas/`), que no traerán uno propio.

   > **Comprobado durante la implementación:** cada microservicio **ya trae su propio
   > `.gitignore`** generado por Spring Initializr, que cubre `target/`, `.settings`,
   > `.classpath` y `.project`. No hay ningún archivo de compilación versionado, así que este
   > paso es menos urgente de lo que parecía.

5. **Scripts de PowerShell** para no abrir 9 terminales a mano cada vez:
   - `arrancar-todo.ps1` — abre una ventana por servicio (o solo los que le indiques)
   - `verificar-servicios.ps1` — tabla con el estado de los 9
   - `detener-todo.ps1` — mata lo que esté escuchando en 8081–8088 y 8090. Hace falta porque
     Maven deja un proceso Java hijo que a veces sobrevive al cerrar la ventana y luego provoca
     el confuso error `Port already in use`. **Solo detiene procesos `java`**: si en un puerto
     hay otra cosa, avisa y no la toca

### Por qué no rompe nada

No hay nada que romper: hoy no funciona ninguna funcionalidad. Agregar dependencias y un
controlador nuevo no afecta a ningún código existente.

### Cómo probarlo tú mismo

**Paso 1 — Verificar que la combinación de versiones resuelve** (esto es lo único arriesgado de
la fase; hazlo primero, en un solo servicio):

```powershell
cd "F:\COMPUTACION_E_INFORMATICA_CIBERTEC\6 - SEXTO CICLO\MODULO 2\DESARROLLO DE APLICACIONES WEB II\Proyectos\eclipse-workspace\ms-eventos"
.\mvnw.cmd -q dependency:tree
```

Si imprime el árbol de dependencias sin errores, la combinación Boot 4.1.1 + Cloud 2025.1.3
funciona y puedes aplicarla a los otros 8. Si aparece un error de incompatibilidad, cambia a la
**opción B** de la sección 8.1 de `ESTADO-ACTUAL.md` (Boot 4.0.6 + Cloud 2025.1.0).

**Paso 2 — Arrancar todo:**

```powershell
cd "F:\...\eclipse-workspace"
.\arrancar-todo.ps1
```

**Paso 3 — Preguntarle a cada servicio si está vivo:**

```powershell
8090,8081,8082,8083,8084,8085,8086,8087,8088 | ForEach-Object {
    Write-Host "Puerto $_ -> " -NoNewline
    try { (curl.exe -s "http://localhost:$_/ping") } catch { Write-Host "SIN RESPUESTA" }
    Write-Host ""
}
```

**Lo que deberías ver:**

```text
Puerto 8090 -> {"servicio":"api-gateway","estado":"ok"}
Puerto 8081 -> {"servicio":"ms-usuarios","estado":"ok"}
...
Puerto 8088 -> {"servicio":"ms-notificaciones","estado":"ok"}
```

**Paso 4 — Comprobar el endpoint de salud estándar:**

```powershell
curl.exe -s http://localhost:8082/actuator/health
```
→ `{"status":"UP"}`

### Criterios de aceptación

- [x] Los 9 servicios compilan con `mvnw.cmd clean package`
- [x] Los 9 arrancan y **se quedan encendidos** (no se apagan solos)
- [x] Los 9 responden `/ping` y `/actuator/health`
- [x] Ningún puerto choca con otro
- [x] `git status` no muestra archivos de compilación

### Documento a generar

✅ [`docs/fases/FASE-00-cimientos.md`](docs/fases/FASE-00-cimientos.md) — **FASE COMPLETADA el
21/10/2026.** Verificado: los 9 compilan sin red, los 9 responden en `UP`, y los scripts de
arranque, verificación y parada funcionan.

---

# FASE 1 — Infraestructura local con Docker

> Aquí no se escribe nada de Java. Se levanta la «sala de máquinas»: la base de datos, el
> mensajero y el buzón de correo falso.

### Objetivo

Tener PostgreSQL con las 9 bases creadas, RabbitMQ con su consola web y MailHog, todo
levantándose con **un solo comando**.

### Qué se construye

1. **`infra/docker-compose.yml`** con tres contenedores:

   | Contenedor | Imagen | Puertos | Para qué |
   |---|---|---|---|
   | `teleticket-postgres` | `postgres:16-alpine` | 5433→5432 | Las 9 bases de datos |
   | `teleticket-rabbitmq` | `rabbitmq:3-management-alpine` | 5672 (mensajes), 15672 (consola web) | La mensajería asíncrona |
   | `teleticket-mailhog` | `mailhog/mailhog` | 1025 (SMTP), 8025 (web) | Capturar los correos sin enviarlos |

   > **Nota sobre el puerto 5433:** tu máquina **ya tiene PostgreSQL 16 instalado** y
   > probablemente ocupando el 5432. Para no pelear con él, el PostgreSQL de Docker se expone en
   > el **5433**. Si compruebas que el 5432 está libre, puedes usarlo y ahorrarte la diferencia.

2. **`infra/init-db/01-crear-bases.sql`** que crea las 9 bases al arrancar el contenedor:
   `db_usuarios`, `db_eventos`, `db_cola`, `db_inventario`, `db_ordenes`, `db_pagos`,
   `db_tickets`, `db_notificaciones`, `db_reclamos`.
3. **`infra/README.md`** con los comandos de uso: levantar, apagar, ver logs, borrar todo y
   empezar de cero, más una sección «Si algo sale mal».
4. **Volúmenes de Docker** para que los datos sobrevivan al apagar los contenedores.
5. **`verificar-infra.ps1`** en la raíz: comprueba de una pasada el daemon de Docker, los 3
   contenedores, las 9 bases y las 2 consolas web. Es el equivalente de
   `verificar-servicios.ps1` de la Fase 0, pero para la infraestructura.
6. **Healthchecks** en PostgreSQL y RabbitMQ. No hacen falta hoy, pero en la Fase 15 son lo que
   permite que los microservicios esperen a que la base esté lista antes de arrancar.

### Por qué no rompe nada

No se toca ni un archivo de los microservicios. Si algo sale mal, se borran los contenedores y
el código Java queda intacto.

### Cómo probarlo tú mismo

```powershell
cd "F:\...\eclipse-workspace\infra"
docker compose up -d
docker compose ps
```

**Lo que deberías ver:** tres contenedores en estado `running` o `healthy`.

**Comprobación 1 — PostgreSQL tiene las 9 bases:**

```powershell
docker exec -it teleticket-postgres psql -U teleticket -c "\l"
```
→ aparecen `db_usuarios`, `db_eventos`, … `db_reclamos`

**Comprobación 2 — RabbitMQ:** abre http://localhost:15672 en el navegador.
Usuario `guest`, contraseña `guest`. Deberías ver el panel de control (todavía sin colas: es
normal, las crea el código en la Fase 5).

**Comprobación 3 — MailHog:** abre http://localhost:8025. Deberías ver una bandeja de entrada
vacía.

### Prueba de no regresión

- [x] Los 9 servicios de la Fase 0 siguen arrancando y respondiendo `/ping`
      ⚠️ **Esta prueba destapó el conflicto del puerto 8080.** El gateway se movió al 8090

### Criterios de aceptación

- [x] `docker compose up -d` levanta los 3 contenedores sin errores
- [x] Las 9 bases existen
- [x] La consola de RabbitMQ abre en el navegador
- [x] MailHog abre en el navegador
- [x] Al hacer `docker compose down` y `up` otra vez, los datos siguen ahí

### Documento a generar

✅ [`docs/fases/FASE-01-infraestructura.md`](docs/fases/FASE-01-infraestructura.md) — **FASE
COMPLETADA el 22/10/2026.** Verificado: 3 contenedores `healthy`, las 9 bases creadas solas,
conexión desde Windows en el 5433, persistencia comprobada con un `down`/`up`, y los 9
microservicios respondiendo (gateway en 8090).

---

# FASE 2 — API Gateway con enrutamiento real

> Hasta ahora, para hablar con cada servicio tenías que saber su puerto. Desde aquí, **un solo
> puerto** (`8090`) te lleva a todos.

### Objetivo

Que el gateway reciba todas las peticiones del cliente y las reenvíe al microservicio correcto,
agregando la cabecera de rastreo `X-Request-Id`.

### Qué se construye

1. **Dependencia** `spring-cloud-starter-gateway-server-webmvc` en `ms-api-gateway`.
2. **Tabla de rutas** (sección 4.2 del diseño):

   | Ruta pública | Destino |
   |---|---|
   | `/api/usuarios/**` | `localhost:8081` |
   | `/api/eventos/**` | `localhost:8082` |
   | `/api/cola/**` | `localhost:8083` |
   | `/api/ordenes/**` | `localhost:8085` |
   | `/api/tickets/**` | `localhost:8087` |
   | *(sin ruta)* | ms-inventario, ms-pagos, ms-notificaciones |

   > **Lo más importante de esta tabla es lo que NO está.** `ms-inventario`, `ms-pagos` y
   > `ms-notificaciones` **no tienen ruta**. Esa ausencia es una medida de seguridad: así nadie
   > puede saltarse la cola reservando stock directamente. Es parte de la solución a PRB-03.

3. **Filtro global `RequestIdFilter`:** si la petición no trae `X-Request-Id`, el gateway genera
   un UUID y lo agrega. Sirve para seguir una compra completa por los 8 servicios en los logs.
4. **Configuración de CORS** para que el futuro front-end pueda llamar al gateway.
5. **Filtro `PaseCompraFilter`:** rechaza con `400` cualquier `POST /api/ordenes` que no traiga
   la cabecera `X-Pase-Compra`. *(Se puede dejar preparado y desactivado hasta la Fase 8, o
   activarlo ya: como todavía no existe `ms-ordenes`, no afecta a nada.)*

### Por qué no rompe nada

Las rutas nuevas conviven con el `/ping` de cada servicio, al que se puede seguir llegando
directamente por su puerto. Si el gateway falla, los 8 servicios siguen respondiendo por su
puerto propio.

### Cómo probarlo tú mismo

**Prueba 1 — El gateway enruta:**

```powershell
curl.exe -s http://localhost:8090/api/eventos/ping
```
→ `{"estado":"ok","servicio":"ms-eventos","puerto":8082,...}`

> ⚠️ **Corregido durante la implementación.** El gateway reenvía la **ruta completa**: no recorta
> el prefijo. Por eso `/api/eventos/ping` llega a ms-eventos como `/api/eventos/ping`, y el `/ping`
> de la Fase 0 está en la raíz. Se añadió a los 5 servicios ruteados una segunda ruta
> `@GetMapping({ "/ping", "/api/eventos/ping" })` para que esta prueba tenga sentido. Detalle en
> [FASE-02](docs/fases/FASE-02-api-gateway.md).

Fíjate en lo que acaba de pasar: pediste al puerto **8090** y te respondió el servicio del puerto
**8082**. Eso es enrutamiento.

**Prueba 2 — Los servicios internos NO son alcanzables:**

```powershell
curl.exe -s -o NUL -w "%{http_code}" http://localhost:8090/api/inventario/ping
```
→ `404` ✅ **Esto es un éxito, no un error.** Significa que el gateway no tiene ruta hacia
inventario, que es exactamente lo que queremos.

Compruébalo: ese mismo servicio **sí** responde por su puerto directo:

```powershell
curl.exe -s http://localhost:8084/ping
```
→ `{"servicio":"ms-inventario","estado":"ok"}`

**Prueba 3 — La cabecera de rastreo se agrega:**

```powershell
curl.exe -s -D - http://localhost:8090/api/eventos/ping -o NUL
```
→ entre las cabeceras de respuesta debe aparecer `X-Request-Id: <un-uuid>`

### Prueba de no regresión

- [x] Los 9 servicios siguen respondiendo `/ping` por su puerto directo
- [x] Los contenedores de Docker siguen arriba

### Criterios de aceptación

- [x] Las 5 rutas públicas enrutan correctamente
- [x] Las 3 rutas internas devuelven `404` desde el gateway
- [x] `X-Request-Id` aparece en todas las respuestas
- [x] `POST /api/ordenes` sin `X-Pase-Compra` → `400`, y `POST /api/ordenes/{id}/pagar` sin pase
      **no** se bloquea

### Documento a generar

✅ [`docs/fases/FASE-02-api-gateway.md`](docs/fases/FASE-02-api-gateway.md) — **FASE COMPLETADA
el 22/10/2026.** Verificado: 5 rutas enrutando, 3 servicios internos inalcanzables, `X-Request-Id`
de punta a punta y el filtro del pase rechazando con `400`.

---

# FASE 3 — `ms-eventos`: catálogo de eventos y zonas

> **El primer microservicio de verdad.** Aquí aparece la primera base de datos y el primer CRUD.
> Cubre CU-02 y CU-14.

### Objetivo

Que un operador pueda crear un evento con sus zonas, precios y aforos, y que cualquiera pueda
consultarlos desde el gateway.

### Qué se construye

1. **Dependencias:** `spring-boot-starter-data-jpa`, `postgresql`, `spring-boot-starter-validation`.
2. **Conexión a `db_eventos`** en `application.properties`. **Esta es la plantilla que copiarán
   los 7 servicios siguientes cambiando solo `DB_NAME`**, así que conviene dejarla bien desde aquí:

   ```properties
   # Lo que va despues de los dos puntos es el VALOR POR DEFECTO: si no hay
   # variable de entorno, usa ese. Por eso funciona desde Eclipse sin
   # configurar nada, y en Docker basta con pasarle DB_HOST y DB_PORT.
   spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:db_eventos}
   spring.datasource.username=${DB_USER:teleticket}
   spring.datasource.password=${DB_PASSWORD:teleticket}

   # Las tablas las crea schema.sql; Hibernate solo comprueba que coincidan.
   spring.jpa.hibernate.ddl-auto=validate
   spring.sql.init.mode=always
   ```

   | Escenario | `DB_HOST` | `DB_PORT` |
   |---|---|---|
   | Desde Eclipse (fases 3–14) | *(sin definir)* → `localhost` | `5433` |
   | Como contenedor (Fase 15) | `postgres` | `5432` |
   | Un contenedor por servicio *(futuro)* | `postgres-eventos` | `5432` |

   > **Por qué partir la URL en variables** y no usar `SPRING_DATASOURCE_URL` con la URL completa:
   > así, cambiar de escenario es cambiar **un solo valor** en vez de repetir la URL JDBC entera
   > nueve veces en el `docker-compose.yml`. Ver [DEC-01](docs/decisiones/DEC-01-bases-de-datos.md).
3. **`schema.sql`** con las 3 tablas:

   | Tabla | Campos | Restricciones |
   |---|---|---|
   | `evento` | id, nombre, fecha, recinto, inicio_venta, estado, max_por_cliente, condiciones_ingreso, reglas_cola | — |
   | `zona` | id, evento_id, nombre, precio, aforo | `UNIQUE(evento_id, nombre)` |
   | `preventa` | id, evento_id, zona_id, banco, inicio, fin, descuento, stock | — |

4. **Entidades JPA + repositorios** para las 3 tablas.
5. **API pública:**

   | Método | Ruta | Quién | Qué hace |
   |---|---|---|---|
   | `POST` | `/api/eventos` | operador | Crear evento en estado `BORRADOR` |
   | `POST` | `/api/eventos/{id}/zonas` | operador | Agregar una zona |
   | `GET` | `/api/eventos` | todos | Listar eventos publicados |
   | `GET` | `/api/eventos/{id}` | todos | Ver un evento con sus zonas |

6. **API interna** (sin ruta en el gateway):

   | Método | Ruta | Quién lo llama | Para qué |
   |---|---|---|---|
   | `GET` | `/internal/eventos/{id}/zonas/{zonaId}` | ms-ordenes (Fase 8) | Precio y máximo por cliente |
   | `GET` | `/internal/eventos/{id}/condiciones` | ms-tickets (Fase 10) | Condiciones de ingreso |

7. **Manejo de errores** con `@RestControllerAdvice`: un evento que no existe devuelve `404` con
   un cuerpo claro, no un error feo de Java.
8. **Regla RN-01 (primera mitad):** no se puede modificar `inicio_venta` si el evento ya está
   `EN_VENTA`. *(La publicación en sí llega en la Fase 5.)*

### Por qué no rompe nada

`ms-eventos` no llama a ningún otro servicio ni publica mensajes todavía. Es una isla.

### Cómo probarlo tú mismo

**Paso 1 — Crear un evento:**

```powershell
curl.exe -s -X POST http://localhost:8090/api/eventos `
  -H "Content-Type: application/json" `
  -H "X-Usuario-Id: 00000000-0000-0000-0000-000000000001" `
  -d '{\"nombre\":\"Concierto Prueba 2026\",\"fecha\":\"2026-12-20T20:00:00Z\",\"recinto\":\"Estadio Nacional\",\"inicioVenta\":\"2026-11-15T10:00:00Z\",\"maxPorCliente\":4,\"condicionesIngreso\":\"Entrada digital valida en el celular. No se exige impresion. Presentar DNI.\"}'
```
→ devuelve `201` y el JSON del evento con su `id` (un UUID). **Cópialo**, lo necesitas.

**Paso 2 — Agregarle dos zonas:**

```powershell
$EVENTO = "<pega-aqui-el-id>"
curl.exe -s -X POST "http://localhost:8090/api/eventos/$EVENTO/zonas" -H "Content-Type: application/json" -d '{\"nombre\":\"VIP\",\"precio\":450.00,\"aforo\":100}'
curl.exe -s -X POST "http://localhost:8090/api/eventos/$EVENTO/zonas" -H "Content-Type: application/json" -d '{\"nombre\":\"General\",\"precio\":150.00,\"aforo\":500}'
```

**Paso 3 — Consultarlo:**

```powershell
curl.exe -s "http://localhost:8090/api/eventos/$EVENTO"
```
→ el evento con sus dos zonas.

**Paso 4 — Mirar la base de datos con tus propios ojos:**

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_eventos -c "SELECT nombre, precio, aforo FROM zona;"
```

**Paso 5 — Probar que los errores están bien manejados:**

```powershell
curl.exe -s http://localhost:8090/api/eventos/11111111-1111-1111-1111-111111111111
```
→ `404` con un mensaje legible, no un *stack trace*.

**Paso 6 — Probar la restricción de zona duplicada:** vuelve a crear la zona "VIP" del Paso 2.
→ debe devolver `409 Conflict` con un mensaje claro.

### Prueba de no regresión

- [x] Los otros 8 servicios siguen respondiendo `/ping`
- [x] El gateway sigue devolviendo `404` para `/api/inventario/**`

### Criterios de aceptación

- [x] Se crea un evento con zonas y se recupera por el gateway
- [x] Los datos están realmente en `db_eventos` (verificado con `psql`)
- [x] Los errores devuelven códigos HTTP correctos con mensajes legibles
- [x] Una zona duplicada en el mismo evento se rechaza
- [x] **RN-01 probada en los dos sentidos:** cambiar la hora da `200` antes de publicar y `409`
      después

### Documento a generar

✅ [`docs/fases/FASE-03-ms-eventos.md`](docs/fases/FASE-03-ms-eventos.md) — **FASE COMPLETADA
el 22/10/2026.** Verificado: 3 tablas creadas, evento publicado con 3 zonas y 900 de aforo, RN-01
congelando la hora (`409 EVENTO_CONGELADO`) y la API interna inalcanzable por el gateway.

---

# FASE 4 — `ms-usuarios`: registro y roles

> Cubre CU-01. Es el servicio más sencillo; buen momento para afianzar el patrón de la Fase 3.

### Objetivo

Registrar compradores y personal con su rol, y ofrecer el endpoint interno que
`ms-notificaciones` usará más adelante para saber a quién escribirle.

### Qué se construye

1. **Mismas dependencias** que `ms-eventos` + `spring-security-crypto` (solo para cifrar
   contraseñas con BCrypt; **no** es Spring Security completo todavía).
   > **Conexión:** misma plantilla de la Fase 3, cambiando solo `DB_NAME=db_usuarios`.
   > Ver [DEC-01](docs/decisiones/DEC-01-bases-de-datos.md).
2. **`schema.sql`** con la tabla `usuario` (id, dni, nombres, email, password_hash, rol,
   creado_en) con `UNIQUE(dni)` y `UNIQUE(email)`.
3. **Roles** como enumeración: `COMPRADOR`, `OPERADOR`, `CONTROLADOR`, `ATENCION`.
4. **API pública:**

   | Método | Ruta | Qué hace |
   |---|---|---|
   | `POST` | `/api/usuarios` | Registro. Devuelve el usuario **sin** la contraseña |
   | `GET` | `/api/usuarios/me` | Datos del usuario de la cabecera `X-Usuario-Id` |

5. **API interna:** `GET /internal/usuarios/{id}/contacto` → `{nombres, email}`.
6. **Datos de prueba (`data.sql`)**: 3 compradores, 1 operador y 1 controlador ya creados, para
   no tener que registrarlos a mano en cada prueba de las fases siguientes.

> **Sobre el login:** en esta etapa **no hay login con JWT**. El diseño (sección 4.3) dice que
> hasta que llegue Spring Security, el cliente envía la cabecera `X-Usuario-Id` y cada servicio
> confía en ella. Cuando llegue Security, solo cambia **quién escribe** esa cabecera (el
> gateway, tras validar el JWT) y **ningún microservicio necesita cambiar**.

### Por qué no rompe nada

Servicio aislado, igual que `ms-eventos`. No llama a nadie ni publica mensajes.

### Cómo probarlo tú mismo

```powershell
curl.exe -s -X POST http://localhost:8090/api/usuarios `
  -H "Content-Type: application/json" `
  -d '{\"dni\":\"70123456\",\"nombres\":\"Daniel Fuentes\",\"email\":\"daniel@correo.com\",\"password\":\"Secreta123\",\"rol\":\"COMPRADOR\"}'
```
→ `201` con el usuario y su `id`. **Verifica que la respuesta NO incluye la contraseña.**

**Comprobar que la contraseña está cifrada:**

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_usuarios -c "SELECT email, password_hash FROM usuario;"
```
→ debes ver algo como `$2a$10$...`, **nunca** `Secreta123` en texto plano.

**Probar el DNI duplicado:** repite la petición del registro.
→ `409 Conflict`.

**Probar el endpoint interno:**

```powershell
curl.exe -s http://localhost:8081/internal/usuarios/<id>/contacto
```
→ `{"nombres":"Daniel Fuentes","email":"daniel@correo.com"}`

**Y confirmar que ese endpoint NO se alcanza desde fuera:**

```powershell
curl.exe -s -o NUL -w "%{http_code}" http://localhost:8090/internal/usuarios/<id>/contacto
```
→ `404` ✅

### Prueba de no regresión

- [ ] Los eventos de la Fase 3 siguen creándose y consultándose

### Criterios de aceptación

- [x] Registro funcionando con validaciones (email válido, DNI de 8 dígitos, contraseña mínima)
- [x] Contraseña cifrada con BCrypt y nunca devuelta en las respuestas
- [x] DNI y email duplicados rechazados con `409`
- [x] Endpoint interno accesible por puerto directo e inaccesible por el gateway
- [x] *(añadido)* El registro público no permite elegir el rol: siempre `COMPRADOR`

### Documento a generar

✅ [`docs/fases/FASE-04-ms-usuarios.md`](docs/fases/FASE-04-ms-usuarios.md) — **FASE COMPLETADA el
23/10/2026.** 6 usuarios sembrados (ids `…0001` a `…0006`, contraseña `Teleticket123`).

---

# FASE 5 — Librería común y el primer mensaje de RabbitMQ

> **La fase conceptualmente más importante del proyecto.** Aquí aparece la comunicación
> asíncrona, que es la mitad de la nota del curso. Cubre CU-15.

### Objetivo

Que al publicar un evento, `ms-eventos` emita el mensaje `evento.publicado` a RabbitMQ, y que tú
puedas **verlo con tus propios ojos** en la consola web de RabbitMQ.

### Qué es RabbitMQ, en una frase

> Es un **cartero**. Un servicio le deja una carta («se publicó el evento X») sin saber quién la
> leerá, y el cartero la entrega en los buzones de todos los interesados. El que envía no espera
> respuesta y sigue trabajando.

Las piezas tienen nombres propios:

| Pieza | Qué es | Ejemplo en el proyecto |
|---|---|---|
| **Exchange** | La oficina de correos: recibe las cartas y decide a qué buzones van | `teleticket.exchange` |
| **Routing key** | La dirección escrita en el sobre | `evento.publicado` |
| **Cola (queue)** | El buzón de un destinatario | `cola.evento-publicado` |
| **Binding** | La regla «las cartas dirigidas a X van al buzón Y» | — |
| **DLX** | La oficina de cartas no entregadas | `teleticket.dlx` |

### Qué se construye

1. **`libs/mensajeria-comun/`**, un módulo Maven independiente (entregable N.º 5 del informe)
   que contiene:
   - `SobreMensaje<T>`: la envoltura estándar de todo mensaje, con `eventoId` (UUID del mensaje),
     `tipo`, `fecha`, `correlationId` (el `X-Request-Id`) y `datos`.
   - La interfaz **`PublicadorEventos`** con un único método `publicar(routingKey, mensaje)`.
   - Los DTO de los mensajes: `EventoPublicado`, `TurnoHabilitado`, `OrdenPagada`,
     `TicketsEmitidos`, `EmisionFallida`, `ReembolsoSolicitado`, `PagoReembolsado`,
     `OrdenReembolsada`, `OrdenExpirada`, `ReservaLiberada`, `TicketValidado`.
   - Las constantes con los nombres de exchanges y routing keys, **en un solo sitio**, para que
     nunca haya un nombre mal escrito en un servicio.

   > **Por qué la interfaz `PublicadorEventos` importa:** la sección 11 del diseño dice que más
   > adelante algunos mensajes migrarán a Kafka. Si todos los servicios publican a través de esta
   > interfaz, cambiar a Kafka será **cambiar una implementación, no el negocio**.

2. **Implementación `PublicadorRabbitMQ`** que usa `RabbitTemplate` con **publisher confirms**
   (para que si el broker no confirma la recepción, se reintente).
3. **En `ms-eventos`:** dependencia `spring-boot-starter-amqp`, la declaración del exchange, y el
   endpoint `PUT /api/eventos/{id}/publicar` que:
   - valida que el evento tenga al menos una zona,
   - cambia su estado a `EN_VENTA`,
   - **bloquea** `inicio_venta`, `reglas_cola` y `condiciones_ingreso` (RN-01),
   - publica `evento.publicado` con las zonas, aforos, hora de venta y reglas.
4. **Tabla `mensaje_procesado`** (`PRIMARY KEY(evento_id)`) en el `schema.sql` de los servicios
   que consumirán mensajes. Sirve para **descartar mensajes repetidos**: RabbitMQ puede entregar
   el mismo mensaje dos veces, y esta tabla evita, por ejemplo, crear el stock dos veces.
5. **Colas declaradas pero todavía sin consumidor:** `cola.evento-publicado` e
   `inventario.evento-publicado`. Así, al publicar un evento, verás los mensajes **acumulados y
   esperando** en la consola de RabbitMQ. Es la demostración más clara de qué hace un mensaje.

### Por qué no rompe nada

El endpoint `PUT /publicar` es **nuevo**: los endpoints de la Fase 3 no cambian. Si RabbitMQ
está caído, solo falla `/publicar`; crear y consultar eventos sigue funcionando.

### Cómo probarlo tú mismo

**Paso 1 — Abre la consola de RabbitMQ** en http://localhost:15672 (guest/guest) y ve a la
pestaña **Queues**. Deberías ver las dos colas con **0 mensajes**.

**Paso 2 — Crea un evento nuevo y publícalo** (el de la Fase 3 ya está `EN_VENTA`, y al
publicarse antes de existir el mensaje no emitió nada: `PUT /publicar` daría `409 YA_PUBLICADO`.
Los comandos para crearlo están en [FASE-05](docs/fases/FASE-05-mensajeria-rabbitmq.md)):

```powershell
curl.exe -s -X PUT "http://localhost:8090/api/eventos/$EVENTO/publicar" -H "X-Usuario-Id: 00000000-0000-0000-0000-000000000001"
```
→ `200` y el evento en estado `EN_VENTA`.

**Paso 3 — Vuelve a la consola de RabbitMQ y refresca.**

🎉 **Las dos colas ahora tienen 1 mensaje cada una.** Un solo `PUT` produjo dos cartas en dos
buzones distintos. Eso es el patrón *publish/subscribe*.

**Paso 4 — Lee el mensaje:** haz clic en `cola.evento-publicado` → *Get messages* → *Get
Message(s)*. Verás el JSON con `eventoId`, `tipo`, `fecha`, `correlationId` y los datos del
evento con sus zonas.

> Usa **«Requeue: Yes»** para que el mensaje vuelva a la cola. Lo necesitarás en la Fase 6.

**Paso 5 — Probar RN-01 (la hora queda bloqueada):** intenta cambiar `inicio_venta` del evento
ya publicado.
→ debe devolver `409` con un mensaje tipo *«No se puede modificar la hora de venta de un evento
EN_VENTA»*. **Esa única línea de código es la respuesta directa a PRB-02 y al incidente de
2017.**

### Prueba de no regresión

- [ ] Crear y consultar eventos (Fase 3) sigue funcionando
- [ ] El registro de usuarios (Fase 4) sigue funcionando
- [ ] Los 9 servicios responden `/ping`

### Criterios de aceptación

- [ ] El módulo `libs/mensajeria-comun` compila e se instala con `mvnw install`
- [ ] Publicar un evento deja mensajes visibles en 2 colas de RabbitMQ
- [ ] El JSON del mensaje tiene los 5 campos del formato estándar
- [ ] La hora de venta no se puede modificar después de publicar

### Documento a generar

`docs/fases/FASE-05-mensajeria-rabbitmq.md`

---

# FASE 6 — `ms-inventario`: stock y reservas

> El primer **consumidor** de mensajes. Aquí ves el otro lado del cartero.

### Objetivo

Que al publicarse un evento, `ms-inventario` cree automáticamente el stock de cada zona, y que
ofrezca los endpoints internos para reservar.

### Qué se construye

1. **Dependencias:** JPA, PostgreSQL, AMQP + el módulo `mensajeria-comun`.
   > **Conexión:** misma plantilla de la Fase 3, cambiando solo `DB_NAME=db_inventario`.
   > Ver [DEC-01](docs/decisiones/DEC-01-bases-de-datos.md).
2. **`schema.sql`:** `stock_zona` (zona_id, evento_id, total, disponible, version, con
   `CHECK (disponible >= 0)`), `reserva` (id, orden_id, zona_id, cantidad, estado, vence_en, con
   `UNIQUE(orden_id)`) y `mensaje_procesado`.
3. **Consumidor de `evento.publicado`:** crea una fila de `stock_zona` por cada zona, con
   `disponible = aforo`. **Idempotente**: si el mensaje llega dos veces, el `eventoId` ya está en
   `mensaje_procesado` y se descarta.
4. **Endpoints internos:**

   | Método | Ruta | Qué hace |
   |---|---|---|
   | `POST` | `/internal/reservas` | Reserva N entradas de una zona → estado `ACTIVA` |
   | `PUT` | `/internal/reservas/{id}/en-pago` | Protege la reserva mientras se cobra |
   | `PUT` | `/internal/reservas/{id}/activar` | La devuelve a `ACTIVA` si el pago se rechazó |
   | `GET` | `/internal/zonas/{zonaId}/disponible` | Cuántas quedan |

5. **La consulta atómica anti-sobreventa**, implementada con `@Modifying @Query`:

   ```sql
   UPDATE stock_zona SET disponible = disponible - :cantidad
   WHERE zona_id = :zonaId AND disponible >= :cantidad
   ```

   Si devuelve `1`, reservado. Si devuelve `0`, no había stock. **No hace falta ningún bloqueo
   manual**: PostgreSQL garantiza que dos peticiones simultáneas no puedan restar sobre el mismo
   valor.

6. **Confirmación manual del `ack`:** el mensaje solo se confirma **después** de guardar en la
   base de datos. Si el servicio se cae a mitad, el mensaje vuelve a la cola y no se pierde nada.

### Por qué no rompe nada

Solo **agrega** un consumidor a una cola que ya existía vacía desde la Fase 5 y crea endpoints
nuevos en un servicio que hasta ahora solo tenía `/ping`.

### Cómo probarlo tú mismo

**Paso 1 — Arranca `ms-inventario` y mira la consola de RabbitMQ.**
El mensaje que dejaste en `inventario.evento-publicado` en la Fase 5 **desaparece de la cola**
en cuanto el servicio arranca: acaba de ser consumido. 🎉

**Paso 2 — Comprueba que el stock se creó solo:**

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_inventario -c "SELECT zona_id, total, disponible FROM stock_zona;"
```
→ dos filas: VIP con 100/100 y General con 500/500. **Nadie las creó a mano**: llegaron por un
mensaje.

**Paso 3 — Reserva 2 entradas** (por el puerto directo, porque es endpoint interno):

```powershell
curl.exe -s -X POST http://localhost:8084/internal/reservas `
  -H "Content-Type: application/json" `
  -d '{\"ordenId\":\"aaaaaaaa-0000-0000-0000-000000000001\",\"zonaId\":\"<id-zona-VIP>\",\"cantidad\":2}'
```
→ `201` con la reserva en estado `ACTIVA`.

**Paso 4 — Verifica que el stock bajó:**

```powershell
curl.exe -s http://localhost:8084/internal/zonas/<id-zona-VIP>/disponible
```
→ `98`

**Paso 5 — La prueba de la sobreventa (la más importante):** intenta reservar 200 entradas de la
zona VIP, que solo tiene 98 disponibles.
→ debe devolver `409` con *«Sin stock suficiente»* y el disponible debe seguir en **98**, no en
`-102`.

**Paso 6 — Idempotencia:** en la consola de RabbitMQ, publica a mano el mismo mensaje
`evento.publicado` otra vez (pestaña **Exchanges** → `teleticket.exchange` → *Publish message*,
con el mismo JSON y la misma routing key).
→ el stock **no** se duplica. Compruébalo con el `SELECT` del Paso 2.

### Prueba de no regresión

- [ ] Publicar un evento nuevo sigue creando su stock automáticamente
- [ ] Las fases 3 y 4 siguen funcionando
- [ ] El gateway sigue devolviendo `404` para `/api/inventario/**`

### Criterios de aceptación

- [ ] El stock se crea solo al publicar un evento
- [ ] Reservar baja el disponible
- [ ] No se puede reservar más de lo disponible (probado explícitamente)
- [ ] Un mensaje repetido no duplica el stock

### Documento a generar

`docs/fases/FASE-06-ms-inventario.md`

---

# FASE 7 — `ms-cola`: sala de espera, sorteo y pases

> **El servicio estrella del proyecto.** Es la respuesta directa a PRB-01, PRB-02 y PRB-03, y lo
> que diferencia este trabajo de un CRUD cualquiera. Cubre CU-03 y CU-04.

### Objetivo

Que un comprador entre a la sala de espera, que a la hora exacta se haga **un único sorteo**, y
que cada 10 segundos se habilite a un grupo y se le entregue su **pase de compra**.

### Qué se construye

1. **Dependencias:** JPA, PostgreSQL, AMQP + `mensajeria-comun`. **Ningún Feign**: el diseño es
   explícito en que `ms-cola` no llama a nadie, porque es el servicio que recibe la avalancha y no
   puede depender de otro en el peor momento. Recibe los datos del evento **por mensaje**.
   > **Conexión:** misma plantilla de la Fase 3, cambiando solo `DB_NAME=db_cola`.
   > Ver [DEC-01](docs/decisiones/DEC-01-bases-de-datos.md).
2. **`schema.sql`:**

   | Tabla | Campos | Restricción clave |
   |---|---|---|
   | `venta_evento` | evento_id, inicio_venta, estado_sorteo, capacidad_por_ciclo | copia local que llega por mensaje |
   | `turno` | id, evento_id, usuario_id, posicion, estado, ingreso_en, habilitado_en | **`UNIQUE(evento_id, usuario_id)`** — un turno por persona |
   | `pase_compra` | codigo (UUID, PK), turno_id, usuario_id, evento_id, expira_en, estado | — |

3. **Consumidor de `evento.publicado`:** guarda la copia local y prepara la sala de espera.
4. **API pública:**

   | Método | Ruta | Qué hace |
   |---|---|---|
   | `POST` | `/api/cola/eventos/{eventoId}/ingresar` | Entrar a la sala. **Idempotente**: si ya tienes turno, te devuelve el mismo |
   | `GET` | `/api/cola/turnos/{turnoId}` | Tu posición, el tiempo estimado y, cuando llega tu turno, tu pase |

   Si no llega la cabecera `X-Usuario-Id` → `401` (RN-04).

5. **API interna:** `POST /internal/pases/{codigo}/consumir`, con la consulta atómica:

   ```sql
   UPDATE pase_compra SET estado = 'USADO'
   WHERE codigo = :codigo AND usuario_id = :usuarioId
     AND estado = 'VIGENTE' AND expira_en > now()
   ```
   0 filas afectadas = pase inválido, vencido, ajeno o ya usado. **Un pase, una compra.**

6. **Tarea programada del sorteo** (`@Scheduled`): a la hora `inicio_venta`, asigna una posición
   aleatoria a **todos** los turnos `EN_SALA` **en una sola transacción**, y marca el sorteo como
   realizado para que no se repita jamás.
7. **Tarea programada de admisión** (`@Scheduled(fixedDelay = 10000)`): cada 10 segundos habilita
   a los siguientes `capacidad_por_ciclo` turnos, crea un pase de 10 minutos para cada uno y
   publica `turno.habilitado`.
8. **Regla de llegada tardía (RN-02):** quien entra después del sorteo recibe `max(posicion) + 1`.
9. **Configuración para pruebas:** `teleticket.cola.capacidad-por-ciclo` y
   `teleticket.cola.ciclo-segundos` en el `application.properties`, para poder acelerar las
   pruebas sin tocar código.

### Por qué no rompe nada

Servicio independiente. Consume una cola que ya existía y sus endpoints son nuevos.

### Cómo probarlo tú mismo

> **Truco para no esperar:** crea un evento de prueba con `inicioVenta` **2 minutos en el
> futuro**. Así ves el sorteo en vivo sin esperar horas.

**Paso 1 — Crea y publica un evento con venta en 2 minutos** (fases 3 y 5).

**Paso 2 — Mete a 5 compradores en la sala de espera:**

```powershell
1..5 | ForEach-Object {
  $u = "00000000-0000-0000-0000-00000000000$_"
  curl.exe -s -X POST "http://localhost:8090/api/cola/eventos/$EVENTO/ingresar" -H "X-Usuario-Id: $u"
  Write-Host ""
}
```
→ 5 turnos en estado `EN_SALA`, todos con `posicion: null` (el sorteo aún no ocurrió).

**Paso 3 — Prueba la idempotencia (RN-04):** vuelve a ejecutar el ingreso del usuario 1.
→ te devuelve **el mismo `turnoId`**, no uno nuevo. Esto significa que volver a iniciar sesión,
o entrar desde otro dispositivo, **no te hace perder tu lugar**.

**Paso 4 — Prueba RN-04:** ingresa sin la cabecera `X-Usuario-Id`.
→ `401`.

**Paso 5 — Espera a la hora de venta y mira el sorteo:**

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_cola -c "SELECT usuario_id, posicion, estado FROM turno ORDER BY posicion;"
```
→ los 5 turnos ahora tienen una posición **aleatoria** del 1 al 5. 🎉
**Vuelve a ejecutar el mismo `SELECT` un minuto después: las posiciones son las mismas.** El
sorteo fue **único**, no se reordena. Esa es la diferencia exacta con lo que pasó en 2017.

**Paso 6 — Mira la admisión por ciclos.** Con `capacidad-por-ciclo = 2`, cada 10 segundos se
habilitan 2 turnos. Consulta el turno de la posición 1:

```powershell
curl.exe -s "http://localhost:8090/api/cola/turnos/<turnoId>" -H "X-Usuario-Id: <usuarioId>"
```
→ primero verás `{"estado":"EN_COLA","posicion":1,"esperaEstimadaSegundos":10}` y, en el
siguiente ciclo, `{"estado":"HABILITADO","pase":{"codigo":"...","expiraEn":"..."}}`.

**Paso 7 — Verifica en RabbitMQ** que se publicaron los mensajes `turno.habilitado`.

**Paso 8 — Prueba el pase de un solo uso** (lo más importante de la fase):

```powershell
# Primera vez: debe funcionar
curl.exe -s -X POST "http://localhost:8083/internal/pases/<codigo>/consumir" -H "Content-Type: application/json" -d '{\"usuarioId\":\"<usuarioId>\"}'
# Segunda vez: debe fallar
curl.exe -s -X POST "http://localhost:8083/internal/pases/<codigo>/consumir" -H "Content-Type: application/json" -d '{\"usuarioId\":\"<usuarioId>\"}'
```
→ la primera `200`, la segunda **`409`**. ✅ **Eso es PRB-03 resuelto.**

**Paso 9 — Prueba el pase ajeno:** intenta consumir el pase de otro usuario con tu `usuarioId`.
→ `409`.

### Prueba de no regresión

- [ ] Todo lo de las fases 3 a 6 sigue funcionando
- [ ] Publicar un evento sigue creando el stock en `ms-inventario`

### Criterios de aceptación

- [ ] El sorteo ocurre una sola vez a la hora anunciada y asigna posiciones aleatorias
- [ ] Las posiciones **no cambian** después del sorteo
- [ ] La admisión habilita N turnos cada 10 s y crea un pase por turno
- [ ] Un pase no se puede usar dos veces
- [ ] Un pase no se puede usar con otro usuario
- [ ] Entrar dos veces a la sala devuelve el mismo turno

### Documento a generar

`docs/fases/FASE-07-ms-cola.md`

---

# FASE 8 — `ms-ordenes`: crear la orden con Feign

> Aquí aparece la **comunicación síncrona**: un microservicio llamando a otros tres y esperando
> la respuesta. Completa CU-05.

### Objetivo

Que el comprador, con su pase válido, cree una orden: se consume el pase, se obtiene el precio y
se reserva el stock, **todo en una sola petición**.

### Qué es Feign, en una frase

> Es un **traductor automático de interfaces Java a llamadas HTTP**. Tú escribes una interfaz
> Java que *parece* un método normal, y Feign se encarga de convertirla en una petición HTTP al
> otro microservicio:

```java
@FeignClient(name = "ms-cola", url = "${servicios.cola.url}")
public interface ColaClient {
    @PostMapping("/internal/pases/{codigo}/consumir")
    PaseConsumidoResponse consumirPase(@PathVariable String codigo, @RequestBody ConsumirPaseRequest req);
}
```

Llamas a `colaClient.consumirPase(...)` como si fuera un método local, y Feign hace el `POST` por
ti.

### Qué se construye

1. **Dependencias:** JPA, PostgreSQL, AMQP, `spring-cloud-starter-openfeign` + `mensajeria-comun`.
   > **Conexión:** misma plantilla de la Fase 3, cambiando solo `DB_NAME=db_ordenes`.
   > Ver [DEC-01](docs/decisiones/DEC-01-bases-de-datos.md).
2. **`schema.sql`:** `orden` (con **`UNIQUE(pase_codigo)`**: un pase = una orden) y
   `orden_historial` (auditoría de cada cambio de estado: anterior, nuevo, motivo, fecha).
3. **Tres clientes Feign:**

   | Cliente | Destino | Para qué |
   |---|---|---|
   | `ColaClient` | ms-cola | Consumir el pase |
   | `EventoClient` | ms-eventos | Precio y máximo por cliente |
   | `InventarioClient` | ms-inventario | Reservar stock |

   Todos con **timeout de 2–3 segundos** y una excepción propia por cada error de negocio.

4. **`POST /api/ordenes`** con la cabecera `X-Pase-Compra`, que ejecuta en orden:
   1. Consume el pase en `ms-cola` → si falla, `409 «Pase inválido o vencido»`
   2. Pide precio y máximo a `ms-eventos` → valida RN-05 (máximo por comprador)
   3. Reserva stock en `ms-inventario` → si falla, `409 «Sin stock»`
   4. Crea la orden en estado `RESERVADA` y registra el cambio en `orden_historial`
5. **`GET /api/ordenes/{id}`** y **`GET /api/ordenes/mias`**.
6. **`GET /internal/ordenes/{id}`** (lo usará `ms-reclamos` en la Fase 17).
7. **Activar el `PaseCompraFilter`** del gateway (preparado en la Fase 2).
8. **Propagación del `X-Request-Id`:** un interceptor de Feign que reenvía la cabecera a cada
   llamada, para poder seguir toda la compra en los logs.

### ⚠️ Nota honesta sobre la compensación

Si el paso 3 (reservar stock) falla, **el pase ya fue consumido en el paso 1** y el comprador
pierde su turno. En un sistema real habría que devolver el pase. El diseño de referencia no lo
contempla explícitamente, así que hay dos opciones:

- **(a)** Dejarlo así y documentarlo como limitación conocida.
- **(b) Recomendada:** reservar el stock **antes** de consumir el pase, o agregar un endpoint
  `POST /internal/pases/{codigo}/devolver` en `ms-cola` y llamarlo si falla la reserva.

Es una decisión que conviene tomar en esta fase y **dejar escrita en el documento de la fase**,
porque es justo el tipo de detalle que un profesor pregunta en la sustentación.

### Por qué no rompe nada

`ms-ordenes` solo **lee** de los otros servicios a través de endpoints que ya existen y están
probados. Ninguno de ellos cambia.

### Cómo probarlo tú mismo

**Paso 1 — Sin pase:**

```powershell
curl.exe -s -o NUL -w "%{http_code}" -X POST http://localhost:8090/api/ordenes -H "Content-Type: application/json" -d '{}'
```
→ **`400`** ✅ El gateway lo rechazó antes de llegar a `ms-ordenes`. **Primera barrera de PRB-03.**

**Paso 2 — Con un pase inventado:**

```powershell
curl.exe -s -X POST http://localhost:8090/api/ordenes `
  -H "Content-Type: application/json" -H "X-Usuario-Id: <usuarioId>" `
  -H "X-Pase-Compra: 99999999-9999-9999-9999-999999999999" `
  -d '{\"eventoId\":\"<EVENTO>\",\"zonaId\":\"<zonaVIP>\",\"cantidad\":2}'
```
→ **`409 «Pase inválido»`** ✅ **Segunda barrera**: `ms-ordenes` lo validó por Feign contra
`ms-cola`.

**Paso 3 — Con un pase real** (consigue uno con la Fase 7):

```powershell
curl.exe -s -X POST http://localhost:8090/api/ordenes `
  -H "Content-Type: application/json" -H "X-Usuario-Id: <usuarioId>" `
  -H "X-Pase-Compra: <codigo-del-pase>" `
  -d '{\"eventoId\":\"<EVENTO>\",\"zonaId\":\"<zonaVIP>\",\"cantidad\":2}'
```
→ `201` con la orden en estado `RESERVADA`, el total calculado (`2 × 450.00 = 900.00`) y el
tiempo para pagar.

**Paso 4 — Comprueba el efecto en cadena.** Una sola petición movió **tres bases de datos**:

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_cola       -c "SELECT codigo, estado FROM pase_compra;"
docker exec -it teleticket-postgres psql -U teleticket -d db_inventario -c "SELECT zona_id, disponible FROM stock_zona;"
docker exec -it teleticket-postgres psql -U teleticket -d db_ordenes    -c "SELECT codigo, estado, total FROM orden;"
```
→ el pase está `USADO`, el disponible bajó en 2, y la orden está `RESERVADA`. 🎉

**Paso 5 — Reusar el mismo pase:** repite el Paso 3 con el mismo pase.
→ `409`. **Un pase, una orden.**

**Paso 6 — Probar RN-05:** intenta comprar 10 entradas cuando el máximo del evento es 4.
→ `409 «Máximo 4 entradas por comprador»`.

**Paso 7 — Probar el timeout:** apaga `ms-inventario` y crea una orden.
→ debe responder en **menos de 5 segundos** con un error claro, **no** quedarse colgado. Vuelve a
encenderlo después.

**Paso 8 — Revisa la auditoría:**

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_ordenes -c "SELECT * FROM orden_historial;"
```

### Prueba de no regresión

- [ ] Las fases 3 a 7 siguen funcionando
- [ ] Un comprador que no pasó por la cola **no puede** crear una orden

### Criterios de aceptación

- [ ] Sin pase → `400` desde el gateway
- [ ] Pase inválido, vencido, ajeno o usado → `409`
- [ ] Pase válido → orden `RESERVADA`, pase `USADO` y stock descontado
- [ ] El máximo por comprador se respeta
- [ ] Cada cambio de estado queda en `orden_historial`
- [ ] Con un servicio caído, la respuesta llega en segundos con un error legible

### Documento a generar

`docs/fases/FASE-08-ms-ordenes-feign.md`

---

# FASE 9 — `ms-pagos` y el pago de la orden

> Cubre CU-06. Aquí aparece la **idempotencia**, el concepto que evita los dobles cobros.

### Objetivo

Que el comprador pague su orden reservada y que, pase lo que pase, **nunca se le cobre dos veces**.

### Qué es la idempotencia, en una frase

> Una operación es **idempotente** si ejecutarla una vez o cinco veces produce **el mismo
> resultado**. Apagar un interruptor es idempotente; sumar 1 no lo es.
> Aplicado a pagos: si `ms-ordenes` reintenta el cobro porque no recibió respuesta, `ms-pagos`
> debe darse cuenta de que ya cobró esa orden y **devolver el mismo pago sin volver a cobrar**.

### Qué se construye

1. **En `ms-pagos`:** JPA, PostgreSQL, AMQP + `mensajeria-comun`.
   > **Conexión:** misma plantilla de la Fase 3, cambiando solo `DB_NAME=db_pagos`.
   > Ver [DEC-01](docs/decisiones/DEC-01-bases-de-datos.md).
2. **`schema.sql`:** `pago` (id, orden_id, monto, estado, referencia_pasarela,
   **`idempotency_key UNIQUE`**) y `reembolso` (con `UNIQUE(pago_id)`).
3. **`POST /internal/pagos` idempotente:** antes de cobrar, busca si ya existe un pago con esa
   `idempotency_key` (que es el `ordenId`). Si existe, **devuelve el que ya había**.
4. **Pasarela simulada** según el diseño:

   | Tarjeta termina en | Resultado |
   |---|---|
   | `0000` | **Rechazada** |
   | `9999` | Aprobada, pero **tarda 5 segundos** (para probar timeouts) |
   | cualquier otra | **Aprobada** |

5. **En `ms-ordenes`: `POST /api/ordenes/{id}/pagar`**, que ejecuta:
   1. Valida que la orden esté `RESERVADA`
   2. `PUT /internal/reservas/{id}/en-pago` → protege la reserva mientras se cobra
   3. `POST /internal/pagos` (Feign)
   4. Si **aprobado** → orden `PAGADA` y publica `orden.pagada`
   5. Si **rechazado** → `PUT /internal/reservas/{id}/activar` y orden vuelve a `RESERVADA`,
      para que el comprador **pueda reintentar con otra tarjeta sin perder su reserva**
6. **`PagoClient`** en `ms-ordenes`.
7. **En `ms-inventario`:** consumidor de `orden.pagada` que marca la reserva como `CONFIRMADA`
   (el stock ya no vuelve a estar disponible).

### Por qué no rompe nada

`POST /pagar` es un endpoint nuevo sobre órdenes que ya existen. Crear órdenes no cambia.

### Cómo probarlo tú mismo

**Prueba 1 — Pago rechazado (y que puedes reintentar):**

```powershell
curl.exe -s -X POST "http://localhost:8090/api/ordenes/$ORDEN/pagar" `
  -H "Content-Type: application/json" -H "X-Usuario-Id: <usuarioId>" `
  -d '{\"numeroTarjeta\":\"4111111111110000\",\"titular\":\"DANIEL FUENTES\"}'
```
→ `402` o `409` con *«Pago rechazado»*, y la orden **vuelve a `RESERVADA`**, no se queda colgada.
Verifica que la reserva también volvió a `ACTIVA`.

**Prueba 2 — Pago aprobado:**

```powershell
curl.exe -s -X POST "http://localhost:8090/api/ordenes/$ORDEN/pagar" `
  -H "Content-Type: application/json" -H "X-Usuario-Id: <usuarioId>" `
  -d '{\"numeroTarjeta\":\"4111111111111234\",\"titular\":\"DANIEL FUENTES\"}'
```
→ `200`, orden en estado `PAGADA`.

**Prueba 3 — La idempotencia (la clave de la fase):** ejecuta **exactamente el mismo comando de
la Prueba 2 otra vez**, y después mira la base de datos:

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_pagos -c "SELECT orden_id, monto, estado FROM pago;"
```
→ **debe haber UNA SOLA fila.** 🎉 Al comprador no se le cobró dos veces. Eso es idempotencia.

**Prueba 4 — RabbitMQ:** en la consola, la cola `inventario.orden-pagada` recibió el mensaje y la
reserva pasó a `CONFIRMADA`:

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_inventario -c "SELECT orden_id, estado FROM reserva;"
```

**Prueba 5 — El timeout:** paga con una tarjeta terminada en `9999`. Tarda 5 segundos y el
timeout de Feign es de 3 → debe dar error de timeout **sin dejar la orden en un estado raro**.
Observa qué estado quedó: esto es justo lo que la Fase 11 va a resolver del todo.

### Prueba de no regresión

- [ ] Las fases 3 a 8 siguen funcionando
- [ ] Crear órdenes sigue funcionando igual

### Criterios de aceptación

- [ ] Tarjeta `0000` rechaza y la orden vuelve a `RESERVADA` (se puede reintentar)
- [ ] Tarjeta normal aprueba y la orden pasa a `PAGADA`
- [ ] **Pagar dos veces la misma orden genera un solo pago**
- [ ] `orden.pagada` llega a RabbitMQ y la reserva se confirma

### Documento a generar

`docs/fases/FASE-09-ms-pagos.md`

---

# FASE 10 — `ms-tickets`: emisión asíncrona con firma HMAC

> Cubre CU-07. Aquí se cierra el **camino feliz** completo: desde la cola hasta la entrada en la
> mano.

### Objetivo

Que al pagarse una orden, se emitan automáticamente las entradas **firmadas digitalmente**, sin
que el comprador espere.

### Qué es una firma HMAC, en una frase

> Es un **sello de lacre con una clave secreta**. Se calcula
> `firma = HMAC-SHA256(codigo + eventoId, clave-secreta)`. Cualquiera puede ver el código de la
> entrada, pero **nadie puede calcular una firma válida sin la clave**. Así, una entrada
> falsificada se detecta al instante, **antes incluso de consultar la base de datos**.
> Es la respuesta a PRB-05 (los 7 000 afectados de Daddy Yankee 2022).

### Qué se construye

1. **En `ms-teletickets`:** JPA, PostgreSQL, AMQP, OpenFeign + `mensajeria-comun`.
   > **Conexión:** misma plantilla de la Fase 3, cambiando solo `DB_NAME=db_tickets`.
   > Ver [DEC-01](docs/decisiones/DEC-01-bases-de-datos.md).
2. **`schema.sql`:** `ticket` (id, orden_id, numero, evento_id, zona_id, codigo, firma,
   condiciones_ingreso, dni_nominado, nombre_nominado, estado, usado_en) con
   **`UNIQUE(codigo)`** y **`UNIQUE(orden_id, numero)`**; `validacion`; `mensaje_procesado`.
3. **Consumidor de `orden.pagada`** con **3 reintentos y espera creciente (1 s, 2 s, 4 s)**:
   - Pide las condiciones de ingreso a `ms-eventos` por Feign (`EventoClient`) y **las copia en
     cada entrada** → esto es PRB-06: *lo que se informa en la compra es lo que se valida en la
     puerta*.
   - Genera N tickets con código UUID y firma HMAC.
   - Publica `tickets.emitidos`.
4. **La restricción `UNIQUE(orden_id, numero)` es la idempotencia:** si `orden.pagada` llega dos
   veces, no se emiten entradas duplicadas.
5. **En `ms-ordenes`:** consumidor de `tickets.emitidos` → orden a estado **`EMITIDA`**.
6. **Configuración:** `teleticket.tickets.clave-firma` en el `application.properties`.

### Por qué no rompe nada

Solo se agrega un consumidor a una cola nueva. Si `ms-tickets` está caído, el mensaje **espera en
la cola** y se procesa cuando vuelva. **La compra no se ve afectada** — esa es exactamente la
ventaja de la mensajería asíncrona frente a una llamada directa.

### Cómo probarlo tú mismo

**La prueba completa de la fase** — haz el recorrido entero del comprador:

1. Crear y publicar un evento (fases 3 y 5)
2. Entrar a la sala, esperar el sorteo y obtener el pase (fase 7)
3. Crear la orden con el pase (fase 8)
4. Pagar (fase 9)
5. **No hagas nada más.** Espera 2 segundos y consulta:

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_tickets -c "SELECT numero, codigo, substring(firma,1,16) AS firma, estado FROM ticket;"
docker exec -it teleticket-postgres psql -U teleticket -d db_ordenes -c "SELECT codigo, estado FROM orden;"
```

→ **2 tickets en estado `EMITIDO`** y la orden en **`EMITIDA`**. 🎉
**Nadie los pidió.** Se emitieron solos porque un mensaje dijo «la orden se pagó».

**Prueba de la prueba de fuego (la más instructiva de todo el plan):**

1. **Apaga `ms-tickets`** (cierra su ventana).
2. Haz una compra completa y págala. Verás que **el pago funciona perfectamente** y la orden
   queda en `PAGADA`.
3. Mira RabbitMQ: el mensaje está **esperando** en la cola `tickets.orden-pagada`.
4. **Vuelve a encender `ms-tickets`.**
5. En 2 segundos, las entradas se emiten y la orden pasa a `EMITIDA`.

🎉 **Un servicio caído no tumbó la compra.** Eso es exactamente lo que no pasaba en 2017, y es el
argumento central del proyecto. **Graba esta prueba en video para la sustentación.**

**Prueba de idempotencia:** vuelve a publicar `orden.pagada` a mano desde la consola de RabbitMQ.
→ **no** se crean entradas duplicadas.

**Prueba de las condiciones (PRB-06):**

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_tickets -c "SELECT condiciones_ingreso FROM ticket LIMIT 1;"
```
→ debe mostrar el **mismo texto** que pusiste al crear el evento en la Fase 3.

### Prueba de no regresión

- [ ] Las fases 3 a 9 siguen funcionando
- [ ] El pago sigue siendo idempotente

### Criterios de aceptación

- [ ] Pagar una orden emite N entradas automáticamente
- [ ] Cada entrada tiene código único y firma HMAC
- [ ] La orden pasa sola a `EMITIDA`
- [ ] Con `ms-tickets` apagado, la compra funciona y las entradas se emiten al reencenderlo
- [ ] Un `orden.pagada` repetido no duplica entradas
- [ ] Las condiciones de ingreso del evento están copiadas en cada entrada

### Documento a generar

`docs/fases/FASE-10-ms-tickets-emision.md`

---

# FASE 11 — La saga: reembolso automático

> **La joya del proyecto.** Es la respuesta directa a PRB-04 («la web cobra pero no emite»,
> 2017) y al OBJ 3. Implementa RN-08.

### Objetivo

Garantizar que **un pago aprobado nunca quede sin entrada**: si la emisión falla tras 3 intentos,
el sistema **reembolsa solo**, devuelve el stock y avisa al comprador.

### Qué es una saga, en una frase

> En un solo sistema con una sola base de datos, si algo falla haces `ROLLBACK` y listo. Con 8
> bases de datos distintas eso es imposible. Una **saga** es la alternativa: una secuencia de
> pasos donde **cada paso tiene su acción de compensación**. Si el paso 4 falla, se ejecutan las
> compensaciones de los pasos 3, 2 y 1 para dejar todo consistente.

### La cadena completa

```text
ms-tickets falla 3 veces al emitir
        ↓  el mensaje cae en la DLQ  tickets.orden-pagada.dlq
ms-tickets publica            → emision.fallida
ms-ordenes lo consume         → publica reembolso.solicitado
ms-pagos lo consume           → devuelve el dinero y publica pago.reembolsado
ms-ordenes lo consume         → orden a REEMBOLSADA y publica orden.reembolsada
        ↓
ms-inventario → devuelve el stock a la zona
ms-tickets    → anula las entradas que hubieran llegado a emitirse
```

### Qué se construye

1. **DLQ `tickets.orden-pagada.dlq`** configurada en `ms-tickets`, con su DLX.
2. **Listener de la DLQ** que publica `emision.fallida`.
3. **En `ms-ordenes`:** consumidores de `emision.fallida` y `pago.reembolsado`, y publicación de
   `reembolso.solicitado` y `orden.reembolsada`.
4. **En `ms-pagos`:** consumidor de `reembolso.solicitado` → crea el reembolso y publica
   `pago.reembolsado`.
5. **En `ms-inventario`:** consumidor de `orden.reembolsada` → devuelve el stock.
6. **En `ms-tickets`:** consumidor de `orden.reembolsada` → anula las entradas.
7. **Validación de transiciones de estado:** no se puede pasar a `EMITIDA` una orden que ya está
   `REEMBOLSADA`.
8. **🔧 Interruptor de fallo para la demo:** una propiedad
   `teleticket.tickets.simular-fallo-emision=true` que hace que la emisión falle a propósito.
   **Sin esto no hay forma práctica de demostrar la saga ante el profesor.** Debe quedar
   desactivada por defecto.

### Por qué no rompe nada

El camino feliz de la Fase 10 **no se toca**. Toda la lógica nueva solo se activa cuando la
emisión falla, cosa que en condiciones normales nunca ocurre.

### Cómo probarlo tú mismo

**La demostración estrella del proyecto:**

**Paso 1 — Activa el interruptor de fallo** en `ms-teletickets/src/main/resources/application.properties`:

```properties
teleticket.tickets.simular-fallo-emision=true
```
y reinicia `ms-tickets`.

**Paso 2 — Haz una compra completa y págala** (fases 7 a 9).

**Paso 3 — Observa la consola de RabbitMQ durante 10 segundos.** Verás pasar, en orden, los
mensajes por sus colas: `tickets.orden-pagada` → 3 reintentos → `tickets.orden-pagada.dlq` →
`emision.fallida` → `reembolso.solicitado` → `pago.reembolsado` → `orden.reembolsada`.

**Paso 4 — Mira el resultado final:**

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_ordenes    -c "SELECT codigo, estado FROM orden;"
docker exec -it teleticket-postgres psql -U teleticket -d db_pagos      -c "SELECT * FROM reembolso;"
docker exec -it teleticket-postgres psql -U teleticket -d db_inventario -c "SELECT zona_id, disponible FROM stock_zona;"
```

→ La orden está **`REEMBOLSADA`**, existe un reembolso por el monto exacto, y **el stock volvió a
su valor original**. 🎉

**Paso 5 — La prueba decisiva:** revisa la auditoría completa de la orden:

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_ordenes -c "SELECT estado_anterior, estado_nuevo, motivo, fecha FROM orden_historial ORDER BY fecha;"
```

→ Verás la historia completa: `RESERVADA → PENDIENTE_PAGO → PAGADA → REEMBOLSADA`, con el motivo
de cada paso. **Nunca hubo un estado intermedio sin resolver.**

> **Esta es la diapositiva más importante de tu sustentación.** En 2017 el usuario quedaba con el
> cobro y sin entrada. Aquí, el mismo fallo termina en un reembolso automático en menos de 10
> segundos, sin que nadie intervenga.

**Paso 6 — Apaga el interruptor** y verifica que el camino feliz de la Fase 10 sigue funcionando.

### Prueba de no regresión

- [ ] Con el interruptor apagado, todo lo de las fases 3 a 10 funciona igual
- [ ] El camino feliz sigue terminando en `EMITIDA`

### Criterios de aceptación

- [ ] Tras 3 intentos fallidos, el mensaje llega a la DLQ (visible en RabbitMQ)
- [ ] La cadena completa de 6 mensajes se ejecuta sola
- [ ] La orden termina en `REEMBOLSADA`, nunca en un estado intermedio
- [ ] El stock se devuelve
- [ ] Todo queda registrado en `orden_historial`
- [ ] **Ninguna orden pagada queda sin entrada ni reembolso** ← meta del OBJ 3

### Documento a generar

`docs/fases/FASE-11-saga-reembolso-automatico.md`

---

# FASE 12 — Vencimiento de reservas (TTL + DLX)

> Implementa RN-06. Aquí se usa un truco muy elegante de RabbitMQ: **mensajes programados sin
> programador**.

### Objetivo

Que una reserva no pagada se libere **sola** a los 10 minutos y el stock vuelva a estar
disponible para otros compradores.

### Cómo funciona el truco

> RabbitMQ no tiene «mensajes programados». Pero sí tiene colas con **tiempo de vida (TTL)**.
> Entonces: se mete el mensaje en una cola **que nadie consume**, con un TTL de 10 minutos. A los
> 10 minutos el mensaje «muere» y RabbitMQ lo reenvía automáticamente a otra cola, **que sí tiene
> consumidor**. Resultado: un mensaje que llega 10 minutos tarde, sin escribir ni un `Thread.sleep`.

### Qué se construye

1. **Cola de espera `inventario.reserva-espera`** con `x-message-ttl = 600000` (10 min),
   `x-dead-letter-exchange = teleticket.dlx` y **sin consumidor**.
2. **Al crear una reserva**, `ms-inventario` publica allí el id de la reserva.
3. **Consumidor de `inventario.reserva-vencida`** que revisa el estado actual:

   | Estado de la reserva | Acción |
   |---|---|
   | `ACTIVA` | Libera el stock y publica `reserva.liberada` |
   | `EN_PAGO` | El cobro está en curso → **vuelve a publicar** con una espera corta de 1 minuto |
   | `CONFIRMADA` o `LIBERADA` | No hace nada |

4. **En `ms-ordenes`:** consumidor de `reserva.liberada` → orden a `EXPIRADA` y publica
   `orden.expirada`.
5. **TTL configurable** (`teleticket.inventario.ttl-reserva-ms`) para poder probar con 60
   segundos en vez de 10 minutos.

### Por qué no rompe nada

Las reservas que sí se pagan pasan a `CONFIRMADA` antes de que venza el TTL, y el consumidor las
ignora. El camino feliz no cambia en absoluto.

### Cómo probarlo tú mismo

**Paso 1 — Baja el TTL a 60 segundos** en `ms-inventario`:

```properties
teleticket.inventario.ttl-reserva-ms=60000
```

**Paso 2 — Crea una orden y NO la pagues** (fases 7 y 8). Anota el disponible de la zona.

**Paso 3 — Mira la consola de RabbitMQ.** La cola `inventario.reserva-espera` tiene 1 mensaje y
**nadie lo consume**. Es un mensaje esperando su hora.

**Paso 4 — Espera 60 segundos y refresca.** El mensaje **desapareció de la cola de espera** y
apareció (y fue consumido) en `inventario.reserva-vencida`. 🎉

**Paso 5 — Verifica el resultado:**

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_inventario -c "SELECT orden_id, estado FROM reserva;"
docker exec -it teleticket-postgres psql -U teleticket -d db_inventario -c "SELECT zona_id, disponible FROM stock_zona;"
docker exec -it teleticket-postgres psql -U teleticket -d db_ordenes    -c "SELECT codigo, estado FROM orden;"
```
→ reserva `LIBERADA`, **stock recuperado** y orden `EXPIRADA`. Nadie hizo nada: el tiempo pasó.

**Paso 6 — El caso `EN_PAGO`:** crea una orden, ponla a pagar con una tarjeta terminada en `9999`
(la lenta) y deja que el TTL venza durante el cobro.
→ la reserva **no** se libera; el mensaje se reprograma. **El comprador no pierde sus entradas a
mitad del pago.**

**Paso 7 — El camino feliz:** crea una orden, **págala de inmediato** y espera 60 segundos.
→ la reserva sigue `CONFIRMADA` y la orden `EMITIDA`. El vencimiento la ignoró correctamente.

**Paso 8 — Devuelve el TTL a 600000** (10 minutos).

### Prueba de no regresión

- [ ] Las compras pagadas a tiempo no se ven afectadas
- [ ] La saga de la Fase 11 sigue funcionando

### Criterios de aceptación

- [ ] Una reserva no pagada se libera sola al vencer el TTL
- [ ] El stock vuelve a estar disponible
- [ ] La orden pasa a `EXPIRADA`
- [ ] Una reserva `EN_PAGO` **no** se libera: se reprograma
- [ ] Una reserva `CONFIRMADA` se ignora

### Documento a generar

`docs/fases/FASE-12-vencimiento-reservas-ttl-dlx.md`

---

# FASE 13 — `ms-notificaciones`: los correos

> Cubre CU-10 y la medida correctiva de Indecopi sobre **información veraz**. Implementa RN-09.

### Objetivo

Que el comprador reciba un correo en **cada cambio de estado importante**, y que puedas verlos
todos en MailHog.

### Qué se construye

1. **Dependencias:** JPA, PostgreSQL, AMQP, OpenFeign, `spring-boot-starter-mail` +
   `mensajeria-comun`.
   > **Conexión:** misma plantilla de la Fase 3, cambiando solo `DB_NAME=db_notificaciones`.
   > Ver [DEC-01](docs/decisiones/DEC-01-bases-de-datos.md).
2. **`schema.sql`:** `notificacion` con **`UNIQUE(mensaje_origen_id, tipo)`** — así, si un
   mensaje llega dos veces, **no se envía el correo dos veces**.
3. **Una sola cola `notificaciones.todas`** con binding a cuatro routing keys:

   | Mensaje | Correo que se envía |
   |---|---|
   | `turno.habilitado` | *«Llegó tu turno. Tienes hasta las HH:MM para comprar»* |
   | `tickets.emitidos` | *«Aquí están tus entradas»* + **las condiciones de ingreso** |
   | `orden.expirada` | *«Tu reserva venció sin pago»* |
   | `orden.reembolsada` | *«No pudimos emitir tus entradas. Te devolvimos S/ X»* |

4. **`UsuarioClient` (Feign)** hacia `ms-usuarios` para obtener nombre y correo.
5. **Plantillas de correo** (texto plano o Thymeleaf). La de `tickets.emitidos` **debe incluir
   las condiciones de ingreso copiadas del evento** → PRB-06: lo que dice el correo es lo mismo
   que se valida en la puerta.
6. **Configuración SMTP** apuntando a MailHog (`localhost:1025`).

### Por qué no rompe nada

Es un consumidor puro: no publica nada y nadie depende de él. Si `ms-notificaciones` está caído,
los mensajes esperan en su cola y **ninguna compra se ve afectada**. Esa es la razón de que sea
asíncrono.

### Cómo probarlo tú mismo

**Paso 1 — Abre MailHog** en http://localhost:8025. Bandeja vacía.

**Paso 2 — Haz un recorrido completo:** sala de espera → sorteo → pase → orden → pago → emisión.

**Paso 3 — Refresca MailHog.** 🎉 Deberías ver **dos correos**: *«Llegó tu turno»* y *«Aquí están
tus entradas»*.

**Paso 4 — Abre el correo de las entradas** y verifica que incluye **exactamente el mismo texto
de condiciones de ingreso** que escribiste al crear el evento en la Fase 3. Esa coincidencia es
la prueba de que PRB-06 está resuelto: en 2022 Teleticket fue multada porque el correo decía una
cosa y en la puerta se exigía otra.

**Paso 5 — Provoca los otros dos correos:**
- Activa el interruptor de fallo de la Fase 11 → correo de **reembolso**
- Deja vencer una reserva (Fase 12) → correo de **expiración**

**Paso 6 — Prueba la no duplicación:** republica `tickets.emitidos` a mano desde RabbitMQ.
→ en MailHog **sigue habiendo un solo correo**.

**Paso 7 — Prueba la resiliencia:** apaga `ms-notificaciones`, haz una compra completa (funciona
sin problema), y vuelve a encenderlo.
→ los correos atrasados se envían de golpe.

### Prueba de no regresión

- [ ] Todo el flujo de compra de las fases 7 a 12 funciona igual
- [ ] Con `ms-notificaciones` apagado, la compra no se ve afectada

### Criterios de aceptación

- [ ] Los 4 tipos de correo llegan a MailHog
- [ ] El correo de entradas incluye las condiciones de ingreso del evento
- [ ] Un mensaje duplicado no genera un correo duplicado
- [ ] Cada notificación queda registrada en `db_notificaciones`

### Documento a generar

`docs/fases/FASE-13-ms-notificaciones.md`

---

# FASE 14 — Nominación, descarga y validación de entradas

> **Al cerrar esta fase se cumple el OBJ 2:** los 12 casos de uso «Must» quedan implementados.
> Cubre CU-08, CU-09 y CU-11.

### Objetivo

Que el comprador nomine sus entradas con el DNI del asistente, las vea con su QR, y que el
controlador de acceso las valide en la puerta — **una sola vez**.

### Qué se construye

1. **`PUT /api/tickets/{id}/nominar`**: asocia DNI y nombre del asistente. **Una sola vez**
   (RN-10): el estado pasa de `EMITIDO` a `NOMINADO` y ya no se puede volver a cambiar.
2. **`GET /api/tickets/mios`**: lista las entradas del usuario de la cabecera `X-Usuario-Id`,
   con su código, estado, condiciones y el QR en base64.
3. **Generación del QR** con la librería ZXing. El contenido del QR es `codigo|firma`.
4. **`POST /api/tickets/validar`** (rol controlador), que ejecuta en orden:
   1. **Verifica la firma HMAC** → si no coincide: `RECHAZADA (FIRMA_INVALIDA)` **sin tocar la
      base de datos**
   2. La consulta atómica de un solo uso:
      ```sql
      UPDATE ticket SET estado = 'USADO', usado_en = now()
      WHERE codigo = :codigo AND evento_id = :eventoId AND estado = 'NOMINADO'
      ```
   3. 1 fila → `ACEPTADA` con el DNI del asistente · 0 filas → `RECHAZADA` con el motivo
5. **Registro de TODOS los intentos** en la tabla `validacion`, **incluidos los rechazados**. Eso
   da trazabilidad para detectar intentos de fraude.
6. **Publicación de `ticket.validado`** (todavía sin consumidores; queda listo para los reportes
   con Kafka, sección 11 del diseño).

### Por qué no rompe nada

Son endpoints nuevos sobre entradas que ya existen. La emisión de la Fase 10 no cambia.

### Cómo probarlo tú mismo

**Paso 1 — Ver tus entradas:**

```powershell
curl.exe -s http://localhost:8090/api/tickets/mios -H "X-Usuario-Id: <usuarioId>"
```

**Paso 2 — Nominar:**

```powershell
curl.exe -s -X PUT "http://localhost:8090/api/tickets/<ticketId>/nominar" `
  -H "Content-Type: application/json" -H "X-Usuario-Id: <usuarioId>" `
  -d '{\"dni\":\"70123456\",\"nombres\":\"Daniel Fuentes\"}'
```
→ `200`, estado `NOMINADO`.

**Paso 3 — Probar RN-10:** intenta nominar la misma entrada otra vez con otro DNI.
→ `409 «La entrada ya fue nominada»`.

**Paso 4 — Validar en la puerta (primera vez):**

```powershell
curl.exe -s -X POST http://localhost:8090/api/tickets/validar `
  -H "Content-Type: application/json" -H "X-Usuario-Rol: CONTROLADOR" `
  -d '{\"codigo\":\"<codigo>\",\"firma\":\"<firma>\",\"eventoId\":\"<EVENTO>\",\"puerta\":\"Puerta 3\"}'
```
→ `{"resultado":"ACEPTADA","dni":"70123456","nombres":"Daniel Fuentes"}` ✅

**Paso 5 — Validar la misma entrada otra vez** (el clásico intento de entrar dos veces con la
misma entrada):
→ `{"resultado":"RECHAZADA","motivo":"YA_USADA"}` ✅ **PRB-05 resuelto.**

**Paso 6 — La entrada falsificada** (la prueba más vistosa):

```powershell
curl.exe -s -X POST http://localhost:8090/api/tickets/validar `
  -H "Content-Type: application/json" -H "X-Usuario-Rol: CONTROLADOR" `
  -d '{\"codigo\":\"<codigo-real>\",\"firma\":\"firma-inventada-por-un-estafador\",\"eventoId\":\"<EVENTO>\",\"puerta\":\"Puerta 3\"}'
```
→ `{"resultado":"RECHAZADA","motivo":"FIRMA_INVALIDA"}` ✅
**Y lo mejor: ni siquiera se consultó la base de datos.** El sello de lacre no coincidía.

**Paso 7 — Entrada no nominada:** intenta validar una entrada que no nominaste.
→ `RECHAZADA (NO_NOMINADA)`.

**Paso 8 — La trazabilidad:**

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_tickets -c "SELECT codigo_leido, resultado, puerta, fecha FROM validacion ORDER BY fecha;"
```
→ **todos** los intentos, incluidos los 3 rechazados. Un auditor de Indecopi podría revisarlo.

**Paso 9 — El QR:** copia el QR en base64 de `GET /api/tickets/mios`, pégalo en cualquier
visualizador de base64 online y escanéalo con tu celular. Debe contener `codigo|firma`.

### Prueba de no regresión

- [ ] El recorrido completo de las fases 7 a 13 funciona de punta a punta
- [ ] Los correos siguen llegando a MailHog

### Criterios de aceptación

- [ ] Nominar funciona una sola vez por entrada
- [ ] `GET /api/tickets/mios` devuelve las entradas con QR
- [ ] Validar una entrada legítima y nominada → `ACEPTADA`
- [ ] Validar la misma dos veces → la segunda `RECHAZADA (YA_USADA)`
- [ ] Firma inválida → `RECHAZADA (FIRMA_INVALIDA)`
- [ ] Todos los intentos quedan registrados
- [ ] ✅ **Los 12 CU «Must» están implementados → OBJ 2 cumplido**

### Documento a generar

`docs/fases/FASE-14-nominacion-y-validacion.md`
Y además: `docs/OBJ2-cobertura-casos-de-uso.md`, una tabla que demuestre que CU-01 a CU-09, CU-11,
CU-14 y CU-15 están cubiertos, con el endpoint concreto de cada uno. **Es la evidencia del OBJ 2
para la evaluación.**

---

# FASE 15 — Dockerización completa

> Entregable N.º 6 del informe. Hasta ahora arrancabas 9 ventanas a mano; desde aquí, **un
> comando levanta todo**.

### Objetivo

Que todo el sistema — gateway, 8 microservicios, PostgreSQL, RabbitMQ y MailHog — se levante con
`docker compose up`.

### Qué se construye

1. **Un `Dockerfile` por servicio**, con build en dos etapas (compilar con Maven, ejecutar con un
   JRE ligero) para que las imágenes sean pequeñas.
2. **`infra/docker-compose.yml` ampliado** con los 9 servicios.
3. **Pasar las variables de entorno** en el compose. Gracias a la convención fijada en la Fase 3
   ([DEC-01](docs/decisiones/DEC-01-bases-de-datos.md)), **no hay que tocar ningún
   `application.properties`**: basta con darle a cada servicio su `DB_HOST` y `DB_PORT`.

   ```yaml
   ms-eventos:
     environment:
       DB_HOST: postgres          # el nombre del servicio en este mismo compose
       DB_PORT: 5432              # el puerto INTERNO, no el 5433 de Windows
       SPRING_RABBITMQ_HOST: rabbitmq
       SERVICIOS_COLA_URL: http://ms-cola:8083
   ```

   > **Y si algún día se quiere un contenedor de PostgreSQL por servicio**, el cambio es poner
   > `DB_HOST: postgres-eventos` en vez de `postgres`. Nada más: ni código, ni properties.
4. **Perfiles de Spring:** `application-local.properties` (lo que tienes hoy) y
   `application-docker.properties`. **Así no pierdes la posibilidad de ejecutar desde Eclipse**,
   que es clave para depurar.
5. **`depends_on` con `condition: service_healthy`** para que los microservicios no arranquen
   antes que PostgreSQL y RabbitMQ.
6. **Healthchecks** usando el `/actuator/health` de la Fase 0.

### Por qué no rompe nada

El perfil `local` se mantiene intacto. Puedes seguir ejecutando los servicios desde Eclipse
exactamente igual que hasta ahora.

### Cómo probarlo tú mismo

```powershell
cd "F:\...\eclipse-workspace\infra"
docker compose build
docker compose up -d
docker compose ps
```
→ 12 contenedores en estado `healthy`.

**La prueba:** repite el **recorrido completo del comprador** (fases 7 a 14) contra
`http://localhost:8090`, sin tener nada abierto en Eclipse.
→ debe funcionar **exactamente igual**. 🎉

**Prueba de resiliencia:**

```powershell
docker compose stop ms-teletickets
# haz una compra y págala -> funciona
docker compose start ms-teletickets
# las entradas se emiten solas
```

**Prueba de que no perdiste el modo local:** apaga los contenedores de los microservicios (deja
PostgreSQL, RabbitMQ y MailHog) y arranca `ms-eventos` desde Eclipse con el perfil `local`.
→ debe funcionar.

### Prueba de no regresión

- [ ] El recorrido completo funciona en Docker
- [ ] El recorrido completo sigue funcionando desde Eclipse con el perfil `local`

### Criterios de aceptación

- [ ] `docker compose up -d` levanta los 12 contenedores
- [ ] El recorrido completo funciona en Docker
- [ ] Ningún valor de configuración está escrito a mano en el código Java
- [ ] Se puede seguir desarrollando desde Eclipse

### Documento a generar

`docs/fases/FASE-15-dockerizacion.md`

---

# FASE 16 — Colección Postman y pruebas de carga

> Entregable N.º 7 del informe. **Aquí se mide el OBJ 3**, que es el objetivo más difícil de
> demostrar y el que más peso tiene en la evaluación.

### Objetivo

Dejar evidencia medible de que con **1 000 compradores concurrentes**: la disponibilidad es
**≥ 99 %**, hay **0 compras sin pase válido** y el **100 % de las órdenes pagadas** termina en
`EMITIDA` o `REEMBOLSADA`.

### Qué se construye

1. **`pruebas/teleticket.postman_collection.json`** con todas las rutas públicas organizadas en
   carpetas por microservicio, variables de entorno (`{{gateway}}`, `{{eventoId}}`, `{{pase}}`…)
   y scripts que guardan automáticamente los ids de una petición a la siguiente, para poder
   ejecutar **el recorrido completo con un clic**.
2. **`pruebas/carga/apertura-venta.js`** con **k6** (más simple que JMeter y se escribe en
   JavaScript), que simula el escenario real:
   - 1 000 usuarios virtuales entran a la sala de espera **a la vez**
   - esperan el sorteo
   - consultan su turno hasta recibir el pase
   - crean la orden y pagan
3. **`pruebas/carga/sin-pase.js`**: 200 usuarios intentando comprar **sin pase** o con pases
   inventados. **Debe dar 0 éxitos.**
4. **Consultas SQL de verificación del OBJ 3**, en `pruebas/verificar-obj3.sql`:

   ```sql
   -- Meta: 0 filas
   SELECT COUNT(*) AS ordenes_sin_pase_valido FROM orden WHERE pase_codigo IS NULL;

   -- Meta: 0 filas
   SELECT COUNT(*) AS ordenes_pagadas_sin_resolver
   FROM orden WHERE estado = 'PAGADA' AND creada_en < now() - interval '5 minutes';

   -- Meta: 100 %
   SELECT estado, COUNT(*) FROM orden GROUP BY estado;
   ```

5. **`docs/OBJ3-resultados-pruebas-carga.md`** con las capturas, los números obtenidos y la
   comparación contra las metas. **Este documento es la evidencia del OBJ 3.**
6. **Spring Actuator + métricas** ya activas desde la Fase 0.

### Por qué no rompe nada

Solo se agregan archivos de prueba. No se toca ni una línea del código de los servicios.

> **Consejo práctico:** ejecuta la carga contra la **base de datos limpia** (`docker compose down
> -v && up -d`) para que los números sean limpios, y sube las capturas al repositorio.

### Cómo probarlo tú mismo

**Postman:** importa la colección, ejecuta el *runner* de la carpeta «Recorrido completo».
→ todas las peticiones en verde.

**Carga:**

```powershell
cd "F:\...\eclipse-workspace\pruebas\carga"
k6 run apertura-venta.js
```

**Lo que debes mirar en el resultado de k6:**

| Métrica | Meta |
|---|---|
| `http_req_failed` | **< 1 %** → disponibilidad ≥ 99 % |
| `http_req_duration p(95)` | razonable (< 2 s) |
| `checks` | 100 % de los pases consumidos una sola vez |

**Compras sin pase:**

```powershell
k6 run sin-pase.js
```
→ **0 órdenes creadas.** Si creó aunque sea una, hay un agujero de seguridad que hay que cerrar.

**Verificación final:**

```powershell
docker exec -i teleticket-postgres psql -U teleticket -d db_ordenes < verificar-obj3.sql
```

### Prueba de no regresión

- [ ] Después de la carga, el sistema sigue respondiendo con normalidad
- [ ] No quedó ninguna orden en un estado intermedio

### Criterios de aceptación

- [ ] Colección Postman completa y ejecutable
- [ ] Disponibilidad **≥ 99 %** con 1 000 usuarios
- [ ] **0** compras sin pase válido
- [ ] **100 %** de las órdenes pagadas en `EMITIDA` o `REEMBOLSADA`
- [ ] ✅ **OBJ 3 documentado con evidencia**

### Documento a generar

`docs/fases/FASE-16-pruebas-y-carga.md` + `docs/OBJ3-resultados-pruebas-carga.md`

---

# FASE 17 — `ms-reclamos` *(opcional)*

> Prioridad *Could* en MoSCoW. Cubre CU-12, CU-13 y PRB-07. **Hazla solo si tienes tiempo
> después de la Fase 16.**

### Objetivo

Libro de reclamaciones con número correlativo y **alerta antes de vencer el plazo legal de 15
días hábiles** (Ley 31435).

### Qué se construye

1. **Proyecto nuevo `ms-reclamos`** (puerto 8089, base `db_reclamos`) — es el único que hay que
   crear desde cero con Spring Initializr.
   > **Conexión:** misma plantilla de la Fase 3, cambiando solo `DB_NAME=db_reclamos`. La base ya
   > existe desde la Fase 1. Ver [DEC-01](docs/decisiones/DEC-01-bases-de-datos.md).
2. **Ruta `/api/reclamos/**`** en el gateway.
3. **`schema.sql`:** `reclamo` (correlativo, orden_id, usuario_id, tipo, detalle, estado,
   creado_en, **fecha_limite**, respondido_en).
4. **`OrdenClient` (Feign)** para adjuntar los datos de la orden al reclamo.
5. **Cálculo del plazo en días hábiles** (15 días hábiles, excluyendo sábados y domingos).
6. **Tarea programada diaria** que busca reclamos próximos a vencer y publica
   `reclamo.por-vencer`.
7. **Endpoints:** `POST /api/reclamos`, `GET /api/reclamos/mios`, `GET /api/reclamos` (rol
   atención), `PUT /api/reclamos/{id}/responder`.

### Cómo probarlo tú mismo

- Crear un reclamo desde una orden → verificar que el correlativo es secuencial y que la
  `fecha_limite` son 15 **días hábiles** después (no 15 días naturales).
- Insertar a mano un reclamo con `fecha_limite` a 2 días → al ejecutar la tarea programada debe
  publicar `reclamo.por-vencer` y generar un correo.

### Documento a generar

`docs/fases/FASE-17-ms-reclamos.md`

---

# FASE 18 — Front-end

> **Deliberadamente al final.** Para cuando llegues aquí, el backend estará completo, probado y
> documentado, y ya sabrás exactamente qué endpoints existen y qué devuelven.

### Por qué conviene que vaya al final

1. El backend ya está **validado con Postman**, así que el front no tiene que depurar dos capas a
   la vez.
2. Los contratos de la API **ya no van a cambiar**.
3. Si el tiempo se acaba, **el proyecto ya cumple los tres objetivos** sin el front: el informe
   habla de «cliente web» y de una app móvil como *etapa siguiente del curso*.

### Qué habrá que decidir cuando llegues

| Decisión | Opciones | Comentario |
|---|---|---|
| Tecnología | React · Angular · Vue · Thymeleaf | React es lo más usado; **Thymeleaf** es la opción más rápida si quieres quedarte solo en Java |
| Alcance | Solo comprador · comprador + operador · + controlador | Lo mínimo para una demo es el recorrido del comprador |

### Pantallas mínimas para la demostración

| # | Pantalla | Endpoints que consume |
|---|---|---|
| 1 | Lista de eventos | `GET /api/eventos` |
| 2 | Detalle del evento con zonas y precios | `GET /api/eventos/{id}` |
| 3 | **Sala de espera con reloj** hacia la hora de venta | `POST /api/cola/.../ingresar` |
| 4 | **Mi turno**: posición, espera estimada y pase | `GET /api/cola/turnos/{id}` (polling cada 3 s) |
| 5 | Elegir zona y cantidad | `POST /api/ordenes` (con `X-Pase-Compra`) |
| 6 | Pago con cuenta regresiva de 10 minutos | `POST /api/ordenes/{id}/pagar` |
| 7 | Mis e-tickets con QR | `GET /api/tickets/mios` |
| 8 | Nominar entrada | `PUT /api/tickets/{id}/nominar` |
| 9 | *(Controlador)* Escanear y validar | `POST /api/tickets/validar` |

> **La pantalla 4 es la más vistosa de la sustentación:** ver la posición en la cola bajando en
> tiempo real hasta que aparece el pase es la demostración visual de que el problema de 2017 está
> resuelto.

### Lo único que hay que tocar del backend

El CORS del gateway, que ya quedó preparado en la Fase 2. **Nada más.**

### Documento a generar

`docs/fases/FASE-18-frontend.md`

---

# Cómo trabajar cada fase

### Rutina recomendada

1. **Antes de empezar:** crea una rama → `git checkout -b fase-07-ms-cola`
2. **Durante:** haz commits pequeños y frecuentes
3. **Al terminar:** ejecuta las pruebas manuales de la sección «Cómo probarlo tú mismo»
4. **Luego:** ejecuta la «Prueba de no regresión»
5. **Después:** escribe `docs/fases/FASE-XX-nombre.md`
6. **Finalmente:** `git checkout main && git merge fase-07-ms-cola`

Así, si una fase se complica, `main` **siempre tiene la última versión que funcionaba**.

### Si una fase se rompe

| Síntoma | Dónde mirar primero |
|---|---|
| Un servicio no arranca | ¿El puerto está ocupado? ¿Docker está arriba? Mira el log completo |
| Un mensaje no llega | Consola de RabbitMQ → pestaña **Queues**. ¿La cola existe? ¿Tiene consumidor? ¿Hay mensajes acumulados? |
| Un mensaje se procesa pero no hace nada | ¿El `eventoId` ya está en `mensaje_procesado`? Bórralo y reintenta |
| Feign da error | ¿El servicio destino está encendido? ¿La URL del `application.properties` es correcta? |
| Un dato no se guardó | `psql` directo a la tabla. ¿Se hizo commit? ¿Falló una restricción? |
| Todo está raro | `docker compose down -v && docker compose up -d` deja la infraestructura como nueva |

---

# Plantilla del documento de fase

Copia esta plantilla en `docs/fases/FASE-XX-nombre.md` al cerrar cada fase.

```markdown
# Fase XX — <Nombre>

**Estado:** ✅ Completada · **Fecha:** DD/MM/2026
**Cubre:** CU-XX, RN-XX · **Resuelve:** PRB-XX

## 1. En una frase
<Qué hace ahora el sistema que antes no podía hacer.>

## 2. Qué se creó

### Archivos nuevos
| Archivo | Para qué sirve |
|---|---|

### Archivos modificados
| Archivo | Qué se le agregó | Por qué era seguro |
|---|---|---|

### Tablas de base de datos
| Base | Tabla | Para qué |
|---|---|---|

### Endpoints
| Método | Ruta | Público/Interno | Qué hace |
|---|---|---|---|

### Mensajes de RabbitMQ
| Routing key | Publica | Consume | Qué provoca |
|---|---|---|---|

## 3. Conceptos nuevos explicados
<Cada término técnico que apareció por primera vez, en lenguaje simple. Ej.: «Idempotencia es…»>

## 4. Qué puedo hacer ahora
- …

## 5. Cómo lo compruebo
<Los comandos exactos, listos para copiar, y lo que debería verse.>

## 6. Qué sigue sin funcionar
<Lo que aún no está, para no confundirse. Ej.: «Aún no llegan correos: eso es la Fase 13.»>

## 7. Decisiones tomadas y problemas encontrados
<Lo que hubo que decidir sobre la marcha y por qué. Esta sección es oro puro
para la sustentación: es donde demuestras criterio propio.>

## 8. Relación con el informe
| Requisito del .docx | Cómo lo cumple esta fase |
|---|---|
```

---

# Resumen de cobertura: del plan al informe

| Requisito del informe | Fase(s) | Evidencia |
|---|---|---|
| **OBJ 1** — Diseño con los 7 problemas asignados | ya cumplido | Los dos `.md` de diseño |
| **OBJ 2** — 8 microservicios, 12 CU «Must» | **0 a 14** | `docs/OBJ2-cobertura-casos-de-uso.md` |
| **OBJ 3** — ≥99 %, 0 sin pase, 100 % resueltas | **16** | `docs/OBJ3-resultados-pruebas-carga.md` |
| Producto 3 — API Gateway | 2 | `ms-api-gateway` |
| Producto 4 — 8 microservicios con su BD | 3 a 14 | 8 proyectos + 8 bases |
| Producto 5 — Librería de mensajería común | 5 | `libs/mensajeria-comun` |
| Producto 6 — Infraestructura local | 1 y 15 | `infra/docker-compose.yml` |
| Producto 7 — Pruebas | 16 | `pruebas/` |
| PRB-01 Colapso en picos | 7, 15, 16 | Cola virtual + carga de 1 000 usuarios |
| PRB-02 Cola injusta | 5, 7 | RN-01 bloqueada + sorteo único |
| PRB-03 Saltarse la cola | 2, 7, 8 | Filtro del gateway + pase de un solo uso |
| PRB-04 Cobro sin entrada | 9, 10, 11 | Saga con reembolso automático |
| PRB-05 Entradas falsas | 10, 14 | Firma HMAC + validación de un solo uso |
| PRB-06 Información contradictoria | 10, 13, 14 | Condiciones copiadas al ticket y al correo |
| PRB-07 Reclamos manuales | 17 *(opcional)* | `ms-reclamos` con alerta de plazo |
| Medida Indecopi 1 — Información veraz | 5, 13, 14 | Hora bloqueada, correos, condiciones |
| Medida Indecopi 2 — Corregir errores | 10, 11, 12 | Reintentos, DLQ, compensación, `orden_historial` |
| Medida Indecopi 3 — Seguridad de la compra | 2, 7, 10 | Pase obligatorio, endpoints internos, HMAC |

---

## Fuentes consultadas para la decisión de versiones

- [Spring Cloud 2025.1.1 (aka Oakwood) Has Been Released](https://spring.io/blog/2026/01/29/spring-cloud-2025-1-1-aka-oakwood-has-been-released/)
- [Spring Cloud Train Reference Documentation](https://docs.spring.io/spring-cloud-release/reference/index.html)
- [Tabla de compatibilidad Spring Boot / Spring Cloud](https://stevenpg.com/references/spring-version-compatibility/)
