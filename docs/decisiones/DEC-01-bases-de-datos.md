# DEC-01 — Una instancia de PostgreSQL con una base de datos por servicio

**Fecha:** 22/10/2026 · **Estado:** ✅ Decidido
**Afecta a:** Fases 1, 3–14 y 15 · **Relacionado con:** OBJ 2, PRB-01

---

## Qué es este documento

Un **registro de decisión**: deja escrito *por qué* se eligió una opción, no *cómo* se implementa.
Vive separado de los documentos de fase porque esta decisión no pertenece a una fase concreta:
afecta a todas las que usan base de datos.

---

## La pregunta que lo originó

> «¿Qué tanto cambiaría la estructura del proyecto si decido que cada servicio tenga su propia base
> de datos? ¿Tendría que haber varios contenedores en Docker, uno para cada servicio?»

---

## La aclaración más importante

**El proyecto ya tiene una base de datos por servicio.** La Fase 1 creó **9 bases independientes**
dentro de un servidor PostgreSQL:

```text
teleticket-postgres  (1 contenedor)
├── db_usuarios        ← solo ms-usuarios puede tocarla
├── db_eventos         ← solo ms-eventos
├── db_cola            ← solo ms-cola
├── db_inventario      ← solo ms-inventario
├── db_ordenes         ← solo ms-ordenes
├── db_pagos           ← solo ms-pagos
├── db_tickets         ← solo ms-tickets
├── db_notificaciones  ← solo ms-notificaciones
└── db_reclamos        ← solo ms-reclamos (opcional)
```

El patrón *database per service* trata sobre **quién es dueño de las tablas**, no sobre cuántos
servidores hay corriendo. Lo que se comparte hoy es el **proceso servidor**, no los datos.

La garantía real del patrón —que ningún servicio lea las tablas de otro— ya está en pie, y el
informe la enuncia así:

> «Ningún servicio lee la base de datos de otro: si necesita un dato, lo pide por Feign o lo recibe
> en un mensaje.» *(informe, sección 6.3.2)*

Esa regla se cumple porque cada microservicio solo conoce la URL de **su** base, y porque las bases
son objetos separados en PostgreSQL: desde `db_ordenes` no se puede hacer un `SELECT` contra una
tabla de `db_pagos`.

### ¿Dónde viven físicamente las 9 bases?

Las 9 bases **comparten un solo volumen de Docker**, porque comparten el proceso de PostgreSQL:

| Volumen | Tamaño | Contenido |
|---|---|---|
| `teleticket_postgres-datos` | 142 MB | **Las 9 bases juntas** |
| `teleticket_rabbitmq-datos` | 142,5 kB | Configuración y mensajes de RabbitMQ |
| *(MailHog no tiene)* | — | Guarda los correos en memoria, a propósito |

Y dentro de ese volumen, PostgreSQL no las guarda con su nombre, sino numeradas por **OID**:

| Base | Carpeta real | Base | Carpeta real |
|---|---|---|---|
| `db_usuarios` | `base/16384/` | `db_pagos` | `base/16389/` |
| `db_eventos` | `base/16401/` | `db_tickets` | `base/16390/` |
| `db_cola` | `base/16386/` | `db_notificaciones` | `base/16391/` |
| `db_inventario` | `base/16387/` | `db_reclamos` | `base/16392/` |
| `db_ordenes` | `base/16388/` | | |

> Los números son un ejemplo: el OID cambia si la base se vuelve a crear.

