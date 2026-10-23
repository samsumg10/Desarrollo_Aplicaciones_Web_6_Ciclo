# Infraestructura local — Teleticket

Aquí vive la «sala de máquinas» del proyecto: la base de datos, el mensajero y el buzón de
correo falso. **No hay código Java en esta carpeta.**

Todo se levanta con un solo comando, y los microservicios se conectan a estas piezas.

---

## Requisito previo

**Docker Desktop tiene que estar abierto y en marcha.** Si no lo está, cualquier comando `docker`
falla con un error del tipo:

```text
failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine
```

La solución es abrir Docker Desktop desde el menú Inicio y esperar a que el icono de la ballena
deje de animarse (~1 minuto).

---

## Comandos del día a día

Todos se ejecutan **desde esta carpeta `infra/`**.

| Qué quiero                               | Comando                             |
| ----------------------------------------- | ----------------------------------- |
| Levantar todo                             | `docker compose up -d`            |
| Ver si está arriba                       | `docker compose ps`               |
| Ver los logs en vivo                      | `docker compose logs -f`          |
| Ver los logs de uno                       | `docker compose logs -f postgres` |
| Apagar (**conservando** los datos)  | `docker compose down`             |
| Apagar y **borrar todos los datos** | `docker compose down -v`          |
| Reiniciar uno                             | `docker compose restart rabbitmq` |

> `-d` significa *detached*: los contenedores siguen corriendo en segundo plano y te devuelve el
> prompt.

---

## Qué se levanta

| Contenedor              | Imagen                           | Para qué              | Dónde lo veo          |
| ----------------------- | -------------------------------- | ---------------------- | ---------------------- |
| `teleticket-postgres` | `postgres:16-alpine`           | Las 9 bases de datos   | `localhost:5433`     |
| `teleticket-rabbitmq` | `rabbitmq:3-management-alpine` | Mensajería asíncrona | http://localhost:15672 |
| `teleticket-mailhog`  | `mailhog/mailhog:v1.0.1`       | Captura los correos    | http://localhost:8025  |

### Credenciales

| Servicio               | Usuario        | Contraseña    |
| ---------------------- | -------------- | -------------- |
| PostgreSQL             | `teleticket` | `teleticket` |
| RabbitMQ (consola web) | `guest`      | `guest`      |
| MailHog                | *(no pide)*  | —             |

> Son los valores **por defecto** de desarrollo local, a propósito simples. Se cambian con las
> variables `DB_USER`, `DB_PASSWORD`, `RABBIT_USER` y `RABBIT_PASSWORD` del `.env` de la raíz
> (copia de `.env.example`), y los puertos con `DB_PORT`, `RABBIT_PORT`, `RABBIT_CONSOLA_PORT`,
> `MAILHOG_SMTP_PORT` y `MAILHOG_WEB_PORT`. Para que el compose lea ese archivo:
> `docker compose --env-file ../.env up -d`. En un sistema real irían en un gestor de secretos,
> nunca en el repositorio.

---

## ⚠️ El puerto de PostgreSQL es el 5433, no el 5432

Esta máquina **ya tiene PostgreSQL 16 instalado** y ocupando el puerto 5432. Para no pelear con
él, el PostgreSQL de Docker se expone en el **5433**.

```text
Tu máquina                          Dentro de Docker
-----------                         ----------------
localhost:5433  ──────────────────▶ postgres:5432
```

Esto importa en dos momentos:

- **Al conectarte tú** (con `psql` o DBeaver desde Windows): usa el puerto **5433**.
- **Al configurar los microservicios** (fase 3 en adelante):
  - si el servicio corre desde Eclipse → `jdbc:postgresql://localhost:5433/db_eventos`
  - si el servicio corre como contenedor (fase 15) → `jdbc:postgresql://postgres:5432/db_eventos`

En el segundo caso se usa `postgres` como nombre de host (el nombre del servicio en el
`docker-compose.yml`) y el puerto interno 5432, porque la comunicación ocurre **dentro** de la red
de Docker y no pasa por Windows.

### Las variables de conexión

Para no tener que editar ningún `application.properties` al cambiar de escenario, los
microservicios leen la conexión de estas variables de entorno. **Si no están definidas, usan el
valor por defecto**, que es el del montaje local de esta carpeta:

| Variable        | Valor por defecto        | Para qué               |
| --------------- | ------------------------ | ----------------------- |
| `DB_HOST`     | `localhost`            | Dónde está PostgreSQL |
| `DB_PORT`     | `5433`                 | En qué puerto escucha  |
| `DB_NAME`     | la base de cada servicio | Qué base usar          |
| `DB_USER`     | `teleticket`           | Usuario                 |
| `DB_PASSWORD` | `teleticket`           | Contraseña             |

Así, lo único que cambia entre escenarios es `DB_HOST` y `DB_PORT`:

