# Estado del proyecto Teleticket — punto de partida

> ⚠️ **Este documento es la foto del proyecto ANTES de empezar a implementar.** Se conserva tal
> cual como línea base, para poder comparar y para la sustentación. **Ya no describe el estado de
> hoy.**
>
> **La Fase 0 está implementada** (21/10/2026): los 9 servicios ya arrancan, se quedan encendidos
> y responden. Lo que cambió está en
> [docs/fases/FASE-00-cimientos.md](docs/fases/FASE-00-cimientos.md).
>
> Para saber en qué punto va el proyecto, mira el mapa de fases de
> [PLAN-DE-IMPLEMENTACION.md](PLAN-DE-IMPLEMENTACION.md) y los documentos de
> [docs/fases/](docs/fases/).

**Fecha de revisión:** 21 de octubre de 2026
**Rama:** `main` · **Último commit:** `6ebff3e` — *Creación de microservicios incluyendo api-gateway*

Este documento describe **exactamente lo que existe hoy** en el workspace, microservicio por
microservicio, y **qué se puede hacer** con el proyecto en este momento. Está escrito para
alguien que recién está aprendiendo Spring Boot y microservicios, así que cada término técnico
se explica la primera vez que aparece.

El plan para llevarlo desde aquí hasta cumplir toda la documentación está en
[PLAN-DE-IMPLEMENTACION.md](PLAN-DE-IMPLEMENTACION.md).

---

## 1. Resumen en una frase

> Existen **9 proyectos Spring Boot creados y vacíos**. Ninguno tiene código de negocio, ni base
> de datos, ni puerto asignado, ni capacidad de atender peticiones HTTP. Son **esqueletos
> recién generados** desde Spring Initializr. **Hoy el proyecto no hace nada todavía** más allá
> de compilar y arrancar por un segundo.

Lo que sí está muy avanzado es **el diseño**: los tres documentos de análisis y arquitectura
(`.docx` y los dos `.md`) ya definen el 100 % de lo que hay que construir. El trabajo pendiente
es **todo el código**.

---

## 2. Qué hay en el workspace

```text
eclipse-workspace/
├── .git/                       ← repositorio Git (2 commits)
├── .gitignore                  ← solo ignora /.metadata/
│                                 (cada ms-* trae además su propio .gitignore
│                                  de Spring Initializr, que cubre target/)
├── .metadata/                  ← configuración interna de Eclipse (no es código)
│
├── ms-api-gateway/             ← 9 proyectos Spring Boot
├── ms-usuarios/                   TODOS vacíos
├── ms-eventos/
├── ms-cola/
├── ms-inventario/
├── ms-ordenes/
├── ms-pagos/
├── ms-teletickets/
├── ms-notificaciones/
│
├── CircuitBreak/   + Circuit Break.zip   ← ejemplos del curso (NO son del proyecto)
├── Feign/          + Feign.zip           ← ejemplos del curso (NO son del proyecto)
└── RabbitMQ/       + RabbitMQ2.zip       ← ejemplos del curso (NO son del proyecto)
```

Y un nivel más arriba, en la carpeta `Proyectos/`:

| Archivo                                             | Qué es                                                                                             |
| --------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| `Proyecto_Investigacion_Teleticket_reducido.docx` | El informe del proyecto de investigación. Es **la fuente de requerimientos**                 |
| `Teleticket_Presentacion_final.pptx`              | Presentación                                                                                       |
| `analisis-negocio-teleticket.md`                  | Análisis de negocio: problemas, actores, 17 casos de uso, 14 reglas de negocio (RN-01…RN-14)      |
| `arquitectura-microservicios-teleticket.md`       | **Guía técnica de construcción**: puertos, tablas, endpoints, mensajes de RabbitMQ, flujos |

> **Importante:** estos dos `.md` son el plano de la obra. El plan de implementación los sigue
> al pie de la letra, no inventa una arquitectura nueva.

---

## 3. Qué tienen los 9 microservicios (son todos idénticos)

Los nueve proyectos se generaron con la misma plantilla y **nadie los ha tocado desde
entonces**. Cada uno contiene exactamente esto:

