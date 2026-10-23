# Fase 1 — Infraestructura local con Docker

**Estado:** ✅ Completada · **Fecha:** 22/10/2026
**Cubre:** ningún caso de uso (es la «sala de máquinas» del sistema)
**Producto del informe:** **N.º 6 — Infraestructura local (PostgreSQL, RabbitMQ y MailHog con docker-compose)**

---

## 1. En una frase

> Con **un solo comando** se levantan las tres piezas que los microservicios necesitan por
> detrás: la base de datos con sus 9 bases ya creadas, el mensajero RabbitMQ con su consola web, y
> un servidor de correo falso que captura los correos en lugar de enviarlos.

**En esta fase no se escribió ni una línea de Java.** Los 9 microservicios siguen exactamente
igual que al terminar la Fase 0.

---

## 2. Qué se creó

### Archivos nuevos

| Archivo | Para qué sirve |
|---|---|
| [`infra/docker-compose.yml`](../../infra/docker-compose.yml) | Define los 3 contenedores, sus puertos, volúmenes y healthchecks |
| [`infra/init-db/01-crear-bases.sql`](../../infra/init-db/01-crear-bases.sql) | Crea las 9 bases de datos automáticamente al arrancar |
| [`infra/README.md`](../../infra/README.md) | Manual de uso: comandos, credenciales, cómo mirar dentro y qué hacer si algo falla |
| [`verificar-infra.ps1`](../../verificar-infra.ps1) | Comprueba de una pasada el daemon, los 3 contenedores, las 9 bases y las 2 consolas web |
| `docs/fases/FASE-01-infraestructura.md` | Este documento |

### Archivos modificados

Ninguno. **Esta fase solo agrega archivos nuevos**, por eso era imposible que rompiera algo.

### Contenedores que se levantan

| Contenedor | Imagen | Puertos (tu PC → contenedor) | Para qué |
|---|---|---|---|
| `teleticket-postgres` | `postgres:16-alpine` | **5433** → 5432 | Las 9 bases de datos |
| `teleticket-rabbitmq` | `rabbitmq:3-management-alpine` | 5672 → 5672 (mensajes)<br>15672 → 15672 (consola web) | Mensajería asíncrona |
| `teleticket-mailhog` | `mailhog/mailhog:v1.0.1` | 1025 → 1025 (SMTP)<br>8025 → 8025 (bandeja web) | Capturar los correos |

### Volúmenes creados

| Volumen | Qué guarda |
|---|---|
| `teleticket_postgres-datos` | Los datos de las 9 bases. Sobreviven a `docker compose down` |
| `teleticket_rabbitmq-datos` | La configuración y los mensajes persistentes de RabbitMQ |

MailHog **no** tiene volumen a propósito: guarda los correos en memoria, así que reiniciarlo vacía
la bandeja. Eso es lo que quieres para las pruebas.

### Bases de datos creadas

| Base | Microservicio | Sus tablas llegan en |
|---|---|---|
| `db_usuarios` | ms-usuarios | Fase 4 |
| `db_eventos` | ms-eventos | Fase 3 |
| `db_cola` | ms-cola | Fase 7 |
| `db_inventario` | ms-inventario | Fase 6 |
| `db_ordenes` | ms-ordenes | Fase 8 |
| `db_pagos` | ms-pagos | Fase 9 |
| `db_tickets` | ms-tickets | Fase 10 |
| `db_notificaciones` | ms-notificaciones | Fase 13 |
| `db_reclamos` | ms-reclamos *(opcional)* | Fase 17 |

Las 9 están **completamente vacías**: ni una tabla. Eso es lo correcto en esta fase.

> **¿Por qué una sola instancia de PostgreSQL y no nueve contenedores?** Porque el patrón
> *database per service* trata sobre quién es dueño de las tablas, no sobre cuántos servidores hay,
> y porque la sección 6.3.3 del informe pide explícitamente «una instancia con una base de datos
> distinta por servicio». El análisis completo, con los costos medidos de la alternativa y la
> respuesta lista para la sustentación, está en
> [DEC-01 — Bases de datos](../decisiones/DEC-01-bases-de-datos.md).

### Credenciales

| Servicio | Usuario | Contraseña |
|---|---|---|
| PostgreSQL | `teleticket` | `teleticket` |
| RabbitMQ | `guest` | `guest` |
| MailHog | *(no pide)* | — |

---

## 3. Conceptos nuevos explicados

