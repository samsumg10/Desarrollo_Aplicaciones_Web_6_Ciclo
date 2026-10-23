# Fase 3 — `ms-eventos`: catálogo de eventos y zonas

**Estado:** ✅ Completada · **Fecha:** 22/10/2026
**Cubre:** **CU-02** Consultar eventos · **CU-14** Configurar evento · **CU-15** Publicar reglas y horario
**Implementa:** **RN-01**, **RN-05** (el dato), **RN-14** (el dato) · **Resuelve:** **PRB-02**, **PRB-06**

---

## 1. En una frase

> **El primer microservicio de verdad.** Aparece la primera base de datos y el primer CRUD: ya se
> puede crear un evento con sus zonas, publicarlo y consultarlo desde el gateway. Y al publicarlo,
> **la hora de venta queda congelada para siempre**, que es la respuesta directa al incidente de
> 2017.

---

## 2. Qué se creó

### Archivos nuevos

| Archivo | Para qué sirve |
|---|---|
| `resources/schema.sql` | Crea las 3 tablas al arrancar |
| `entity/Evento.java` | El evento como clase Java |
| `entity/Zona.java` | Un sector del recinto con precio y aforo |
| `entity/EstadoEvento.java` | BORRADOR → EN_VENTA → FINALIZADO / CANCELADO |
| `repository/EventoRepository.java` | Consultas sobre la tabla `evento` |
| `repository/ZonaRepository.java` | Consultas sobre la tabla `zona` |
| `dto/CrearEventoRequest.java` | Lo que envía el operador, con sus validaciones |
| `dto/CrearZonaRequest.java` | Idem para una zona |
| `dto/EventoResponse.java` · `dto/ZonaResponse.java` | Lo que ve el cliente |
| `dto/ZonaInternaResponse.java` | Lo que necesitará `ms-ordenes` (Fase 8) |
| `dto/CondicionesResponse.java` | Lo que necesitará `ms-tickets` (Fase 10) |
| `service/EventoService.java` | **Las reglas de negocio** |
| `controller/EventoController.java` | La API pública `/api/eventos` |
| `controller/internal/EventoInternalController.java` | La API interna `/internal/eventos` |
| `exception/NoEncontradoException.java` | → `404` |
| `exception/ReglaNegocioException.java` | → `409` |
| `exception/ManejadorErrores.java` | Convierte las excepciones en JSON legible |

### Archivos modificados

| Archivo | Qué se le agregó |
|---|---|
| `ms-eventos/pom.xml` | `spring-boot-starter-data-jpa`, `postgresql`, `spring-boot-starter-validation` |
| `ms-eventos/.../application.properties` | Conexión a `db_eventos` (plantilla de DEC-01) y configuración de JPA |

### Tablas de base de datos

Todas en `db_eventos`, creadas por `schema.sql`.

| Tabla | Campos clave | Restricciones importantes |
|---|---|---|
| `evento` | id, nombre, fecha, recinto, **inicio_venta**, estado, max_por_cliente, **condiciones_ingreso**, **reglas_cola** | `estado` solo admite los 4 valores válidos; `max_por_cliente` entre 1 y 20 |
| `zona` | id, evento_id, nombre, precio, aforo | **`UNIQUE(evento_id, nombre)`** — no hay dos «VIP» en el mismo evento |
| `preventa` | id, evento_id, zona_id, banco, inicio, fin, descuento, stock | `fin > inicio`. Tabla creada; sus endpoints quedan fuera de esta fase |

### Endpoints

**API pública** (por el gateway: `http://localhost:8090`)

| Método | Ruta | CU | Qué hace |
|---|---|---|---|
| `GET` | `/api/eventos` | CU-02 | Lista los eventos **EN_VENTA**. Con `?incluirBorradores=true`, todos |
| `GET` | `/api/eventos/{id}` | CU-02 | Un evento con sus zonas, precios, hora y condiciones |
| `POST` | `/api/eventos` | CU-14 | Crea un evento en **BORRADOR** → `201` |
| `POST` | `/api/eventos/{id}/zonas` | CU-14 | Agrega una zona → `201` |
| `PUT` | `/api/eventos/{id}` | CU-14 | Modifica. **`409` si ya está publicado** (RN-01) |
| `PUT` | `/api/eventos/{id}/publicar` | CU-15 | Pasa a **EN_VENTA** y congela todo |

**API interna** (🔒 solo por puerto directo `8082`, sin ruta en el gateway)