```text
ms-xxxxx/
├── .mvn/wrapper/maven-wrapper.properties   ← Maven 3.9.16 embebido
├── mvnw  /  mvnw.cmd                       ← lanzadores de Maven (Linux / Windows)
├── pom.xml                                 ← dependencias del proyecto
├── HELP.md                                 ← archivo de ayuda de Spring Initializr
├── src/main/java/com/teleticket/xxxxx/MsXxxxxApplication.java
├── src/main/resources/application.properties
├── src/test/java/.../MsXxxxxApplicationTests.java
└── target/                                 ← resultado de compilar (no debería versionarse)
```

### 3.1 La única clase Java que existe

En cada servicio hay **una sola clase**, y es la clase de arranque generada automáticamente:

```java
package com.teleticket.eventos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MsEventosApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsEventosApplication.class, args);
    }
}
```

**Qué significa esto:** `@SpringBootApplication` le dice a Spring «arranca una aplicación y
busca componentes en este paquete». Como no hay ningún componente, Spring arranca, no encuentra
nada que hacer y **se apaga de inmediato**.

### 3.2 El único archivo de configuración

```properties
spring.application.name=ms-eventos
```

Una sola línea: el nombre. **No hay puerto configurado**, no hay conexión a base de datos, no
hay conexión a RabbitMQ.

### 3.3 Las dependencias de cada `pom.xml`

El `pom.xml` es la lista de «librerías que mi proyecto necesita». Hoy, los nueve tienen la misma
lista mínima:

| Dependencia                                    | Para qué sirve                                                  | ¿Alcanza?                            |
| ---------------------------------------------- | ---------------------------------------------------------------- | ------------------------------------- |
| `spring-boot-starter-parent` **4.1.1** | Versión base de Spring Boot                                     | Sí                                   |
| `spring-boot-starter`                        | Núcleo de Spring Boot, **sin servidor web**               | ❌ No permite atender peticiones HTTP |
| `lombok`                                     | Genera *getters*, *setters* y constructores automáticamente | Sí, útil                            |
| `spring-boot-starter-test`                   | Para pruebas automáticas                                        | Sí                                   |

**Java:** 21 · **Maven:** 3.9.16 (wrapper) · **Grupo:** `com.teleticket`

**Lo que falta en todos** (y que el diseño exige):

- `spring-boot-starter-webmvc` → sin esto **no se puede exponer ni un solo endpoint REST**
- `spring-boot-starter-data-jpa` + `postgresql` → sin esto no hay base de datos
- `spring-boot-starter-amqp` → sin esto no hay RabbitMQ
- `spring-cloud-starter-openfeign` → sin esto no hay llamadas entre microservicios
- `spring-cloud-starter-gateway-server-webmvc` → sin esto el gateway no enruta nada
- `spring-boot-starter-actuator` → sin esto no hay `/actuator/health` para saber si está vivo
- `spring-boot-starter-mail` → sin esto `ms-notificaciones` no envía correos

---

## 4. Detalle servicio por servicio

En las tablas siguientes, **«Diseñado»** es lo que piden los documentos y **«Implementado»** es
lo que existe hoy en el código.

---

### 4.1 `ms-api-gateway` — la puerta de entrada

|                                  |                               |
| -------------------------------- | ----------------------------- |
| **Paquete Java**           | `com.teleticket.apigateway` |
| **Puerto diseñado**       | 8080 — *en la Fase 1 se movió al **8090**: ver [FASE-01](docs/fases/FASE-01-infraestructura.md)* |
| **Puerto configurado hoy** | ninguno                       |
| **Base de datos**          | no lleva (no guarda nada)     |

**Para qué sirve (en palabras simples):** es el **único teléfono público** del sistema. El
navegador y la app móvil solo conocen `http://localhost:8090`. El gateway recibe cada petición y
la reenvía al microservicio que corresponde. Así, el cliente no necesita saber que existen 8
servicios distintos en 8 puertos distintos.