**Contenedor**
Imagina una caja que trae **el programa y todo lo que necesita para funcionar** (su sistema
operativo mínimo, sus librerías, su configuración). No se instala nada en tu Windows: la caja se
descarga, se ejecuta y, cuando la borras, no deja rastro. Por eso puedes tener PostgreSQL 16 de
Docker **al mismo tiempo** que tu PostgreSQL 16 instalado, sin que se estorben.

**Imagen vs. contenedor**
La **imagen** es la receta (`postgres:16-alpine`); el **contenedor** es el plato ya servido
(`teleticket-postgres`). De una imagen puedes crear muchos contenedores. `alpine` significa que usa
una versión mínima de Linux, así que la descarga es mucho más pequeña.

**`docker-compose.yml`**
Un archivo donde describes **qué contenedores quieres y cómo se conectan**, en lugar de escribir
tres comandos `docker run` larguísimos con veinte parámetros cada uno. Después, `docker compose
up -d` levanta todo de una vez.

**Mapeo de puertos (`"5433:5432"`)**
Se lee **izquierda = tu PC, derecha = dentro del contenedor**. Significa: «cuando algo llame al
puerto 5433 de mi Windows, mándalo al 5432 de dentro del contenedor». Así PostgreSQL sigue
creyendo que vive en el 5432 (su puerto de siempre) pero desde afuera lo buscas en el 5433.

**Volumen**
Los contenedores son desechables: si borras el contenedor, se borra todo lo que tenía dentro. Un
**volumen** es una carpeta que vive **afuera** del contenedor y sobrevive. Por eso los datos de
las bases no se pierden al hacer `docker compose down`.

> La diferencia es importante:
> - `docker compose down` → borra los contenedores, **conserva** los volúmenes (los datos siguen)
> - `docker compose down -v` → borra los contenedores **y los volúmenes** (empiezas de cero)

**Healthcheck**
Un comando que Docker ejecuta cada cierto tiempo para preguntarle al contenedor «¿ya estás
listo?». PostgreSQL tarda unos segundos en aceptar conexiones después de arrancar, y RabbitMQ
bastante más. Hoy esto es solo informativo, pero en la **Fase 15** es lo que va a permitir que los
microservicios **esperen** a que la base esté lista antes de intentar conectarse.

**`/docker-entrypoint-initdb.d`**
Una carpeta especial de la imagen de PostgreSQL: todo archivo `.sql` que encuentre ahí lo ejecuta
automáticamente, **pero solo la primera vez**, cuando el volumen de datos está vacío. Es la forma
estándar de dejar una base de datos preparada.

**SMTP y por qué MailHog**
SMTP es el protocolo con el que se envían correos. MailHog finge ser un servidor SMTP: acepta los
correos como si fuera real, pero **no los manda a nadie** — los guarda y los muestra en una página
web. Así, en la Fase 13 vas a poder comprobar que el sistema notifica en cada cambio de estado sin
mandarle correos de prueba a nadie.

---

## 4. Qué puedo hacer ahora

- ✅ Levantar toda la infraestructura con `docker compose up -d`
- ✅ Conectarme a las 9 bases de datos (con `psql`, DBeaver o pgAdmin, en el puerto **5433**)
- ✅ Abrir la consola de RabbitMQ en http://localhost:15672 y navegar sus pestañas
- ✅ Abrir la bandeja de MailHog en http://localhost:8025
- ✅ Apagar todo conservando los datos (`down`) o borrándolos (`down -v`)
- ✅ Comprobar que todo está bien con `.\verificar-infra.ps1`
- ✅ Tener la base de datos lista para que `ms-eventos` guarde su primer evento en la **Fase 3**

---

## 5. Cómo lo compruebo

### La forma rápida

```powershell
cd "F:\COMPUTACION_E_INFORMATICA_CIBERTEC\6 - SEXTO CICLO\MODULO 2\DESARROLLO DE APLICACIONES WEB II\Proyectos\eclipse-workspace\infra"
docker compose up -d
cd ..
.\verificar-infra.ps1
```

**Lo que debes ver:**

```text
  Teleticket - infraestructura local
  ===============================================================

  [1] Docker
      OK  daemon activo (version 29.8.0)

  [2] Contenedores
      OK  teleticket-postgres      running / healthy
      OK  teleticket-rabbitmq      running / healthy
      OK  teleticket-mailhog       running / sin healthcheck

  [3] Bases de datos en PostgreSQL (puerto 5433)
      OK  db_usuarios
      OK  db_eventos
      ... (las 9)

  [4] Consolas web
      OK  RabbitMQ   http://localhost:15672
      OK  MailHog    http://localhost:8025

  ===============================================================
  Todo correcto. Fase 1 verificada.
```