| Método | Ruta | Quién lo llamará | Para qué |
|---|---|---|---|
| `GET` | `/internal/eventos/{id}/zonas/{zonaId}` | `ms-ordenes` (Fase 8) | Precio, aforo, máximo por cliente y si la venta está abierta |
| `GET` | `/internal/eventos/{id}/condiciones` | `ms-tickets` (Fase 10) | Copiar las condiciones en cada e-ticket |

### Mensajes de RabbitMQ

Ninguno todavía. En la **Fase 5**, `PUT /publicar` publicará `evento.publicado`.

---

## 3. Conceptos nuevos explicados

**JPA y entidad**
Una **entidad** es una clase Java que representa una fila de una tabla. JPA traduce entre las dos,
así que el código trabaja con objetos (`evento.getNombre()`) en vez de con SQL.

**Repositorio**
Una interfaz que da acceso a una tabla. Lo llamativo es que **no se escribe la implementación**:
Spring Data la genera a partir del nombre de cada método.

```java
List<Evento> findByEstadoOrderByInicioVentaAsc(EstadoEvento estado);
```
se traduce solo a `SELECT * FROM evento WHERE estado = ? ORDER BY inicio_venta`.

**DTO, y por qué no se devuelve la entidad**
Un **DTO** es el objeto que entra o sale por la API. Se usa en vez de la entidad por tres razones:
el cliente no puede mandar campos que no le tocan (como `estado` o `id`), no se expone la estructura
interna de la base, y cada consumidor recibe solo lo que usa.

Se ve claro aquí: `ZonaResponse` (público) no incluye el máximo por cliente, y
`ZonaInternaResponse` (para ms-ordenes) sí.

**`record`**
Una clase de solo lectura con menos ceremonia: Java genera el constructor y los getters. Ideal para
DTO.

**Validación con anotaciones**
`@NotBlank`, `@Min`, `@DecimalMin`… se comprueban **antes** de tocar la base de datos. Si fallan,
la respuesta es `400` con la lista de campos incorrectos.

**`@Transactional`**
Agrupa varias operaciones en una sola unidad: o todas se guardan, o ninguna. Si agregar una zona
falla a medio camino, no queda nada a medias.

**`@RestControllerAdvice`**
Un sitio único donde se convierten las excepciones en respuestas. Sin esto, un error devolvería un
volcado de pila que no sirve ni al comprador ni a quien depura.

**El problema N+1 y `JOIN FETCH`**
Si pides un evento y luego recorres sus zonas, JPA haría una consulta para el evento y otra para las
zonas. Con 100 eventos serían 101 consultas. `LEFT JOIN FETCH` las trae en **una**.

```java
@Query("SELECT e FROM Evento e LEFT JOIN FETCH e.zonas WHERE e.id = :id")
```

**`BigDecimal` para el dinero, nunca `double`**
Los decimales de coma flotante pierden precisión: `0.1 + 0.2` no da exactamente `0.3`. Con dinero
eso termina en céntimos que no cuadran. En la base es `NUMERIC(10,2)`.

**`ddl-auto=validate`**
Hibernate **no** crea ni modifica tablas: solo comprueba que las que existen coincidan con las
entidades. Las tablas las crea `schema.sql`. Así el esquema está en un sitio, es revisable, y nunca
una tabla cambia sola por un descuido en el código.

---

## 4. Qué puedo hacer ahora

- ✅ Crear un evento con su nombre, fecha, recinto, hora de venta y condiciones de ingreso
- ✅ Agregarle zonas con precio y aforo
- ✅ Modificarlo **mientras siga en BORRADOR**
- ✅ Publicarlo, y comprobar que a partir de ahí **ya no se puede cambiar la hora** (RN-01)
- ✅ Consultar el catálogo público desde el gateway
- ✅ Ver que los datos están realmente en PostgreSQL
- ✅ Ver errores con mensajes claros y el `requestId` para rastrearlos

---

## 5. Cómo lo compruebo

> Los cuerpos JSON van en archivos (`-d "@evento.json"`) en lugar de en línea. Es más legible y
> evita el lío de escapar comillas en PowerShell.

### Paso 1 — Crear un evento (CU-14)

Crea `evento.json`:

```json
{
  "nombre": "Concierto Prueba Teleticket 2026",
  "fecha": "2026-12-20T20:00:00Z",
  "recinto": "Estadio Nacional",
  "inicioVenta": "2026-11-15T10:00:00Z",
  "maxPorCliente": 4,
  "condicionesIngreso": "Entrada digital valida en el celular. No se exige impresion. Presentar DNI fisico del asistente nominado."
}
```