| Diseñado                                                                                                            | Implementado |
| -------------------------------------------------------------------------------------------------------------------- | ------------ |
| Enrutar `/api/usuarios/**` → ms-usuarios, `/api/eventos/**` → ms-eventos, etc.                                 | ❌           |
| Agregar la cabecera `X-Request-Id` a cada petición (para rastrear una compra por todos los servicios en los logs) | ❌           |
| Filtro que rechaza con `400` cualquier `POST /api/ordenes` sin la cabecera `X-Pase-Compra`                     | ❌           |
| **No** exponer las rutas `/internal/**` (así nadie puede saltarse la cola llamando directo a inventario)    | ❌           |
| CORS para el cliente web                                                                                             | ❌           |

**Estado:** esqueleto vacío. Ni siquiera tiene la dependencia de Spring Cloud Gateway.

---

### 4.2 `ms-usuarios` — registro y roles

|                            |                                                 |
| -------------------------- | ----------------------------------------------- |
| **Paquete Java**     | `com.teleticket.usuarios`                     |
| **Puerto diseñado** | 8081 · **BD diseñada:** `db_usuarios` |
| **Caso de uso**      | CU-01 Registrarse / iniciar sesión             |

**Para qué sirve:** guarda a los compradores y al personal (operador de ventas, controlador de
acceso, atención al cliente) con su rol. Es el servicio más simple de los ocho.

| Diseñado                                                                                          | Implementado |
| -------------------------------------------------------------------------------------------------- | ------------ |
| Tabla `usuario` (id, dni, nombres, email, password_hash, rol) con `dni` y `email` únicos    | ❌           |
| `POST /api/usuarios` (registro) · `GET /api/usuarios/me`                                      | ❌           |
| `GET /internal/usuarios/{id}/contacto` (lo usa ms-notificaciones para saber a quién escribirle) | ❌           |

**Estado:** esqueleto vacío.

---

### 4.3 `ms-eventos` — catálogo y reglas de la venta

|                               |                                                                                       |
| ----------------------------- | ------------------------------------------------------------------------------------- |
| **Paquete Java**        | `com.teleticket.eventos`                                                            |
| **Puerto diseñado**    | 8082 · **BD diseñada:** `db_eventos`                                        |
| **Casos de uso**        | CU-02 Consultar eventos · CU-14 Configurar evento · CU-15 Publicar reglas y horario |
| **Problemas que ataca** | PRB-02 (cola injusta), PRB-06 (información contradictoria)                           |

**Para qué sirve:** es el catálogo. Guarda los eventos, sus zonas con precio y aforo, las
preventas bancarias y —lo más importante para el proyecto— **la hora de inicio de la venta y las
reglas de la cola**, que una vez publicadas **ya no se pueden cambiar** (regla RN-01). Eso ataca
directamente lo que pasó en 2017, cuando la cola se anunció para las 6:00 a. m. y el sistema
reordenó a todos desde las 5:00.

| Diseñado                                                                                                                                            | Implementado |
| ---------------------------------------------------------------------------------------------------------------------------------------------------- | ------------ |
| Tablas `evento`, `zona`, `preventa`                                                                                                            | ❌           |
| `GET /api/eventos` · `GET /api/eventos/{id}` · `POST /api/eventos` · `POST /api/eventos/{id}/zonas` · `PUT /api/eventos/{id}/publicar` | ❌           |
| `GET /internal/eventos/{id}/zonas/{zonaId}` (precio y máximo por cliente, lo pide ms-ordenes)                                                     | ❌           |
| `GET /internal/eventos/{id}/condiciones` (lo pide ms-tickets para imprimirlas en la entrada)                                                       | ❌           |
| Publicar el mensaje `evento.publicado` en RabbitMQ al publicar el evento                                                                           | ❌           |

**Estado:** esqueleto vacío. **Es el primer servicio que hay que construir**, porque todo lo
demás depende de que exista un evento publicado.

---

### 4.4 `ms-cola` — la sala de espera y el sorteo

|                               |                                                                    |
| ----------------------------- | ------------------------------------------------------------------ |
| **Paquete Java**        | `com.teleticket.cola`                                            |
| **Puerto diseñado**    | 8083 · **BD diseñada:** `db_cola`                        |
| **Casos de uso**        | CU-03 Ingresar a la sala de espera · CU-04 Obtener turno          |
| **Problemas que ataca** | PRB-01 (colapso), PRB-02 (cola injusta), PRB-03 (saltarse la cola) |

