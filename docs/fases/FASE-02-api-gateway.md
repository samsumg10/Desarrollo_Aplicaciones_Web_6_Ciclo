# Fase 2 — API Gateway con enrutamiento real

**Estado:** ✅ Completada · **Fecha:** 22/10/2026
**Cubre:** ningún caso de uso directo (es infraestructura de entrada)
**Resuelve:** primera barrera de **PRB-03** (saltarse la cola) · **Producto N.º 3** del informe

---

## 1. En una frase

> Antes tenías que saber el puerto de cada microservicio para hablarle. Ahora **un solo puerto
> (8090)** te lleva a todos, y los tres servicios internos quedaron **inalcanzables desde fuera**.

---

## 2. Qué se creó

### Archivos nuevos

| Archivo | Para qué sirve |
|---|---|
| `ms-api-gateway/.../filter/RequestIdFilter.java` | Pone un identificador único a cada petición, para poder seguir una compra por los logs de todos los servicios |
| `ms-api-gateway/.../filter/PaseCompraFilter.java` | Rechaza con `400` cualquier intento de crear una orden sin pase de compra |
| `ms-api-gateway/.../config/CorsConfig.java` | Permiso para que el futuro front llame al gateway desde el navegador |
| `docs/fases/FASE-02-api-gateway.md` | Este documento |

### Archivos modificados

| Archivo | Qué se le agregó | Por qué era seguro |
|---|---|---|
| `ms-api-gateway/pom.xml` | `spring-cloud-starter-gateway-server-webmvc` (sin versión: la decide el BOM de la Fase 0) | Solo agrega una dependencia |
| `ms-api-gateway/.../application.properties` | La tabla de 5 rutas y los orígenes de CORS | El puerto y el actuator no cambian |
| Los 5 `PingController` de servicios ruteados | Una segunda ruta `/api/<servicio>/ping` | Se **agrega** una ruta; la `/ping` original sigue igual |

### La tabla de rutas

| Ruta pública | Destino | Quién la usa |
|---|---|---|
| `/api/usuarios/**` | `localhost:8081` | Visitante, comprador |
| `/api/eventos/**` | `localhost:8082` | Todos / operador |
| `/api/cola/**` | `localhost:8083` | Comprador |
| `/api/ordenes/**` | `localhost:8085` | Comprador |
| `/api/tickets/**` | `localhost:8087` | Comprador, controlador |
| **(sin ruta)** | `ms-inventario`, `ms-pagos`, `ms-notificaciones` | **Solo otros servicios** |

> **Lo más importante de esta tabla es lo que NO está.** Esos tres servicios no tienen ruta, y esa
> ausencia es una medida de seguridad: sin ruta, nadie puede reservar stock ni cobrar desde fuera.
> Es parte de la solución a PRB-03.

### Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/ping` (en el 8090) | El ping del gateway mismo |
| `GET` | `/actuator/health` | Salud del gateway |
| *(todas)* | `/api/**` | Se reenvían al microservicio que corresponda |

### Mensajes de RabbitMQ

Ninguno. La mensajería llega en la Fase 5.

---

## 3. Conceptos nuevos explicados

**API Gateway**
Es el **único teléfono público** del sistema. El navegador y la app móvil solo conocen
`http://localhost:8090`; no saben que detrás hay 8 microservicios en 8 puertos. Eso trae tres
ventajas: el cliente se simplifica, los puertos internos se pueden cambiar sin avisar a nadie, y
hay un sitio único donde aplicar reglas comunes (trazabilidad, CORS, el pase de compra).

**Enrutamiento por prefijo**
Cada regla dice «lo que empiece por `/api/eventos/`, mándalo al 8082». El gateway no entiende de
eventos ni de órdenes: solo mira el inicio de la dirección y reenvía.

**El gateway reenvía la ruta COMPLETA**
Esto es lo que más confunde al principio. Si pides `/api/eventos/ping`, el gateway lo manda a
`localhost:8082/api/eventos/ping`, **no** a `localhost:8082/ping`. No recorta el prefijo.

Por eso los controladores de cada servicio se mapean en `/api/<servicio>` y no en la raíz. Lo verás
en la Fase 3: `EventoController` lleva `@RequestMapping("/api/eventos")`.