> ⚠️ **RabbitMQ tarda ~40 segundos** en pasar a `healthy`. Si el script dice `arrancando
> todavia`, espera un poco y repítelo.

### Las comprobaciones visuales (las más convincentes)

**1. Las 9 bases existen:**

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d postgres -c "\l"
```

**2. RabbitMQ:** abre http://localhost:15672 (`guest` / `guest`). Vas a ver el panel con la
pestaña **Queues vacía**. Eso es lo correcto: las colas las crea el código Java en la Fase 5.
Acuérdate de esta pantalla vacía, porque en la Fase 5 vas a ver aparecer mensajes acá.

**3. MailHog:** abre http://localhost:8025. Bandeja de entrada vacía, como un cliente de correo
recién abierto. Los correos empiezan a llegar en la Fase 13.

### Resultado real de esta implementación

| Verificación | Resultado |
|---|---|
| `docker compose config` (sintaxis del YAML) | ✅ Válida, 3 servicios declarados |
| `docker compose up -d` | ✅ 3 contenedores, 2 volúmenes y 1 red creados |
| Healthchecks de postgres y rabbitmq | ✅ Ambos `healthy` |
| Las 9 bases de datos | ✅ Creadas solas por el script de `init-db/`, propietario `teleticket`, codificación UTF8 |
| Conexión desde Windows con `psql -p 5433` | ✅ Responde `PostgreSQL 16.15 on x86_64-pc-linux-musl` (confirma que es el de Docker/Alpine, no el local) |
| El PostgreSQL local del 5432 **no** tiene las bases del proyecto | ✅ Confirmado: rechaza al usuario `teleticket`. Son dos servidores independientes |
| RabbitMQ: colas | ✅ 0 colas (correcto en esta fase), solo los exchanges `amq.*` por defecto |
| MailHog: bandeja | ✅ `{"total":0,"count":0,"items":[]}` |
| Consola web de RabbitMQ (15672) | ✅ HTTP 200 |
| Bandeja web de MailHog (8025) | ✅ HTTP 200 |
| **Persistencia:** crear una tabla → `down` → `up` | ✅ **El dato sobrevivió**, y las 9 bases siguieron ahí |
| **No regresión:** los 9 servicios de la Fase 0 | ✅ Arrancan y responden `/ping` igual que antes |

La prueba de persistencia se hizo creando una tabla `prueba_persistencia` con una fila, apagando
con `docker compose down`, volviendo a levantar y comprobando que la fila seguía ahí. Después se
borró la tabla, así que **las 9 bases quedaron vacías** para la Fase 3.

---

## 6. Qué sigue sin funcionar

| No funciona | Llega en |
|---|---|
| Las bases están **vacías**: ni una tabla | Fase 3 en adelante |
| RabbitMQ **no tiene el exchange `teleticket.exchange` ni colas** | Fase 5 (los declara el código Java) |
| MailHog no recibe ningún correo | Fase 13 |
| Los microservicios **todavía no se conectan** a nada de esto: siguen sin dependencias de base de datos | Fase 3 |
| El gateway sigue sin enrutar | Fase 2 |
| Los microservicios no corren como contenedores | Fase 15 |

**Importante:** aunque la infraestructura ya está levantada, los microservicios **no la están
usando**. Son dos mundos que todavía no se tocan. El primer puente se construye en la Fase 3,
cuando `ms-eventos` se conecte a `db_eventos`.

---

## 7. Decisiones tomadas y problemas encontrados

### Problema 1 — Docker Desktop no estaba corriendo

El CLI de Docker estaba instalado (versión 29.8.0) pero **el daemon no estaba activo**, así que
cualquier comando fallaba con:

```text
failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine
```

Hubo que abrir Docker Desktop y esperar. **Tardó ~164 segundos** en responder. Vale la pena
saberlo: es normal que tarde un par de minutos, sobre todo la primera vez del día. Por eso
`verificar-infra.ps1` detecta este caso concreto y da la instrucción exacta en vez de un error
crudo.

### Decisión 1 — PostgreSQL en el puerto 5433, confirmada con evidencia

El plan ya proponía el 5433 por precaución. **Se verificó que era necesario:** el puerto 5432
estaba ocupado por el proceso 5492, que es el PostgreSQL 16.13 instalado en Windows.

Después del montaje se comprobó que los dos servidores conviven sin problema:

| | Puerto 5432 (local) | Puerto 5433 (Docker) |
|---|---|---|
| Versión | PostgreSQL 16.13 en Windows | PostgreSQL 16.15 en Alpine Linux |
| Usuario `teleticket` | **No existe** (rechaza la conexión) | Existe |
| Bases `db_*` | 0 | **9** |

**Consecuencia para las fases siguientes:** la URL de conexión cambia según dónde corra el
servicio.

| Dónde corre el microservicio | URL de conexión |
|---|---|
| Desde Eclipse (fases 3–14) | `jdbc:postgresql://localhost:5433/db_eventos` |
| Como contenedor (Fase 15) | `jdbc:postgresql://postgres:5432/db_eventos` |