**Para qué sirve:** es **el corazón de la solución al problema de los picos de demanda**.
Absorbe la avalancha de miles de personas que entran a la misma hora y solo deja pasar a la
compra a unos pocos por ciclo. Funciona así:

1. Antes de la hora de venta, el comprador entra a la **sala de espera** y se le crea un *turno*.
2. A la hora exacta anunciada se hace **un único sorteo** que asigna una posición aleatoria a
   todos los que están en la sala. Quien llega después se pone al final.
3. **Cada 10 segundos** el sistema habilita a los siguientes N compradores y a cada uno le
   entrega un **pase de compra**: un código personal, de un solo uso, que vence en 10 minutos.
4. **Sin pase no hay compra.** Eso elimina los «enlaces para saltarse la cola» de 2017.

| Diseñado                                                                            | Implementado |
| ------------------------------------------------------------------------------------ | ------------ |
| Tablas `venta_evento`, `turno` (un turno por persona y evento), `pase_compra`  | ❌           |
| `POST /api/cola/eventos/{eventoId}/ingresar` · `GET /api/cola/turnos/{turnoId}` | ❌           |
| `POST /internal/pases/{codigo}/consumir` (lo llama ms-ordenes por Feign)           | ❌           |
| Tarea programada del **sorteo** a la hora de inicio                            | ❌           |
| Tarea programada de **admisión cada 10 s** que crea los pases                 | ❌           |
| Consumir `evento.publicado` y publicar `turno.habilitado`                        | ❌           |

**Estado:** esqueleto vacío.

---

### 4.5 `ms-inventario` — stock y reservas temporales

|                            |                                                                                      |
| -------------------------- | ------------------------------------------------------------------------------------ |
| **Paquete Java**     | `com.teleticket.inventario`                                                        |
| **Puerto diseñado** | 8084 · **BD diseñada:** `db_inventario`                                    |
| **Caso de uso**      | CU-05 Reservar entradas                                                              |
| **Acceso**           | 🔒 **Interno.** No tiene ruta en el gateway: nadie lo puede llamar desde fuera |

**Para qué sirve:** lleva la cuenta de cuántas entradas quedan en cada zona y las **reserva por
10 minutos** mientras el comprador paga. Si no paga, la reserva se libera sola y el stock vuelve
a estar disponible. El truco clave para que nunca se venda de más es una sola instrucción SQL:

```sql
UPDATE stock_zona SET disponible = disponible - :cantidad
WHERE zona_id = :zonaId AND disponible >= :cantidad;
-- 1 fila afectada = reservado. 0 filas = no había stock.
```

Como PostgreSQL ejecuta eso como una operación atómica, aunque lleguen mil peticiones a la vez
es imposible que dos personas se lleven la misma entrada.

| Diseñado                                                                                                                                                        | Implementado |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------ |
| Tablas `stock_zona` (con `CHECK disponible >= 0`), `reserva`                                                                                               | ❌           |
| `POST /internal/reservas` · `PUT /internal/reservas/{id}/en-pago` · `PUT /internal/reservas/{id}/activar` · `GET /internal/zonas/{zonaId}/disponible` | ❌           |
| Consumir `evento.publicado` (crear stock), `reserva.vencida`, `orden.pagada`, `orden.reembolsada`                                                        | ❌           |
| Cola con TTL de 10 minutos + DLX para liberar reservas vencidas                                                                                                  | ❌           |

**Estado:** esqueleto vacío.

---

### 4.6 `ms-ordenes` — el coordinador de la compra

|                               |                                                       |
| ----------------------------- | ----------------------------------------------------- |
| **Paquete Java**        | `com.teleticket.ordenes`                            |
| **Puerto diseñado**    | 8085 · **BD diseñada:** `db_ordenes`        |
| **Casos de uso**        | CU-05 Reservar · CU-06 Pagar                         |
| **Problemas que ataca** | PRB-03 (saltarse la cola), PRB-04 (cobro sin entrada) |