**Filtro (servlet `Filter`)**
Un trozo de código que se ejecuta **antes** de que la petición llegue a su destino. Puede
inspeccionarla, modificarla o cortarla en seco. Los filtros se ordenan con `@Order`: el número más
bajo va primero.

```text
Cliente → [CorsFilter] → [RequestIdFilter] → [PaseCompraFilter] → enrutamiento → microservicio
```

**`X-Request-Id` y por qué importa**
Una sola compra pasa por ms-cola, ms-ordenes, ms-inventario, ms-pagos y ms-tickets. Si algo falla,
hay que poder seguir **esa compra concreta** por los logs de los cinco. Con este identificador
basta buscar el mismo valor en todos.

Corresponde a la **medida correctiva 2 de Indecopi** (corregir oportunamente los errores del
portal): no se puede corregir lo que no se puede seguir.

Si el cliente envía su propio `X-Request-Id`, se respeta; si no, el gateway genera un UUID.

**CORS**
Por seguridad, el navegador bloquea las llamadas de una página a un servidor en otra dirección,
salvo que ese servidor diga «acepto llamadas desde ahí». Esto es ese permiso.

Hoy no se usa, porque todavía no hay front y `curl` o Postman **no** aplican CORS: es una regla del
navegador, no del protocolo. Se deja listo porque cuando llegue el front, el clásico error de CORS
cuesta horas de depurar.

---

## 4. Qué puedo hacer ahora

- ✅ Llamar a cualquier servicio público desde **un solo puerto**: `http://localhost:8090`
- ✅ Comprobar que `ms-inventario`, `ms-pagos` y `ms-notificaciones` **no se alcanzan** desde fuera
- ✅ Seguir una petición por los logs con su `X-Request-Id`
- ✅ Ver cómo se rechaza una compra sin pase, con un mensaje entendible

---

## 5. Cómo lo compruebo

Levanta todo primero:

```powershell
cd infra ; docker compose up -d ; cd ..
.\arrancar-todo.ps1
.\verificar-servicios.ps1
```

### Prueba 1 — El gateway enruta

```powershell
curl.exe -s http://localhost:8090/api/eventos/ping
```
→ `{"estado":"ok","servicio":"ms-eventos","puerto":8082,...}`

**Fíjate en lo que acaba de pasar:** pediste al puerto **8090** y te respondió el servicio del
puerto **8082**. Eso es enrutamiento.

Las cinco rutas de una pasada:

```powershell
"usuarios","eventos","cola","ordenes","tickets" | ForEach-Object {
    Write-Host "/api/$_ -> " -NoNewline
    curl.exe -s "http://localhost:8090/api/$_/ping"
    Write-Host ""
}
```

### Prueba 2 — Los servicios internos NO son alcanzables