| Escenario                               | `DB_HOST`              | `DB_PORT`       | Qué hay que tocar |
| --------------------------------------- | ------------------------ | ----------------- | ------------------ |
| Desde Eclipse (fases 3–14)             | *(sin definir)*        | *(sin definir)* | **nada**     |
| Como contenedor (Fase 15)               | `postgres`             | `5432`          | solo el compose    |
| Un contenedor por servicio *(futuro)* | `postgres-eventos`, … | `5432`          | solo el compose    |
| Kubernetes *(futuro)*                 | el nombre del Service    | `5432`          | solo el manifiesto |

El contenedor de PostgreSQL usa las **mismas** `DB_USER` y `DB_PASSWORD` (el compose las pasa como
`POSTGRES_USER` y `POSTGRES_PASSWORD`). Solo se aplican al **crear** el volumen: si las cambias con
el volumen ya creado, la base queda inaccesible y hay que borrarlo con `docker compose down -v`.
Las 9 bases quedan a nombre de ese usuario (el script de `init-db/` ya no lo fija a `teleticket`).

El razonamiento completo de por qué hay una sola instancia y no nueve está en
[DEC-01](../docs/decisiones/DEC-01-bases-de-datos.md).

---

## Las 9 bases de datos

Las crea automáticamente [`init-db/01-crear-bases.sql`](init-db/01-crear-bases.sql) la primera
vez que arranca el contenedor.

| Base                  | Microservicio              | Llega en |
| --------------------- | -------------------------- | -------- |
| `db_usuarios`       | ms-usuarios                | Fase 4   |
| `db_eventos`        | ms-eventos                 | Fase 3   |
| `db_cola`           | ms-cola                    | Fase 7   |
| `db_inventario`     | ms-inventario              | Fase 6   |
| `db_ordenes`        | ms-ordenes                 | Fase 8   |
| `db_pagos`          | ms-pagos                   | Fase 9   |
| `db_tickets`        | ms-tickets                 | Fase 10  |
| `db_notificaciones` | ms-notificaciones          | Fase 13  |
| `db_reclamos`       | ms-reclamos *(opcional)* | Fase 17  |

Están **vacías**: no tienen ni una tabla. Cada microservicio crea las suyas con su propio
`schema.sql` a partir de la fase 3.

> **Importante:** el script de `init-db/` se ejecuta **solo cuando el volumen de datos está
> vacío**. Si lo modificas después, no volverá a correr. Para forzarlo:
> `docker compose down -v && docker compose up -d` (esto borra todos los datos).

---

## Cómo mirar dentro

### PostgreSQL

Listar las bases:

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d postgres -c "\l"
```

Entrar a una base en modo interactivo:

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_eventos
```

Dentro de `psql`: `\dt` lista las tablas, `\d nombre_tabla` describe una, `\q` sale.

Una consulta suelta sin entrar:

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_eventos -c "SELECT * FROM evento;"
```

Desde tu `psql` de Windows (ojo al puerto):

```powershell
psql -h localhost -p 5433 -U teleticket -d db_eventos
```

O con DBeaver / pgAdmin: host `localhost`, puerto `5433`, usuario y contraseña `teleticket`.

### RabbitMQ

Abre http://localhost:15672 (`guest` / `guest`). Las pestañas que vas a usar:

| Pestaña              | Para qué                                                                                                                   |
| --------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| **Queues**      | **La más importante.** Lista las colas y cuántos mensajes tiene cada una. Acá vas a *ver* los mensajes esperando |
| **Exchanges**   | Los exchanges y sus reglas de ruteo. Permite publicar un mensaje a mano para probar                                         |
| **Connections** | Qué microservicios están conectados                                                                                       |

Ahora mismo **no hay colas**: las crea el código Java en la fase 5. Ver el panel vacío es lo
correcto en esta fase.

### MailHog

Abre http://localhost:8025. Vas a ver una bandeja de entrada vacía, igual que un cliente de
correo. Los correos empiezan a llegar en la fase 13.

---

## Si algo sale mal

| Síntoma                                | Causa probable y solución                                                          |
| --------------------------------------- | ----------------------------------------------------------------------------------- |
| `failed to connect to the docker API` | Docker Desktop no está abierto. Ábrelo y espera                                   |
| `port is already allocated`           | Otro programa ocupa ese puerto. Averigua quién: `netstat -ano \| findstr :5433`   |
| Las bases no existen                    | El volumen ya existía de antes. `docker compose down -v && docker compose up -d` |
| `password authentication failed`      | Estás apuntando al PostgreSQL **local** (5432) en vez del de Docker (5433)   |
| Un contenedor en `unhealthy`          | Mira por qué: `docker compose logs postgres`                                     |
| Quiero empezar de cero                  | `docker compose down -v && docker compose up -d`                                  |

### Comprobación rápida de que todo está bien

```powershell
cd ..
.\verificar-infra.ps1
```

---

## Qué NO está en esta carpeta todavía

- Los `Dockerfile` de los 9 microservicios → **Fase 15**
- Los microservicios como contenedores en el `docker-compose.yml` → **Fase 15**
- El exchange y las colas de RabbitMQ → los declara el código Java en la **Fase 5**
- Las tablas de las bases de datos → cada microservicio, desde la **Fase 3**