**Para qué sirve:** es **el director de orquesta**. Es el único servicio que habla con casi
todos los demás. Cuando el comprador crea una orden, `ms-ordenes`:

1. Llama a `ms-cola` para **consumir el pase** (si el pase no es válido, no hay orden).
2. Llama a `ms-eventos` para saber el **precio** y el **máximo por cliente**.
3. Llama a `ms-inventario` para **reservar el stock**.
4. Cuando el comprador paga, llama a `ms-pagos`.

Y sobre todo: **coordina la saga**. «Saga» suena complicado pero es simple: si el pago se aprueba
pero la emisión de las entradas falla, `ms-ordenes` **pide el reembolso automáticamente**. Un
pago aprobado siempre termina en `EMITIDA` o en `REEMBOLSADA`, nunca en un limbo. Eso es
exactamente lo que falló en 2017 («la web cobra pero no emite»).

| Diseñado                                                                                                                       | Implementado |
| ------------------------------------------------------------------------------------------------------------------------------- | ------------ |
| Tablas `orden` (con `UNIQUE(pase_codigo)`: un pase = una orden) y `orden_historial` (auditoría de cada cambio de estado) | ❌           |
| `POST /api/ordenes` · `POST /api/ordenes/{id}/pagar` · `GET /api/ordenes/{id}` · `GET /api/ordenes/mias`             | ❌           |
| 4 clientes Feign: `ColaClient`, `EventoClient`, `InventarioClient`, `PagoClient`                                        | ❌           |
| Publicar `orden.pagada`, `orden.expirada`, `reembolso.solicitado`, `orden.reembolsada`                                  | ❌           |
| Consumir `tickets.emitidos`, `emision.fallida`, `pago.reembolsado`, `reserva.liberada`                                  | ❌           |
| Máquina de estados: RESERVADA → PENDIENTE_PAGO → PAGADA → EMITIDA / REEMBOLSADA / EXPIRADA                                  | ❌           |

**Estado:** esqueleto vacío. **Es el servicio más complejo del proyecto.**

---

### 4.7 `ms-pagos` — pasarela simulada

|                            |                                              |
| -------------------------- | -------------------------------------------- |
| **Paquete Java**     | `com.teleticket.pagos`                     |
| **Puerto diseñado** | 8086 · **BD diseñada:** `db_pagos` |
| **Caso de uso**      | CU-06 Pagar la orden (el cobro)              |
| **Acceso**           | 🔒 **Interno**                         |

**Para qué sirve:** simula un banco. El alcance del proyecto dice explícitamente que **no hay
integración real con pasarelas**. La simulación acordada en el diseño es:

- Tarjeta que termina en `0000` → **rechazada**
- Tarjeta que termina en `9999` → **tarda 5 segundos** (sirve para probar los *timeouts*)
- Cualquier otra → **aprobada**

Lo importante técnicamente es que el cobro sea **idempotente**: si `ms-ordenes` reintenta la
misma petición, `ms-pagos` devuelve el mismo pago y **no cobra dos veces**. Se consigue con una
columna `idempotency_key` con restricción `UNIQUE`.

| Diseñado                                                         | Implementado |
| ----------------------------------------------------------------- | ------------ |
| Tablas `pago` (con `UNIQUE(idempotency_key)`) y `reembolso` | ❌           |
| `POST /internal/pagos` idempotente por `ordenId`              | ❌           |
| Consumir `reembolso.solicitado` y publicar `pago.reembolsado` | ❌           |

**Estado:** esqueleto vacío.

---

### 4.8 `ms-teletickets` — emisión, nominación y validación

|                               |                                                                                 |
| ----------------------------- | ------------------------------------------------------------------------------- |
| **Paquete Java**        | `com.teleticket.teletickets`                                                  |
| **Puerto diseñado**    | 8087 · **BD diseñada:** `db_tickets`                                  |
| **Casos de uso**        | CU-07 Emitir · CU-08 Nominar · CU-09 Descargar · CU-11 Validar en el ingreso |
| **Problemas que ataca** | PRB-04, PRB-05 (entradas falsas), PRB-06                                        |