En el segundo caso se usa `postgres` como nombre de host —el nombre del servicio en el
`docker-compose.yml`— y el puerto **interno** 5432, porque la comunicación ocurre dentro de la red
de Docker y no pasa por Windows. Esto está documentado en `infra/README.md` para no tropezar en la
Fase 3.

### Decisión 2 — MailHog, aunque ya no se mantiene

MailHog no recibe mantenimiento desde 2020. La alternativa moderna es **Mailpit**, que es
compatible y usa los mismos puertos.

**Se eligió MailHog** porque es el que nombra explícitamente el informe del proyecto
(«PostgreSQL, RabbitMQ y MailHog con docker-compose», Producto N.º 6), y lo que se entrega debe
coincidir con lo que dice el documento. Funciona perfectamente para este uso.

Se fijó la versión concreta `mailhog/mailhog:v1.0.1` en lugar de `latest`, para que el proyecto no
se rompa si algún día cambia la imagen. La alternativa Mailpit queda anotada en
`infra/README.md` por si hiciera falta.

### Decisión 3 — `name: teleticket` en el compose

Por defecto, Docker Compose nombra el proyecto según la carpeta, que acá sería `infra`. Se fijó
`name: teleticket` para que los volúmenes y la red se llamen `teleticket_postgres-datos` y
`teleticket_default`, y no `infra_postgres-datos`. Es más claro al listar volúmenes con
`docker volume ls`.

### Decisión 4 — MailHog sin volumen, a propósito

PostgreSQL y RabbitMQ tienen volumen porque sus datos deben sobrevivir. MailHog **no**, y se le
puso `MH_STORAGE: memory` explícitamente: así, reiniciarlo deja la bandeja limpia, que es
justamente lo que se quiere al empezar una prueba nueva.

### Decisión 5 — Un script de verificación, que el plan no pedía

El plan pedía las comprobaciones a mano. Se agregó `verificar-infra.ps1` por la misma razón que
en la Fase 0: a partir de la Fase 3 vas a tener que comprobar «¿está todo arriba?» muchísimas
veces, y un comando es más rápido y menos propenso a error que cinco comprobaciones manuales.

El script distingue tres situaciones que a simple vista parecen la misma: el **daemon apagado**,
un contenedor **todavía arrancando** (`starting`, típico de RabbitMQ en los primeros 40 s) y un
contenedor **realmente caído** (`unhealthy`). Cada caso da una instrucción distinta.

### Problema 2 (el importante) — Al arrancar Docker, el gateway dejó de poder arrancar

Este fue el hallazgo más valioso de la fase, y apareció **solo porque se hizo la prueba de no
regresión** de la Fase 0.

Al volver a levantar los 9 microservicios, **8 arrancaron bien y el gateway falló**:

```text
***************************
APPLICATION FAILED TO START
***************************
Description:
Web server failed to start. Port 8080 was already in use.
```

**La causa:** en esta máquina hay unos 30 contenedores de otros proyectos del curso. Al arrancar
Docker Desktop, dos quedaron corriendo, y uno de ellos —`efsrt-iv-proyecto-app-1`— mapea el
puerto **8080**, que es justo el que el diseño reserva para el API Gateway.

Lo confuso es que en la Fase 0 el gateway sí arrancó en el 8080, **porque Docker Desktop estaba
apagado**. El conflicto apareció recién al encenderlo.

| Puerto | Quién lo tiene | ¿Choca con Teleticket? |
|---|---|---|
| **8080** | contenedor `efsrt-iv-proyecto-app-1` | ❌ **Sí**: es el del gateway |
| 8081–8088 | libres | No |
| 5433, 5672, 15672, 1025, 8025 | los contenedores de Teleticket | No |

**La decisión tomada: mover el gateway al 8090** y no apagar el contenedor del otro proyecto. Se
eligió esto para no tener que acordarse de apagar el proyecto EFSRT cada vez que se trabaja en
Teleticket.

**Es una desviación consciente del diseño** (sección 12.2 dice 8080) y queda registrada en:

- el comentario de `ms-api-gateway/src/main/resources/application.properties`
- la tabla de decisiones técnicas de `PLAN-DE-IMPLEMENTACION.md`
- este documento