```powershell
"inventario","pagos","notificaciones" | ForEach-Object {
    Write-Host "$_ -> " -NoNewline
    curl.exe -s -o NUL -w "%{http_code}`n" "http://localhost:8090/api/$_/ping"
}
```
→ `404` en los tres. ✅ **Esto es un éxito, no un error.**

Y compruébalo: esos mismos servicios **sí** responden por su puerto directo.

```powershell
curl.exe -s http://localhost:8084/ping
```
→ `{"estado":"ok","servicio":"ms-inventario","puerto":8084,...}`

### Prueba 3 — La cabecera de rastreo

```powershell
curl.exe -s -D - -o NUL http://localhost:8090/api/eventos/ping | Select-String "X-Request-Id"
```
→ `X-Request-Id: 41c96385-dfe2-438a-9eb8-d09ba3176848`

Ejecútalo dos veces: el valor cambia, porque se genera uno por petición. Y si envías el tuyo, lo
respeta:

```powershell
curl.exe -s -D - -o NUL -H "X-Request-Id: mi-trazador-12345" http://localhost:8090/api/eventos/ping | Select-String "X-Request-Id"
```
→ `X-Request-Id: mi-trazador-12345`

### Prueba 4 — El filtro del pase de compra (PRB-03)

```powershell
curl.exe -s -X POST http://localhost:8090/api/ordenes -H "Content-Type: application/json" -d "{}"
```
→ **`400`** con este cuerpo:

```json
{
  "error": "PASE_REQUERIDO",
  "mensaje": "Para comprar necesitas un pase de compra valido. Entra a la sala de espera y aguarda tu turno.",
  "detalle": "Falta la cabecera X-Pase-Compra",
  "requestId": "c7f8e030-f01b-4337-ba97-1c5ebb06532a"
}
```

Con la cabecera puesta, pasa el filtro:

```powershell
curl.exe -s -o NUL -w "%{http_code}" -X POST http://localhost:8090/api/ordenes -H "Content-Type: application/json" -H "X-Pase-Compra: cualquier-cosa" -d "{}"
```
→ `404`, porque `ms-ordenes` todavía no tiene ese endpoint (llega en la Fase 8). **El 404 confirma
que el filtro lo dejó pasar.**

Y pagar **no** pide pase, como debe ser:

```powershell
curl.exe -s -o NUL -w "%{http_code}" -X POST http://localhost:8090/api/ordenes/abc/pagar -H "Content-Type: application/json" -d "{}"
```
→ `404`, no `400`.

### Resultado real de esta implementación

| Verificación | Resultado |
|---|---|
| El gateway compila con el starter | ✅ `BUILD SUCCESS` |
| Las 5 rutas públicas enrutan | ✅ Cada una respondió con su servicio y puerto correctos |
| Las 3 rutas internas por el gateway | ✅ `404` en las tres |
| Los 3 servicios internos por puerto directo | ✅ Responden normalmente |
| `X-Request-Id` generado | ✅ Un UUID distinto por petición |
| `X-Request-Id` enviado por el cliente | ✅ Se respeta el valor |
| `POST /api/ordenes` sin pase | ✅ `400 PASE_REQUERIDO` con mensaje entendible |
| `POST /api/ordenes` con pase | ✅ Pasa el filtro (`404` de ms-ordenes) |
| `POST /api/ordenes/{id}/pagar` sin pase | ✅ No se bloquea |
| `/internal/**` por el gateway | ✅ `404`, verificado con un evento que **sí** existe |
| No regresión | ✅ `9/9 servicios vivos`, infraestructura intacta |

---

## 6. Qué sigue sin funcionar

| No funciona | Llega en |
|---|---|
| El filtro del pase solo comprueba que la cabecera **exista**, no que el pase sea válido | Fase 8 (ms-ordenes lo valida contra ms-cola por Feign) |
| `/api/usuarios`, `/api/cola`, `/api/ordenes`, `/api/tickets` enrutan, pero esos servicios aún no tienen endpoints reales | Fases 4, 7, 8, 10 |
| Si un servicio destino está caído, el gateway devuelve `500` sin un mensaje amable | Más adelante, con Circuit Breaker (sección 11 del diseño) |
| El gateway usa direcciones fijas (`localhost:8082`) | Cuando llegue Eureka: pasarán a ser `lb://ms-eventos` |
| CORS está configurado pero no probado de verdad | Fase 18, cuando exista el front |

---

## 7. Decisiones tomadas y problemas encontrados

### Problema 1 — Mi prueba documentada estaba mal, y el 404 era correcto

Al probar por primera vez, las cinco rutas devolvían `404`. Parecía que el enrutamiento no
funcionaba.

El 404 era **ambiguo**: podía venir del gateway (no enruta) o de `ms-eventos` (sí enruta, pero la
dirección no existe allí). Las dos cosas devuelven el mismo cuerpo de error de Spring Boot.

**La prueba decisiva:** apagar `ms-eventos` y ver si el error cambiaba.

| Estado de ms-eventos | Respuesta del gateway |
|---|---|
| Encendido | `404` |
| **Apagado** | **`500`** |

Si el gateway no enrutara, el error no habría cambiado al apagar el destino. Así quedó demostrado
que **sí enrutaba desde el principio**.

**La causa real:** el gateway reenvía la **ruta completa**, así que `/api/eventos/ping` llegaba a
`ms-eventos` como `/api/eventos/ping`, y el `/ping` de la Fase 0 está en la **raíz**. El 404 lo
daba `ms-eventos`, con razón.

**La solución:** exponer el ping en las dos direcciones en los 5 servicios ruteados.

```java
@GetMapping({ "/ping", "/api/eventos/ping" })
```

Así `verificar-servicios.ps1` sigue funcionando por puerto directo y la prueba del gateway tiene
sentido. Las referencias a esa prueba en los documentos de las fases 0 y 2 quedaron corregidas.

> **La lección:** un `404` no prueba que el enrutamiento falle. Hay que distinguir *quién* lo
> devuelve, y para eso lo más rápido es apagar el destino y ver si el error cambia.

### Decisión 1 — Rutas en `application.properties`, no en Java

Spring Cloud Gateway permite definir las rutas en código con `RouterFunction`. Se eligió el archivo
de propiedades porque se lee como la tabla de rutas del documento de arquitectura, y porque cambiar
un puerto no obliga a recompilar.

El prefijo correcto en esta versión es **`spring.cloud.gateway.server.webmvc.routes`**. Cambió
entre versiones de Spring Cloud, así que antes de escribirlo se verificó en el
`spring-configuration-metadata.json` del propio jar 5.0.3, en lugar de confiar en un tutorial.

### Decisión 2 — La URL de cada destino sale de una variable de entorno

```properties
spring.cloud.gateway.server.webmvc.routes[1].uri=${MS_EVENTOS_URL:http://localhost:8082}
```

Mismo criterio que DEC-01 para la base de datos: el valor por defecto sirve para desarrollo, y en la
Fase 15 el `docker-compose.yml` pasará `MS_EVENTOS_URL=http://ms-eventos:8082` sin tocar el archivo.

### Decisión 3 — El filtro del pase solo mira `POST /api/ordenes` exacto

El diseño dice «rechaza cualquier `POST /api/ordenes` sin la cabecera». Tomado literalmente con
`startsWith`, también bloquearía `POST /api/ordenes/{id}/pagar`, y pagar **no** necesita pase: el
pase se consumió al crear la orden.

Por eso el filtro compara la ruta **exacta** (con o sin barra final). Queda probado explícitamente
en la Prueba 4.

### Decisión 4 — CORS como `FilterRegistrationBean` con la prioridad más alta

Se registró con `Ordered.HIGHEST_PRECEDENCE` para que las peticiones `OPTIONS` de verificación
previa que manda el navegador se respondan **antes** de pasar por el filtro del pase. Si no, el
navegador recibiría un `400` en la verificación previa y nunca llegaría a enviar la compra.

### Nota — El `/ping` es temporal

Sigue siendo un andamio de la Fase 0. Se puede borrar en la Fase 16, cuando exista la colección de
Postman. Mientras tanto es la forma más rápida de saber a qué servicio le estás pegando.

---

## 8. Relación con el informe

| Requisito del `.docx` | Cómo lo cumple esta fase |
|---|---|
| **Producto N.º 3** — «API Gateway: enrutamiento, cabecera X-Request-Id, filtro del pase de compra y CORS» | ✅ Las cuatro cosas, implementadas y probadas |
| **Sección 6.3.3** — «Enruta cada ruta `/api/<servicio>/**` al microservicio correspondiente» | ✅ Las 5 rutas de la tabla 4.2 del diseño |
| **Sección 6.3.3** — «agrega la cabecera X-Request-Id para seguir una compra por todos los servicios» | ✅ `RequestIdFilter`, verificado de punta a punta |
| **Sección 6.3.3** — «rechaza toda orden que llegue sin el pase de compra» | ✅ `PaseCompraFilter` → `400 PASE_REQUERIDO` |
| **Sección 6.3.3** — «Los endpoints `/internal/**` no tienen ruta pública, de modo que nadie puede saltarse la cola llamando directamente a ms-inventario o a ms-pagos» | ✅ Verificado con `404` usando un evento real |
| **PRB-03** Saltarse la cola | Primera de las dos barreras. La segunda, en la Fase 8 |
| **Medida correctiva 2 de Indecopi** — corregir errores del portal | Trazabilidad con `X-Request-Id` |
| **Sección 11 del diseño** — preparado para Eureka | Las URL salen de variables; pasarán a `lb://ms-eventos` sin tocar lógica |

---

## Siguiente paso

**[Fase 3 — ms-eventos: catálogo de eventos y zonas](FASE-03-ms-eventos.md)**: el primer
microservicio de verdad, con su base de datos y su CRUD.