> ⚠️ **Nota de nomenclatura:** en los documentos de diseño este servicio se llama **`ms-tickets`**,
> pero la carpeta creada se llama **`ms-teletickets`**. Es el mismo servicio. El plan de
> implementación propone mantener el nombre de la carpeta (para no romper Git ni el workspace de
> Eclipse) y dejar la ruta pública como `/api/tickets/**`, tal como dice el diseño.

**Para qué sirve:** fabrica las entradas digitales y las protege contra falsificación. Cada
e-ticket lleva:

- un **código** (UUID),
- una **firma HMAC-SHA256** calculada con una clave secreta: `firma = HMAC(codigo + eventoId, clave)`.

Una entrada falsificada **no tiene una firma válida**, así que se rechaza antes incluso de
consultar la base de datos. Eso ataca el caso de Daddy Yankee 2022 (más de 7 000 personas con
entradas con QR falsificados).

Además, la entrada se **nomina** con el DNI del asistente y solo **se puede validar una vez** en
la puerta. Los intentos rechazados también se guardan.

| Diseñado                                                                                       | Implementado |
| ----------------------------------------------------------------------------------------------- | ------------ |
| Tablas `ticket` (`UNIQUE(codigo)`, `UNIQUE(orden_id, numero)`) y `validacion`           | ❌           |
| `GET /api/tickets/mios` · `PUT /api/tickets/{id}/nominar` · `POST /api/tickets/validar` | ❌           |
| Consumir `orden.pagada` y emitir **con 3 reintentos** (1 s, 2 s, 4 s)                   | ❌           |
| Si falla las 3 veces → el mensaje cae en la **DLQ** y dispara el reembolso automático   | ❌           |
| Publicar `tickets.emitidos`, `emision.fallida`, `ticket.validado`                         | ❌           |
| `EventoClient` (Feign) para copiar las condiciones de ingreso en la entrada                   | ❌           |

**Estado:** esqueleto vacío.

---

### 4.9 `ms-notificaciones` — correos al comprador

|                            |                                                       |
| -------------------------- | ----------------------------------------------------- |
| **Paquete Java**     | `com.teleticket.notificaciones`                     |
| **Puerto diseñado** | 8088 · **BD diseñada:** `db_notificaciones` |
| **Caso de uso**      | CU-10 Recibir notificaciones                          |
| **Acceso**           | 🔒 **Interno**                                  |

**Para qué sirve:** avisa al comprador por correo en **cada cambio de estado**: llegó tu turno,
aquí están tus e-tickets, tu reserva venció, te devolvimos el dinero. Es asíncrono a propósito:
si el correo tarda o falla, **la compra no se ve afectada**.

En desarrollo no se envían correos de verdad: se usa **MailHog**, un programa que los captura y
los muestra en una página web en `http://localhost:8025`.

| Diseñado                                                                                        | Implementado |
| ------------------------------------------------------------------------------------------------ | ------------ |
| Tabla `notificacion` con `UNIQUE(mensaje_origen_id, tipo)` para no enviar dos veces lo mismo | ❌           |
| Consumir `turno.habilitado`, `tickets.emitidos`, `orden.expirada`, `orden.reembolsada`   | ❌           |
| `UsuarioClient` (Feign) para obtener nombre y correo                                           | ❌           |
| Envío con `JavaMailSender` contra MailHog                                                     | ❌           |

**Estado:** esqueleto vacío.

---

### 4.10 `ms-reclamos` — libro de reclamaciones *(opcional)*

|                            |                                                                                 |
| -------------------------- | ------------------------------------------------------------------------------- |
| **Estado**           | ❌ **El proyecto ni siquiera existe como carpeta**                        |
| **Puerto diseñado** | 8089 · **BD diseñada:** `db_reclamos`                                 |
| **Casos de uso**     | CU-12 Solicitar reclamo · CU-13 Gestionar reclamos (ambos prioridad *Could*) |

Es opcional según el propio documento y queda para el final.

---

## 5. Qué NO existe todavía (y el diseño lo pide)