```powershell
curl.exe -s -X POST http://localhost:8090/api/eventos `
  -H "Content-Type: application/json" `
  -H "X-Usuario-Id: 00000000-0000-0000-0000-000000000001" `
  -d "@evento.json"
```
→ `201` con el evento en estado `BORRADOR`. **Copia el `id`.**

```powershell
$EID = "<pega-aqui-el-id>"
```

### Paso 2 — Agregarle zonas

```powershell
'{"nombre":"VIP","precio":450.00,"aforo":100}'     | Set-Content zona.json
curl.exe -s -X POST "http://localhost:8090/api/eventos/$EID/zonas" -H "Content-Type: application/json" -d "@zona.json"

'{"nombre":"Platea","precio":280.00,"aforo":300}'  | Set-Content zona.json
curl.exe -s -X POST "http://localhost:8090/api/eventos/$EID/zonas" -H "Content-Type: application/json" -d "@zona.json"
```

### Paso 3 — Zona duplicada → `409`

Vuelve a enviar la zona «VIP».
→ `{"error":"ZONA_DUPLICADA","mensaje":"El evento ya tiene una zona llamada 'VIP'"}`

### Paso 4 — Validaciones → `400` con detalle por campo

Envía un evento con `"nombre":""` y `"maxPorCliente":99`:

```json
{
  "error": "DATOS_INVALIDOS",
  "estado": 400,
  "campos": {
    "nombre": "El nombre del evento es obligatorio",
    "maxPorCliente": "El maximo por cliente no puede pasar de 20",
    "condicionesIngreso": "Las condiciones de ingreso son obligatorias: lo informado es lo que se valida en la puerta"
  }
}
```

### Paso 5 — RN-01, **la prueba más importante de la fase**

**Antes** de publicar, cambiar la hora de venta funciona:

```powershell
curl.exe -s -X PUT "http://localhost:8090/api/eventos/$EID" -H "Content-Type: application/json" -d "@evento-con-otra-hora.json"
```
→ `200`, la hora cambió.

Ahora **publica** el evento:

```powershell
curl.exe -s -X PUT "http://localhost:8090/api/eventos/$EID/publicar"
```
→ `200`, estado `EN_VENTA`, con `publicadoEn` y `aforoTotal`.

Y repite **exactamente el mismo cambio de hora** de antes:

→ **`409`** 🎉

```json
{
  "error": "EVENTO_CONGELADO",
  "mensaje": "No se puede modificar la hora de venta ni las reglas de un evento EN_VENTA. Se publicaron al comprador y cambiarlas seria repetir el problema de 2017."
}
```

> **Esta es la diapositiva de PRB-02 para tu sustentación.** El mismo cambio es válido antes de
> publicar y prohibido después. En 2017 la cola se anunció para las 6:00 a. m. y el sistema reordenó
> a todos desde las 5:00. Aquí eso es imposible: una vez publicada, la hora no se puede tocar.

### Paso 6 — La API interna, y que no se alcanza desde fuera

Por el **puerto directo** (como lo llamará `ms-ordenes`):

```powershell
curl.exe -s "http://localhost:8082/internal/eventos/$EID/zonas/<zonaId>"
```
→ precio, aforo, `maxPorCliente` y `ventaAbierta`.

Por el **gateway**, el mismo endpoint:

```powershell
curl.exe -s -o NUL -w "%{http_code}" "http://localhost:8090/internal/eventos/$EID/zonas/<zonaId>"
```
→ **`404`** ✅ Y como el evento **sí existe**, el 404 solo puede significar una cosa: el gateway no
tiene ruta hacia `/internal/**`.

### Paso 7 — El catálogo público solo muestra lo publicado

```powershell
curl.exe -s http://localhost:8090/api/eventos                          # solo EN_VENTA
curl.exe -s "http://localhost:8090/api/eventos?incluirBorradores=true" # todos
```

### Paso 8 — Mirar la base de datos con tus propios ojos

```powershell
docker exec -it teleticket-postgres psql -U teleticket -d db_eventos -c "SELECT nombre, estado, inicio_venta FROM evento;"
docker exec -it teleticket-postgres psql -U teleticket -d db_eventos -c "SELECT nombre, precio, aforo FROM zona ORDER BY precio DESC;"
```

### Resultado real de esta implementación

| Verificación | Resultado |
|---|---|
| Las 3 tablas se crean al arrancar | ✅ `evento`, `zona`, `preventa` |
| Crear evento por el gateway | ✅ `201`, estado `BORRADOR` |
| Fechas en ISO-8601 UTC | ✅ `2026-11-15T10:00:00Z` |
| Agregar 3 zonas | ✅ `201` en las tres |
| Zona duplicada | ✅ `409 ZONA_DUPLICADA` |
| Validaciones del DTO | ✅ `400` con los 3 campos erróneos identificados |
| Evento inexistente | ✅ `404 NO_ENCONTRADO`, con `ruta` y `requestId` |
| **El `requestId` del gateway llega al error de ms-eventos** | ✅ Trazabilidad de punta a punta |
| Cambiar la hora **antes** de publicar | ✅ `200` |
| Publicar | ✅ `EN_VENTA`, `aforoTotal: 900` |
| Cambiar la hora **después** de publicar | ✅ **`409 EVENTO_CONGELADO`** |
| Agregar zona a un evento publicado | ✅ `409 EVENTO_CONGELADO` |
| Publicar dos veces | ✅ `409 YA_PUBLICADO` |
| API interna por puerto directo | ✅ Devuelve los datos |
| API interna por el gateway | ✅ `404` con un evento que **sí** existe |
| Catálogo público vs. con borradores | ✅ 1 evento vs. 2 |
| Datos en PostgreSQL | ✅ Verificado con `psql` |
| No regresión | ✅ `9/9 servicios vivos`, infraestructura intacta |

> **Datos de prueba:** quedaron en `db_eventos` un evento publicado con 3 zonas (900 de aforo) y uno
> en borrador. El publicado es útil para las fases 5, 6 y 7. Para borrarlos:
> `DROP DATABASE db_eventos;` + `CREATE DATABASE db_eventos OWNER teleticket;`
> (ver [`volumes:`](../definiciones/definiciones-de-codigo.md#volumes)).

---

## 6. Qué sigue sin funcionar

| No funciona | Llega en |
|---|---|
| Publicar un evento **no avisa a nadie**: no se crea stock ni sala de espera | Fase 5 (el mensaje) y Fase 6 (el stock) |
| `GET /api/eventos` no dice **cuántas entradas quedan**: eso es de ms-inventario | Fase 6 |
| Los endpoints internos existen pero **nadie los llama todavía** | Fases 8 y 10 |
| No hay preventas: la tabla existe, los endpoints no | Fuera del alcance de esta fase |
| Cualquiera puede crear eventos: no se comprueba el rol OPERADOR | Cuando llegue Spring Security |
| El parámetro `?incluirBorradores=true` reemplaza al control de rol | Idem |

---

## 7. Decisiones tomadas y problemas encontrados

### Problema 1 — `defer-datasource-initialization` hace lo contrario de lo que parece

El servicio no arrancaba:

```text
Schema validation: missing table [evento]
```

Yo había puesto `spring.jpa.defer-datasource-initialization=true` creyendo que *adelantaba* el
`schema.sql`. Hace lo contrario: lo **retrasa** hasta después de que Hibernate valide. Resultado:
Hibernate buscaba las tablas antes de que existieran.

| Valor | Cuándo se ejecuta `schema.sql` | Para qué sirve |
|---|---|---|
| **`false`** ← lo correcto aquí | **Antes** de que Hibernate valide | Las tablas las crea el script |
| `true` | **Después** | Las crea Hibernate (`ddl-auto=create`) y `data.sql` solo inserta datos |

Corregido a `false`, con un comentario en el archivo que explica la trampa.

### Problema 2 — Spring Boot 4 trae Jackson 3, y rompe una propiedad de todos los tutoriales

Segundo fallo de arranque:

```text
No enum constant tools.jackson.databind.SerializationFeature.write-dates-as-timestamps
```

Spring Boot 4 usa **Jackson 3** (`tools.jackson`, no `com.fasterxml.jackson`), y ahí
`WRITE_DATES_AS_TIMESTAMPS` **salió** de `SerializationFeature`. La propiedad
`spring.jackson.serialization.write-dates-as-timestamps`, que aparece en casi cualquier tutorial de
Spring Boot 3, **impide arrancar** en Boot 4.

**La solución fue quitarla:** Jackson 3 ya serializa `Instant` como ISO-8601 UTC por defecto. Se
verificó en la respuesta real: `"inicioVenta": "2026-11-15T10:00:00Z"`. Queda anotado en el archivo
de propiedades para no volver a caer.

> Los dos problemas salieron a la luz **al arrancar el servicio**, no al compilar. Compilar solo
> prueba que el código es válido; arrancar prueba que la configuración lo es.

### Decisión 1 — Métodos de fábrica en vez de constructores públicos

`Evento.crear(...)` y `Zona.crear(...)` en lugar de `new Evento()`. Así ningún evento puede existir
sin `id`, sin `estado` o sin fecha de creación: los campos obligatorios se asignan en un solo sitio
y no se pueden olvidar.

### Decisión 2 — `estaCongelado()` como método de la entidad

RN-01 se consulta en tres sitios (modificar, agregar zona, publicar). En vez de repetir
`evento.getEstado() != BORRADOR`, la propia entidad responde `estaCongelado()`. La regla vive junto
al dato que la define, y si algún día cambia se cambia en un solo lugar.

### Decisión 3 — Reglas de la cola por defecto

`reglasCola` es opcional al crear el evento. Si no se envía, se guarda un texto por defecto con las
5 reglas del diseño (sorteo único, admisión por ciclos, pase de un solo uso…).

El motivo es de cumplimiento: PRB-02 es precisamente que las reglas no estaban publicadas. Dejar el
campo vacío permitiría publicar un evento **sin reglas visibles**, que es el problema original. Con
un valor por defecto, eso no puede pasar.

### Decisión 4 — `condicionesIngreso` es obligatorio

Marcado `@NotBlank`. Es el texto que `ms-tickets` copiará en cada e-ticket y `ms-notificaciones` en
cada correo. Si estuviera vacío, el e-ticket saldría sin condiciones y se repetiría PRB-06 (la multa
de 2,57 UIT por anunciar una entrada digital y exigirla impresa).

### Decisión 5 — No se puede publicar un evento sin zonas

`409 SIN_ZONAS`. Dos razones: sin zonas no hay nada que vender, y en la Fase 6 `ms-inventario` no
tendría aforos con los que crear el stock. Mejor fallar al publicar que dejar un evento publicado e
incomprable.

### Decisión 6 — El `aforo` de la zona nunca baja

`zona.aforo` es el **total**, no lo disponible. Lo disponible lo llevará `ms-inventario` en su propia
base. Es *database per service* en acción: cada servicio responde solo por lo suyo. Por eso
`ZonaResponse` no incluye «entradas restantes» — ms-eventos no lo sabe, y no debe saberlo.

---

## 8. Relación con el informe

| Requisito del `.docx` | Cómo lo cumple esta fase |
|---|---|
| **CU-02** Consultar eventos y disponibilidad por zona | ✅ `GET /api/eventos` y `/{id}`. La disponibilidad llega en la Fase 6 |
| **CU-14** Configurar evento, zonas, stock y preventas | ✅ `POST /api/eventos` y `/zonas`. Preventas fuera de esta fase |
| **CU-15** Publicar reglas y horario de la venta | ✅ `PUT /{id}/publicar` |
| **RN-01** — «La hora de inicio y las reglas se publican antes y no cambian durante la venta» | ✅ `409 EVENTO_CONGELADO`, probado en los dos sentidos |
| **RN-05** — máximo por comprador | ✅ Se define aquí (`max_por_cliente`) y lo aplicará ms-ordenes |
| **RN-14** — las condiciones se informan y se respetan | ✅ Obligatorias, y expuestas en `/internal/.../condiciones` |
| **PRB-02** Cola injusta y no transparente | Hora y reglas congeladas al publicar |
| **PRB-06** Información contradictoria | Las condiciones viajarán desde aquí al e-ticket y al correo |
| **Ley 32415** — no emitir más entradas que el aforo | El aforo por zona es el tope que usará ms-inventario |
| **Sección 7.1 del diseño** — tablas `evento`, `zona`, `preventa` | ✅ Las 3, con las restricciones indicadas |
| **Sección 8.2 del diseño** — API pública e interna de ms-eventos | ✅ Los 6 endpoints públicos y los 2 internos |
| **DEC-01** — conexión por variables de entorno | ✅ Primera aplicación de la plantilla |

---

## Siguiente paso

**[Fase 4 — ms-usuarios: registro y roles](../../PLAN-DE-IMPLEMENTACION.md)** (CU-01), o
directamente la **Fase 5**, que es la conceptualmente más importante del proyecto: la librería de
mensajería común y el primer mensaje de RabbitMQ, donde `PUT /publicar` empezará a avisar a
`ms-cola` y `ms-inventario`.
