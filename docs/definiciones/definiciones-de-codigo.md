# Definiciones de código

Este documento explica, en lenguaje sencillo, las líneas de código y de configuración del proyecto
que no son evidentes a primera vista.

**Para qué sirve:** en lugar de llenar los archivos del proyecto de comentarios largos, cada línea
difícil lleva un comentario de **una sola línea** que apunta aquí, y la explicación completa vive
en este documento. Así el código queda limpio y la explicación queda a mano.

**Cómo usarlo:** busca el término en el índice. Cada entrada dice **en qué archivo está** la línea,
qué hace, qué alternativas había y por qué se eligió esa.

---

## Índice

| Término                                              | Dónde aparece               | Qué es                                    |
| ----------------------------------------------------- | ---------------------------- | ------------------------------------------ |
| [`restart: unless-stopped`](#restart-unless-stopped) | `infra/docker-compose.yml` | Qué hace Docker si un contenedor se apaga |
| [`* text=auto eol=lf`](#-text-auto-eollf) | `.gitattributes` | Cómo guarda Git los finales de línea |
| [`volumes:`](#volumes) | `infra/docker-compose.yml` | Dónde viven los datos y cómo sobreviven |
| [`healthcheck`](#healthcheck) | `infra/docker-compose.yml` | Cómo sabe Docker si un contenedor ya está listo |

---

## `restart: unless-stopped`

**Archivo:** [`infra/docker-compose.yml`](../../infra/docker-compose.yml)
**Aparece 3 veces:** en los servicios `postgres`, `rabbitmq` y `mailhog`
**Tecnología:** Docker Compose

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: teleticket-postgres
    restart: unless-stopped      # <-- esta linea
```

### Qué problema resuelve

Sin esta línea, un contenedor que se cae —o que se apaga porque reiniciaste Windows— **se queda
apagado**, y hay que levantarlo a mano con `docker compose up -d`. Es fácil olvidarse, ponerse a
programar y encontrarse con errores de conexión a la base de datos sin entender por qué.

### Qué significa `unless-stopped`

Literalmente, «a menos que esté detenido». Es decir:

> Docker vuelve a arrancar el contenedor **siempre**, **excepto si fuiste tú quien lo detuvo a
> propósito**.

| Qué pasó                          | Resultado                  |
| ----------------------------------- | -------------------------- |
| El contenedor se cayó por un error | Docker lo levanta solo     |
| Reiniciaste la computadora          | Docker lo levanta solo     |
| Reiniciaste Docker Desktop          | Docker lo levanta solo     |
| Ejecutaste `docker compose stop`  | **Se queda apagado** |
| Ejecutaste `docker compose down`  | **Se queda apagado** |

Esos dos últimos casos son la clave: Docker **respeta tu decisión** de haberlo apagado. No te lo
vuelve a encender por detrás.

### Las cuatro opciones posibles

| Valor                        | Qué hace                                                                       |
| ---------------------------- | ------------------------------------------------------------------------------- |
| `no`                       | *(el valor por defecto)* Nunca lo reinicia. Si se cae, se queda caído        |
| `on-failure`               | Lo reinicia solo si terminó con un código de error                            |
| `always`                   | Lo reinicia siempre, **incluso si tú lo detuviste**                      |
| **`unless-stopped`** | Lo reinicia siempre, **salvo que tú lo hayas detenido** ← la que usamos |

### Por qué se eligió `unless-stopped` y no `always`

Son casi iguales, y la diferencia está en un solo escenario:

1. Ejecutas `docker compose stop` porque quieres la máquina libre.
2. Más tarde reinicias Docker Desktop (o la computadora).

- Con **`always`**, el contenedor **vuelve a encenderse**, ignorando que lo habías apagado.
- Con **`unless-stopped`**, **sigue apagado**, que es lo que tú querías.

En una máquina de desarrollo, donde apagas y prendes cosas constantemente, `unless-stopped` es el
que se comporta como uno espera.

### Por qué conviene en este proyecto

La base de datos, RabbitMQ y MailHog son **infraestructura**: no son el proyecto, son lo que el
proyecto necesita por detrás. Con `unless-stopped` ya están esperando cuando abres la
computadora, y no tienes que acordarte de levantarlas antes de ponerte a programar.

### Cómo comprobarlo

Ver la política que tiene un contenedor:

```powershell
docker inspect --format "{{.HostConfig.RestartPolicy.Name}}" teleticket-postgres
```

→ `unless-stopped`

Probar que funciona: mata el contenedor a mano y mira cómo vuelve solo.

```powershell
docker kill teleticket-postgres
docker ps --filter name=teleticket-postgres --format "{{.Names}} {{.Status}}"
```

→ en unos segundos aparece de nuevo como `Up`, porque Docker lo reinició. Si en cambio haces
`docker compose stop postgres`, **no** vuelve: ahí sí lo detuviste tú.

### Un detalle que confunde

`restart: unless-stopped` **no** es lo mismo que «arrancar al iniciar Windows». Para que Docker
levante los contenedores, **Docker Desktop tiene que estar corriendo**. Si Docker Desktop no
arranca solo con Windows (en esta máquina `AutoStart` está en `false`), hay que abrirlo primero;
recién entonces Docker restaura los contenedores con esta política.

---

## `* text=auto eol=lf`

**Archivo:** [`.gitattributes`](../../.gitattributes)
**Tecnología:** Git

```gitattributes
* text=auto eol=lf
```

### Qué problema resuelve

Windows termina las líneas con **dos** caracteres invisibles (`\r\n`, llamado CRLF) y
Linux/Mac con **uno** (`\n`, llamado LF). Los dos son válidos, pero si en un mismo proyecto se
mezclan, pasa esto:

> Cambias **una** palabra en un archivo, pero tu editor lo guarda con el otro tipo de final de
> línea. Git entonces reporta que cambiaron **todas** las líneas del archivo, y el cambio real
> queda escondido entre cientos de líneas falsas.

Eso ocurrió de verdad en este proyecto: al corregir unas palabras en 5 archivos, un script los
guardó con CRLF y el `diff` pasó de 16 líneas reales a 914 líneas aparentes.

### Qué hace la línea

| Parte | Qué significa |
|---|---|
| `*` | Aplica a todos los archivos |
| `text=auto` | Git detecta solos cuáles son de texto y cuáles binarios |
| `eol=lf` | Los de texto se guardan **siempre con LF**, sin importar el sistema operativo |

### Por qué LF y no CRLF

Porque los archivos originales que generó Spring Initializr (`MsEventosApplication.java`,
`HELP.md`, …) ya venían en LF. Elegir LF deja todo el proyecto parejo, y además es lo que esperan
Git, Docker y las herramientas de Linux donde los contenedores van a correr.

### Cómo comprobarlo

```powershell
git ls-files --eol
```

La columna `i/` dice cómo está guardado en el repositorio. Debe decir `lf` en todos los archivos
de texto.

---

## `volumes:`

**Archivo:** [`infra/docker-compose.yml`](../../infra/docker-compose.yml)
**Tecnología:** Docker Compose

```yaml
services:
  postgres:
    volumes:
      - postgres-datos:/var/lib/postgresql/data   # <-- el volumen va aqui

volumes:            # <-- y se declara aqui, al final del archivo
  postgres-datos:
  rabbitmq-datos:
```

### Qué problema resuelve

Un contenedor es **desechable**: cuando lo borras, desaparece todo lo que tenía dentro. Si la base
de datos viviera dentro del contenedor, un `docker compose down` borraría todos tus eventos,
usuarios y órdenes.

Un **volumen** es una carpeta que Docker guarda **afuera** del contenedor y que sobrevive. El
contenedor va y viene; el volumen se queda.

| Comando | Qué pasa con los datos |
|---|---|
| `docker compose down` | Borra los contenedores, **conserva** los volúmenes → los datos siguen |
| `docker compose down -v` | Borra los contenedores **y los volúmenes** → **se pierde todo** |

### Volumen con nombre vs. volumen anónimo

Si no le pones nombre, Docker le asigna un hash y queda imposible de identificar. Así se ve la
diferencia en esta misma máquina, que tiene volúmenes de muchos proyectos:

```text
06f8e215423c05416766f7da565019072e56b9da0c53741ed238422c64a6c34c   <- anonimo
17afd795cd1ded805dad22d870accb3f7c7c84156c20f904521486835a31dadd   <- anonimo
...  (17 asi)
teleticket_postgres-datos    <- se reconoce de inmediato
teleticket_rabbitmq-datos
```

El prefijo `teleticket_` no es automático: viene de la línea `name: teleticket` que está al
principio del `docker-compose.yml`. Sin ella, el prefijo sería el nombre de la carpeta (`infra_`).

### La confusión más común: 1 volumen = 1 contenedor, NO 1 base de datos

Este proyecto tiene **9 bases de datos** pero solo **2 volúmenes**:

```text
teleticket_postgres-datos   (142 MB)
└── /var/lib/postgresql/data
    ├── db_usuarios          las 9 bases viven
    ├── db_eventos           DENTRO del mismo
    ├── db_cola              volumen, porque
    ├── db_inventario        comparten el mismo
    ├── db_ordenes           proceso de PostgreSQL
    ├── db_pagos
    ├── db_tickets
    ├── db_notificaciones
    └── db_reclamos

teleticket_rabbitmq-datos   (142,5 kB)
└── /var/lib/rabbitmq

(MailHog no tiene volumen: guarda los correos en memoria, a proposito)
```

**Un volumen pertenece a un contenedor, no a una base de datos.** Un solo proceso de PostgreSQL
tiene un solo directorio de datos, y ese directorio es el volumen. Para que cada base tuviera su
propio volumen haría falta un contenedor de PostgreSQL por base; el porqué no se hizo está en
[DEC-01](../decisiones/DEC-01-bases-de-datos.md).

### Dentro del volumen, las bases no se reconocen por su nombre

PostgreSQL no guarda las bases en carpetas con su nombre, sino numeradas por **OID**:

| Base | Carpeta real | Base | Carpeta real |
|---|---|---|---|
| `db_usuarios` | `base/16384/` | `db_pagos` | `base/16389/` |
| `db_eventos` | `base/16401/` | `db_tickets` | `base/16390/` |
| `db_cola` | `base/16386/` | `db_notificaciones` | `base/16391/` |
| `db_inventario` | `base/16387/` | `db_reclamos` | `base/16392/` |
| `db_ordenes` | `base/16388/` | | |

> ⚠️ **Estos números son solo un ejemplo, no una referencia fija.** El OID lo asigna PostgreSQL al
> crear la base, y **cambia si la vuelves a crear**. De hecho, `db_eventos` empezó en `16385` y
> pasó a `16401` al probar el comando de reseteo de más abajo. La única fuente fiable es la
> consulta.

Para ver esta correspondencia en cualquier momento:

```powershell
docker exec teleticket-postgres psql -U teleticket -d postgres -c "SELECT datname, oid FROM pg_database WHERE datname LIKE 'db_%' ORDER BY datname;"
```

**La consecuencia práctica:** no puedes respaldar ni resetear una sola base copiando o borrando una
carpeta. Se hace con `pg_dump` y `psql`, como se muestra más abajo.

### Comandos para mirar los volúmenes

```powershell
docker volume ls --filter name=teleticket          # los 2 del proyecto
docker volume inspect teleticket_postgres-datos    # driver y punto de montaje
docker system df -v                                # cuanto ocupa cada uno
```

> El punto de montaje que informa `inspect`
> (`/var/lib/docker/volumes/teleticket_postgres-datos/_data`) está **dentro de la máquina virtual
> de WSL**, no en una carpeta de Windows. No lo busques en el explorador: para mirar adentro se usa
> `docker exec`.

### Respaldar UNA sola base de datos

```powershell
docker exec teleticket-postgres pg_dump -U teleticket -d db_eventos > respaldo-eventos.sql
```

Genera un archivo de texto con las tablas y los datos. Útil antes de un cambio grande, o para
entregar el proyecto con datos de ejemplo cargados.

### Restaurar UNA sola base de datos

```powershell
Get-Content respaldo-eventos.sql | docker exec -i teleticket-postgres psql -U teleticket -d db_eventos
```

### Resetear UNA sola base, sin tocar las otras 8

Esto es lo más útil del día a día a partir de la Fase 3: cuando estés depurando un microservicio y
quieras dejar su base limpia sin perder el resto del trabajo.

```powershell
docker exec teleticket-postgres psql -U teleticket -d postgres -c "DROP DATABASE db_eventos;"
docker exec teleticket-postgres psql -U teleticket -d postgres -c "CREATE DATABASE db_eventos OWNER teleticket;"
```

> ⚠️ **No confundir con `docker compose down -v`**, que borra el volumen completo y por tanto
> **las 9 bases de golpe**. Esa opción se usa solo cuando quieres empezar de cero del todo.

Al recrear la base, PostgreSQL le asigna un **OID nuevo**, así que su carpeta en disco cambia.
No afecta a nada: los microservicios se conectan por nombre (`db_eventos`), nunca por OID.

### Advertencia: volúmenes huérfanos

Cuando borras un contenedor pero no su volumen, el volumen queda «huérfano» ocupando espacio. Para
verlos:

```powershell
docker volume ls --filter dangling=true
```

Esta máquina tiene **6**, de proyectos viejos. Se limpian con `docker volume prune`, pero **ese
comando borra datos de otros proyectos**, así que revisa la lista antes de ejecutarlo.

---

## `healthcheck`

**Archivo:** [`infra/docker-compose.yml`](../../infra/docker-compose.yml)
**Aparece 2 veces:** en los servicios `postgres` y `rabbitmq` (`mailhog` no lo lleva)
**Tecnología:** Docker / Docker Compose

```yaml
postgres:
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-teleticket} -d postgres"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 20s
```

### Qué problema resuelve

Para Docker, un contenedor está «corriendo» desde el momento en que el proceso arranca. Pero
**arrancar no es estar listo**: Postgres tarda unos segundos en terminar de iniciar el motor y
abrir el puerto, y RabbitMQ tarda bastante más. En ese hueco, `docker compose ps` ya dice `Up`
mientras cualquier conexión todavía falla.

Eso se nota sobre todo al levantar todo de golpe:

> Ejecutas `docker compose up -d`, arrancas un microservicio, y revienta con
> *connection refused* contra la base de datos. Vuelves a arrancarlo 10 segundos después y
> funciona. No había ningún error en el código: el contenedor estaba encendido pero no listo.

El `healthcheck` es un comando que Docker ejecuta **dentro del contenedor**, cada cierto tiempo,
para preguntarle «¿ya puedes atender?». Según la respuesta, el contenedor pasa a estado
`healthy` o `unhealthy`, y eso es lo que se ve en `docker compose ps`.

### Cómo funciona, paso a paso

1. El contenedor arranca. Docker le pone el estado `health: starting`.
2. Docker espera el `start_period` (el tiempo de gracia) y empieza a ejecutar el comando `test`
   cada `interval`.
3. Cada ejecución tiene como máximo `timeout` para responder. Si tarda más, cuenta como fallo.
4. El comando devuelve un **código de salida**: `0` significa sano, cualquier otro significa
   enfermo. (El código `0` es la convención de Unix para «terminó bien».)
5. En cuanto una ejecución devuelve `0`, el contenedor pasa a `healthy`.
6. Si falla `retries` veces **seguidas**, pasa a `unhealthy`. Un solo `0` reinicia la cuenta.

Un detalle importante: el estado `unhealthy` por sí solo **no reinicia ni apaga nada**. Es
información. Lo que sí hace es servirle a otros servicios para esperar, con
`depends_on: condition: service_healthy` (ver más abajo).

### Los cinco parámetros que se usan

| Parámetro        | Qué es                                                              |
| ---------------- | ------------------------------------------------------------------- |
| `test`           | El comando que decide si está sano. Es el único obligatorio         |
| `interval`       | Cada cuánto se repite el comando                                    |
| `timeout`        | Cuánto se le aguanta a cada ejecución antes de darla por fallida    |
| `retries`        | Cuántos fallos seguidos hacen falta para declararlo `unhealthy`     |
| `start_period`   | Tiempo de gracia inicial: los fallos de este tramo no se cuentan    |

#### `test` — el comando que decide

Tiene dos formas, y en este archivo se usa una en cada servicio:

| Forma                | Qué hace                                                                 | Dónde se usa |
| -------------------- | ------------------------------------------------------------------------ | ------------ |
| `["CMD", ...]`       | Ejecuta el programa **directamente**, sin shell                          | `rabbitmq`   |
| `["CMD-SHELL", "…"]` | Pasa la línea a `/bin/sh -c`, con lo que hay shell (variables, tuberías) | `postgres`   |

En `postgres` hace falta `CMD-SHELL` porque la línea trae `${POSTGRES_USER:-teleticket}`, que es
una variable con valor por defecto:

```yaml
test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-teleticket} -d postgres"]
```

`pg_isready` es una herramienta que trae Postgres y que no hace ninguna consulta: solo intenta la
conexión y devuelve `0` si el servidor acepta conexiones. Es lo más liviano que se puede
preguntar, y por eso se puede repetir cada 10 segundos sin molestar.

En `rabbitmq` basta `CMD`, porque es un programa con sus argumentos y nada más:

```yaml
test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
```

`rabbitmq-diagnostics ping` comprueba que el nodo responde. El `-q` es *quiet*: no imprime nada,
porque a Docker solo le interesa el código de salida, no el texto.

> **Ojo con la sintaxis de lista.** Si en lugar de la lista se escribe un string suelto
> (`test: pg_isready -U teleticket`), Compose lo trata como `CMD-SHELL`. Funciona, pero deja a
> medias quién corre el comando. Escribirlo como lista con `CMD` o `CMD-SHELL` deja explícito si
> hay shell o no.

#### `interval` — cada cuánto se pregunta

```yaml
# postgres
interval: 10s
# rabbitmq
interval: 15s
```

Es el tiempo entre el **final** de una comprobación y el **inicio** de la siguiente. Más corto
detecta antes los problemas, pero gasta CPU cada vez. Postgres va a 10s porque `pg_isready` es
casi gratis; RabbitMQ va a 15s porque `rabbitmq-diagnostics` es un comando de Erlang y pesa más.

#### `timeout` — cuánto se espera cada respuesta

```yaml
# postgres
timeout: 5s
# rabbitmq
timeout: 10s
```

Si el comando no termina en ese plazo, Docker lo corta y lo cuenta como fallo. Esto es lo que
evita que un servicio colgado se quede para siempre en `starting`: un comando que no responde es
exactamente igual de inútil que uno que responde «mal».

El valor tiene que ser **menor que el `interval`**; si no, las comprobaciones se solapan.

#### `retries` — cuántos fallos se perdonan

```yaml
retries: 5     # en los dos servicios
```

Evita las falsas alarmas: un fallo aislado (un pico de carga, un `timeout` por mala suerte) no
marca el contenedor como `unhealthy`. Hacen falta 5 fallos **consecutivos**.

Con esos números se puede calcular cuánto tarda en darse por vencido:

| Servicio   | Cálculo                     | Tiempo hasta `unhealthy`            |
| ---------- | --------------------------- | ----------------------------------- |
| `postgres` | `20s` + 5 × (`10s` + hasta `5s`) | entre ~70s y ~95s en el peor caso |
| `rabbitmq` | `40s` + 5 × (`15s` + hasta `10s`) | entre ~115s y ~165s             |

#### `start_period` — el tiempo de gracia

```yaml
# postgres
start_period: 20s
# rabbitmq
start_period: 40s
```

Es el parámetro **menos obvio y el más útil**. Durante este tramo inicial Docker igual ejecuta el
comando, pero **los fallos no cuentan** para `retries`:

- Si falla → el contenedor se queda en `starting`, sin gastar intentos.
- Si acierta → pasa a `healthy` de inmediato y el tiempo de gracia se termina ahí mismo. No se
  espera a que se cumplan los 20 o 40 segundos.

Sin `start_period`, un servicio que normalmente tarda un minuto en arrancar sería declarado
`unhealthy` mientras todavía estaba arrancando con total normalidad. RabbitMQ lleva 40s porque
levanta la máquina virtual de Erlang y los plugins de la consola web; Postgres se conforma con
20s.

### Por qué `mailhog` no lleva healthcheck

Porque no tiene con qué: la imagen `mailhog/mailhog` es una imagen mínima, sin `curl` ni `wget`
adentro, así que no hay un comando disponible para preguntarle si está vivo. Y tampoco importa
mucho: MailHog arranca al instante, guarda todo en memoria y ningún otro contenedor depende de
que esté listo. Un healthcheck ahí sería ruido.

### Para qué sirve de verdad: `depends_on`

Hoy el healthcheck solo informa, pero la razón de tenerlo puesto desde ahora es la **fase 15**,
cuando los 9 microservicios entren a este mismo archivo. Ahí se podrá escribir:

```yaml
ms-eventos:
  depends_on:
    postgres:
      condition: service_healthy
    rabbitmq:
      condition: service_healthy
```

`depends_on` a secas solo respeta el **orden de arranque**, que es justo lo que no sirve: arranca
Postgres y sigue de largo sin esperar a que esté listo. Con `condition: service_healthy`, Docker
**no arranca** `ms-eventos` hasta que el healthcheck de Postgres y el de RabbitMQ hayan dado
`healthy`. Eso elimina de raíz los *connection refused* del arranque, sin poner esperas a mano ni
reintentos en el código Java.

### Cómo comprobarlo

Ver el estado de salud de todos los contenedores:

```powershell
docker compose ps
```

→ la columna `STATUS` muestra `Up 2 minutes (healthy)`, `(starting)` o `(unhealthy)`.

Ver solo el de uno:

```powershell
docker inspect --format "{{.State.Health.Status}}" teleticket-postgres
```

Ver el historial de comprobaciones, con la salida y el código de salida de cada una (lo primero
que hay que mirar cuando un healthcheck falla y no se sabe por qué):

```powershell
docker inspect --format "{{json .State.Health}}" teleticket-postgres
```

Y probar el comando a mano, tal como lo ejecuta Docker:

```powershell
docker exec teleticket-postgres pg_isready -U teleticket -d postgres
docker exec teleticket-rabbitmq rabbitmq-diagnostics -q ping
```

### Un detalle que confunde

Un contenedor `unhealthy` **sigue corriendo y sigue aceptando conexiones**. Docker no lo apaga ni
lo reinicia por estar enfermo, y `restart: unless-stopped`
([ver arriba](#restart-unless-stopped)) tampoco se activa por eso: esa política reacciona a que
el proceso *termine*, no a que el healthcheck falle. `unhealthy` es un diagnóstico, no una
acción; quien decide hacer algo con ese dato es `depends_on`, un orquestador como Kubernetes, o
tú mirando `docker compose ps`.

---

## Cómo agregar una definición nueva

Cuando te topes con otra línea que no entiendas:

1. Agrega una fila al **Índice** de arriba.
2. Agrega una sección con el mismo formato: título con el término, **Archivo** donde aparece, qué
   problema resuelve, las alternativas, y por qué se eligió esa.
3. En el archivo del proyecto, deja un comentario de **una sola línea** apuntando aquí.