| Componente                     | Para qué                                                                                                                  | Dónde debería ir         |
| ------------------------------ | -------------------------------------------------------------------------------------------------------------------------- | -------------------------- |
| `libs/mensajeria-comun`      | Módulo Java compartido con los objetos de los mensajes y la interfaz `PublicadorEventos`. Entregable N.º 5 del informe | carpeta nueva `libs/`    |
| `infra/docker-compose.yml`   | Levantar PostgreSQL, RabbitMQ y MailHog con un solo comando. Entregable N.º 6                                             | carpeta nueva `infra/`   |
| `Dockerfile` (×9)           | Empaquetar cada microservicio como contenedor                                                                              | uno por servicio           |
| Las 8 bases de datos           | `db_usuarios` … `db_notificaciones`                                                                                   | PostgreSQL                 |
| Topología de RabbitMQ         | `teleticket.exchange`, `teleticket.dlx`, 12 colas                                                                      | se crea desde el código   |
| Colección Postman             | Entregable N.º 7: peticiones de ejemplo para cada ruta pública                                                           | carpeta nueva `pruebas/` |
| Scripts de carga (k6 o JMeter) | Entregable N.º 7: 1 000 usuarios simulados para medir el OBJ 3                                                            | carpeta nueva `pruebas/` |
| Front-end                      | El informe lo menciona como «cliente web»                                                                                | queda para el final        |

---

## 6. Qué se puede hacer hoy con el proyecto

Siendo completamente honestos: **muy poco**. Esto es lo único que funciona:

| Acción                                         | ¿Funciona?   | Qué pasa exactamente                                                                                                                                              |
| ----------------------------------------------- | ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Abrir los 9 proyectos en Eclipse                | ✅ Sí        | Son proyectos Maven válidos                                                                                                                                       |
| Compilar un servicio (`mvnw clean package`)   | ✅ Sí        | Genera un `.jar` en `target/`                                                                                                                                  |
| Arrancar un servicio (`mvnw spring-boot:run`) | ⚠️ A medias | Spring arranca, imprime el banner… y **se apaga en ~1 segundo**, porque sin `spring-boot-starter-webmvc` no hay servidor web que mantenga el proceso vivo |
| Llamar a cualquier endpoint REST                | ❌ No         | No existe ni un solo `@RestController` en todo el proyecto                                                                                                       |
| Guardar o leer datos                            | ❌ No         | No hay entidades, ni repositorios, ni conexión a base de datos                                                                                                    |
| Enviar o recibir mensajes de RabbitMQ           | ❌ No         | No está la dependencia `spring-boot-starter-amqp`                                                                                                               |
| Que un servicio llame a otro                    | ❌ No         | No está la dependencia de Feign                                                                                                                                   |
| Que el gateway enrute algo                      | ❌ No         | No está la dependencia de Spring Cloud Gateway                                                                                                                    |
| Levantar la infraestructura con Docker          | ❌ No         | No existe el `docker-compose.yml`                                                                                                                                |

### Comprobarlo por tu cuenta

Abre PowerShell en la carpeta de cualquier servicio y ejecuta:

```powershell
cd "F:\COMPUTACION_E_INFORMATICA_CIBERTEC\6 - SEXTO CICLO\MODULO 2\DESARROLLO DE APLICACIONES WEB II\Proyectos\eclipse-workspace\ms-eventos"
.\mvnw.cmd spring-boot:run
```

Verás el banner de Spring Boot, un mensaje tipo `Started MsEventosApplication in 1.2 seconds` y
acto seguido el proceso termina y vuelves al prompt. **Eso es lo esperado hoy**, y es
exactamente lo que la Fase 0 del plan va a cambiar.

---

## 7. Herramientas disponibles en tu máquina

Buenas noticias: **ya tienes todo lo necesario instalado**. No hace falta instalar nada para
empezar.