Todos los comandos de prueba del plan (31 referencias) se actualizaron de `localhost:8080` a
`localhost:8090`.

> **Para la sustentación:** si el profesor pregunta por qué el gateway no está en el 8080, la
> respuesta es que el puerto estaba ocupado en el entorno de desarrollo y que **el diseño no
> cambia**: el gateway sigue siendo el único punto de entrada, solo escucha en otro número. En la
> Fase 15, al correr todo en contenedores, se puede volver al 8080 sin problema, porque ahí el
> puerto lo decide el mapeo del `docker-compose.yml`.

### Problema 3 — El script de parada se había vuelto peligroso

Consecuencia directa del problema anterior. `detener-todo.ps1` recorría los puertos **8080–8088**
y mataba lo que encontrara escuchando. Con el 8080 ahora ocupado por Docker, ejecutarlo habría
intentado matar **`com.docker.backend.exe`** y habría tumbado Docker Desktop completo, con los
contenedores de todos los proyectos.

Se corrigieron dos cosas:

1. El rango pasó a **8081–8088 y 8090** (el 8080 ya no se toca).
2. Se agregó un **seguro**: el script solo detiene procesos llamados `java` o `javaw`. Si en un
   puerto hay otra cosa, lo avisa y no la toca.

```powershell
if ($PERMITIDOS -notcontains $proc.ProcessName) {
    Write-Host "  [!] puerto $puerto  lo ocupa '$($proc.ProcessName)', que NO es un microservicio."
    continue
}
```

> Esto es un buen recordatorio de por qué conviene que un script de «matar procesos» sea lo más
> específico posible. Un rango de puertos no identifica a un proceso; el nombre sí.

### Problema 4 — Mi propio comando de espera dio un falso positivo

Para esperar a que los 9 servicios arrancaran se usó un bucle que contaba sockets escuchando en
el rango 8080–8088 y paraba al llegar a 18 (9 servicios × IPv4 + IPv6). Reportó **«los 9
servicios escuchando»** cuando en realidad solo había 8: los sockets del contenedor ajeno en el
8080 completaron la cuenta.

**La lección:** contar no es verificar. `verificar-servicios.ps1` hace lo correcto, porque le
pregunta a cada puerto **quién es** y compara el nombre con el esperado; por eso detectó el
problema de inmediato con un claro `8/9`.

### Nota — Los healthchecks ya están puestos, aunque todavía no hagan falta

Hoy los healthchecks son solo informativos. Se pusieron ya porque en la **Fase 15** los
microservicios van a declarar `depends_on: condition: service_healthy`, y entonces son lo que
evita que un microservicio arranque antes de que PostgreSQL acepte conexiones y falle con un error
de conexión confuso.

---

## 8. Relación con el informe

| Requisito del `.docx` | Cómo lo cumple esta fase |
|---|---|
| **Producto N.º 6** — «Infraestructura local: PostgreSQL, RabbitMQ y MailHog con docker-compose» | ✅ Los 3 contenedores funcionando con un solo comando |
| **Sección 6.3.3, Despliegue** — «PostgreSQL (una instancia con una base de datos distinta por servicio), RabbitMQ y MailHog, que captura los correos sin enviarlos» | ✅ Una instancia con 9 bases; RabbitMQ con consola; MailHog capturando |
| **Sección 7 del diseño** — patrón *database per service* | ✅ 9 bases separadas, una por microservicio. Ninguna comparte tablas |
| **Sección 12.2 del diseño** — Puertos e infraestructura local | ✅ RabbitMQ 5672/15672 y MailHog 1025/8025 exactamente como dice la tabla. **PostgreSQL en 5433 en vez de 5432**, por el conflicto documentado en la sección 7 |
| **Sección 11 del diseño** — «Docker: un docker-compose.yml con PostgreSQL, RabbitMQ y MailHog» | ✅ Hecho. Los `Dockerfile` de los servicios quedan para la Fase 15 |
| **Variable ecológica (SEPTE)** — escalado selectivo en lugar de sobredimensionar | Los contenedores permiten levantar solo lo necesario y apagarlo al terminar |

---

## Siguiente paso

**[Fase 2 — API Gateway con enrutamiento real](../../PLAN-DE-IMPLEMENTACION.md)**: que el gateway
del puerto 8090 reciba las peticiones y las reenvíe al microservicio correcto, agregando la
cabecera de rastreo `X-Request-Id`. Y lo más importante de esa fase: comprobar que
`ms-inventario`, `ms-pagos` y `ms-notificaciones` **no son alcanzables** desde fuera, que es parte
de la solución a PRB-03 (saltarse la cola).