**Por eso respaldar una base es `pg_dump`, no copiar una carpeta.** Los comandos concretos para
respaldar, restaurar o resetear una sola base están en la entrada
[`volumes:`](../definiciones/definiciones-de-codigo.md#volumes) del documento de definiciones.

---

## Lo que ya decían los documentos entregados

La pregunta estaba respondida desde antes de empezar a programar.

**Informe del proyecto, sección 6.3.3 (Despliegue):**

> «En el entorno del proyecto, todos los componentes corren como contenedores definidos en un
> archivo docker-compose: el API Gateway, los ocho microservicios, **PostgreSQL (una instancia con
> una base de datos distinta por servicio)**, RabbitMQ y MailHog.»

**Documento de arquitectura, sección 7:**

> «**Patrón:** *database per service*. Cada microservicio es dueño de sus tablas y nadie más las
> toca. En desarrollo se usa **una sola instancia de PostgreSQL con 8 bases distintas**. En
> Kubernetes, cada base podrá tener su propio contenedor.»

**OBJ 2 del informe:**

> «Implementar […] ocho microservicios Spring Boot, **cada uno con su propia base de datos
> PostgreSQL** […]»

«Su propia base de datos» = su propia base, no su propio servidor. El montaje actual cumple el
OBJ 2 y es exactamente lo que especifica la sección 6.3.3.

---

## Opciones evaluadas

Las cifras de RAM se midieron en la máquina de desarrollo con `docker stats`; no son estimaciones.

| | A. Una instancia *(actual)* | B. Nueve contenedores |
|---|---|---|
| **RAM** | **21 MB** (medido) | ~190 MB (9 × 21) |
| **Servicios en el compose** | 3 | 11 |
| **Volúmenes** | 2 (uno con las 9 bases dentro) | 10 (uno por base) |
| **Arranque en frío** | ~10 s | ~60–90 s (9 × `initdb`) |
| **Código Java** | — | **sin cambios** |
| **Aislamiento de datos** | ✅ Completo | ✅ Completo |
| **Aislamiento de fallos** | ❌ Si cae el contenedor, caen las 9 bases | ✅ Un fallo afecta a un servicio |
| **Coherencia con el informe** | ✅ Es lo que pide 6.3.3 | ❌ Habría que corregir el `.docx` y los 2 `.md` |

La RAM no es el factor decisivo: hay 15,3 GB disponibles. El costo real de la opción B es
**complejidad y tiempo de arranque**, a cambio de un beneficio que en un proyecto académico es
sobre todo visual.

### El argumento honesto a favor de separar

Con una sola instancia, si ese contenedor muere, los 9 servicios pierden su base **al mismo
tiempo**. Eso roza el problema que el proyecto quiere resolver (PRB-01: «si un servicio falla, los
demás siguen»).

Es un argumento legítimo, y es precisamente el motivo por el que el diseño deja la separación
prevista para Kubernetes. En desarrollo no pesa, porque un contenedor caído se levanta en segundos
y no hay usuarios reales.

---

## Decisión

**Mantener una instancia de PostgreSQL con 9 bases (opción A), y externalizar la configuración de
conexión** para que migrar a la opción B —o a Kubernetes— sea cambiar **solo el
`docker-compose.yml`**.

**Motivos:**

1. Es lo que especifica la sección 6.3.3 del informe. Cambiarlo obligaría a corregir los tres
   documentos ya entregados.
2. La garantía del patrón (aislamiento de datos) ya está cumplida; la opción B no añade nada en ese
   aspecto.
3. Menos piezas que puedan fallar el día de la sustentación.
4. El aislamiento de fallos, que es el único beneficio real, el diseño ya lo tiene previsto para la
   etapa de Kubernetes.

---

## Cómo queda la configuración

Cada servicio con base de datos usa esta plantilla, desde la Fase 3:

```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:db_eventos}
spring.datasource.username=${DB_USER:teleticket}
spring.datasource.password=${DB_PASSWORD:teleticket}
```

Lo que va después de los dos puntos es el **valor por defecto**: si no hay variable de entorno, usa
ese. Por eso funciona desde Eclipse sin configurar nada.

### Por qué partir la URL en variables

Spring Boot ya permite sobrescribir la URL completa con `SPRING_DATASOURCE_URL`. Pero entonces el
`docker-compose.yml` tendría que repetir la URL JDBC entera nueve veces. Partiéndola, **cambiar de
escenario es cambiar un solo valor**:

| Escenario | `DB_HOST` | `DB_PORT` | Qué hay que tocar |
|---|---|---|---|
| Hoy, desde Eclipse | *(sin definir)* → `localhost` | `5433` | nada |
| Fase 15, en Docker | `postgres` | `5432` | solo el compose |
| Opción B: 1 contenedor por servicio | `postgres-eventos` | `5432` | solo el compose |
| Kubernetes | el nombre del Service | `5432` | solo el manifiesto |

**En ningún escenario cambia el código Java.** Ni una línea.

---

## Cómo migrar a la opción B, si algún día se quiere

No hace falta decidirlo ahora. Si más adelante se quisiera un contenedor por servicio, estos son
los pasos, y **ninguno toca Java**:

1. En `infra/docker-compose.yml`, reemplazar el servicio `postgres` por nueve, uno por base:

   ```yaml
   postgres-eventos:
     image: postgres:16-alpine
     environment:
       POSTGRES_USER: teleticket
       POSTGRES_PASSWORD: teleticket
       POSTGRES_DB: db_eventos      # cada uno crea SU base; ya no hace falta init-db/
     volumes:
       - postgres-eventos-datos:/var/lib/postgresql/data
   ```

2. Borrar `infra/init-db/01-crear-bases.sql`: con `POSTGRES_DB`, cada contenedor crea su propia base
   y el script deja de tener sentido.
3. Declarar los nueve volúmenes.
4. Pasarle a cada microservicio su `DB_HOST` (`postgres-eventos`, `postgres-cola`, …) y
   `DB_PORT=5432`.
5. Si hace falta conectarse desde Windows a cada una, mapear puertos distintos (5441–5449).
6. **Corregir la sección 6.3.3 del informe y la sección 7 del documento de arquitectura**, que dicen
   «una instancia».

Esfuerzo estimado: una tarde, casi todo en el `docker-compose.yml`.

---

## La respuesta para la sustentación

Si en la exposición preguntan si cada microservicio tiene su propia base de datos:

> **«Sí. Cada microservicio tiene su propia base de datos y es el único dueño de sus tablas: ninguno
> puede leer las de otro, y si necesita un dato ajeno lo pide por Feign o lo recibe en un mensaje de
> RabbitMQ. Las nueve bases comparten la instancia de PostgreSQL porque así lo especifica la sección
> 6.3.3 del informe, y el diseño deja previsto separarlas en contenedores independientes al pasar a
> Kubernetes. La configuración ya está externalizada en variables de entorno, así que ese cambio no
> tocaría código.»**

---

## Referencias

- [`infra/docker-compose.yml`](../../infra/docker-compose.yml) — la instancia única
- [`infra/init-db/01-crear-bases.sql`](../../infra/init-db/01-crear-bases.sql) — las 9 bases
- [`docs/fases/FASE-01-infraestructura.md`](../fases/FASE-01-infraestructura.md) — el montaje
- `Proyecto_Investigacion_Teleticket_reducido.docx`, secciones 6.3.2 y 6.3.3
- `arquitectura-microservicios-teleticket.md`, secciones 7 y 11