| Herramienta                   | Versión detectada    | Para qué se usará                                         |
| ----------------------------- | --------------------- | ----------------------------------------------------------- |
| Java (Amazon Corretto)        | **21.0.11 LTS** | Ejecutar los microservicios                                 |
| Maven (wrapper del proyecto)  | **3.9.16**      | Compilar. No hace falta instalarlo aparte: usa `mvnw.cmd` |
| Docker                        | **29.8.0**      | Levantar PostgreSQL, RabbitMQ y MailHog                     |
| Docker Compose                | **v5.5.1**      | Levantar todo con un solo comando                           |
| PostgreSQL cliente (`psql`) | **16.13**       | Revisar las bases de datos a mano                           |

### Librerías ya descargadas en tu repositorio local de Maven

Tu carpeta `~/.m2/repository` **ya tiene descargadas** estas librerías (porque las usaste en los
ejemplos del curso), lo que significa que las primeras fases compilarán rápido y **funcionarán
aunque no tengas internet**:

- `spring-cloud-dependencies` **2025.0.0**, **2025.1.0** y **2025.1.3**
- `spring-cloud-starter-openfeign` 4.3.0, 5.0.0 y **5.0.3**
- `spring-cloud-starter-gateway-server-webmvc` **5.0.3**
- `spring-cloud-starter-circuitbreaker-resilience4j`
- Spring Boot 3.0.6, 3.2.0, 3.3.0, 3.5.6, 4.0.3 – 4.0.8 y **4.1.1**

---

## 8. Dos cosas que hay que decidir antes de empezar

### 8.1 La versión de Spring Cloud (importante)

Los `pom.xml` usan **Spring Boot 4.1.1**, pero **ninguno declara Spring Cloud todavía**. Y no
cualquier versión de Spring Cloud sirve con Spring Boot 4.1: si eliges mal, el arranque falla con
un error del tipo *«Spring Boot [4.1.1] is not compatible with this Spring Cloud release train»*.

| Opción                   | Combinación                                                  | Comentario                                                                                                                              |
| ------------------------- | ------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| **A (recomendada)** | Spring Boot **4.1.1** + Spring Cloud **2025.1.3** | 2025.1.2 fue la versión que introdujo compatibilidad con Boot 4.1, y la 2025.1.3 **ya está descargada en tu máquina**          |
| B                         | Spring Boot **4.0.6** + Spring Cloud **2025.1.0** | Es exactamente lo que usa el ejemplo `Feign/api` del profesor. Más conservador, pero obliga a bajar la versión de los 9 `pom.xml` |

La Fase 0 del plan empieza verificando esto con un comando concreto, antes de escribir código.

### 8.2 Los nombres de carpeta vs. los nombres del diseño

| Carpeta real       | Nombre en los documentos | Propuesta                                                                                               |
| ------------------ | ------------------------ | ------------------------------------------------------------------------------------------------------- |
| `ms-api-gateway` | `api-gateway`          | Mantener la carpeta, usar `spring.application.name=api-gateway`                                       |
| `ms-teletickets` | `ms-tickets`           | Mantener la carpeta, usar `spring.application.name=ms-tickets` y la ruta pública `/api/tickets/**` |

Así el código queda alineado con la documentación entregada **sin renombrar carpetas**, que es lo
que podría romper el workspace de Eclipse y ensuciar el historial de Git.

---

## 9. Conclusión

| Dimensión                                       | Avance                                                                   |
| ------------------------------------------------ | ------------------------------------------------------------------------ |
| **Diseño y documentación**               | 🟢 ~95 % — el `.docx` y los dos `.md` cubren todo                   |
| **Estructura de proyectos**                | 🟡 ~40 % — existen 9 de los 10 proyectos, pero sin configurar           |
| **Código de negocio**                     | 🔴 **0 %** — ni una entidad, ni un controlador, ni un repositorio |
| **Infraestructura (Docker, BD, RabbitMQ)** | 🔴 **0 %**                                                         |
| **Pruebas (Postman, carga)**               | 🔴 **0 %**                                                         |
| **Front-end**                              | 🔴 **0 %** (por decisión: va al final)                            |

El punto de partida es bueno: el diseño está cerrado y las herramientas instaladas. Lo que sigue
es construir, fase por fase, lo descrito en
[PLAN-DE-IMPLEMENTACION.md](PLAN-DE-IMPLEMENTACION.md).
