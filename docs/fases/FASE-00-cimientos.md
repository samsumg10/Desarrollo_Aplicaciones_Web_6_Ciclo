# Fase 0 — Cimientos: que los 9 servicios arranquen y respondan

**Estado:** ✅ Completada · **Fecha:** 21/10/2026
**Cubre:** ningún caso de uso todavía (es la preparación del terreno)
**Resuelve:** nada funcionalmente, pero **habilita todas las fases siguientes**

---

## 1. En una frase

> Antes, los 9 microservicios arrancaban y **se apagaban en un segundo**. Ahora los 9 se quedan
> encendidos, cada uno en su propio puerto, y puedes preguntarle a cada uno «¿estás vivo?» y que
> te conteste.

---

## 2. Qué se creó

### Archivos nuevos

| Archivo | Para qué sirve |
|---|---|
| `<cada servicio>/src/main/java/.../controller/PingController.java` (×9) | Endpoint `GET /ping` que responde qué servicio es, en qué puerto está y la hora |
| `arrancar-todo.ps1` | Abre una ventana de PowerShell por servicio. Evita abrir 9 terminales a mano |
| `verificar-servicios.ps1` | Le pega a `/ping` y `/actuator/health` de los 9 y muestra una tabla con el estado |
| `detener-todo.ps1` | Mata los procesos Java que estén escuchando en los puertos 8081–8088 y 8090 |
| `docs/fases/FASE-00-cimientos.md` | Este documento |

### Archivos modificados

| Archivo | Qué se le agregó | Por qué era seguro |
|---|---|---|
| `<cada servicio>/pom.xml` (×9) | `spring-boot-starter-webmvc`, `spring-boot-starter-actuator` y el bloque `<dependencyManagement>` con Spring Cloud 2025.1.3. Se reemplazó `spring-boot-starter` por `spring-boot-starter-webmvc`, que ya lo incluye | No había ninguna funcionalidad que romper: los proyectos estaban vacíos |
| `<cada servicio>/src/main/resources/application.properties` (×9) | Puerto, nombre del servicio, configuración de Actuator y formato de logs | Antes solo tenían una línea (el nombre) |
| `.gitignore` (raíz) | Red de seguridad para carpetas futuras (`libs/`, `pruebas/`) que no traerán su propio `.gitignore` | Solo agrega reglas de exclusión; no afecta a archivos ya versionados |

### Puertos asignados

> ⚠️ **Nota añadida en la Fase 1:** el gateway se configuró originalmente en el **8080**, como
> dice el diseño, y así funcionó en esta fase. En la Fase 1 hubo que **moverlo al 8090** porque al
> arrancar Docker Desktop se descubrió que el 8080 lo ocupa el contenedor de otro proyecto del
> curso. La tabla de abajo ya refleja el puerto definitivo. El motivo está en
> [FASE-01](FASE-01-infraestructura.md).

| Servicio (carpeta) | Puerto | `spring.application.name` |
|---|---|---|
| `ms-api-gateway` | 8090 | `api-gateway` |
| `ms-usuarios` | 8081 | `ms-usuarios` |
| `ms-eventos` | 8082 | `ms-eventos` |
| `ms-cola` | 8083 | `ms-cola` |
| `ms-inventario` | 8084 | `ms-inventario` |
| `ms-ordenes` | 8085 | `ms-ordenes` |
| `ms-pagos` | 8086 | `ms-pagos` |
| `ms-teletickets` | 8087 | **`ms-tickets`** |
| `ms-notificaciones` | 8088 | `ms-notificaciones` |

> Fíjate en `ms-teletickets`: la **carpeta** mantiene su nombre (para no romper Git ni el
> workspace de Eclipse), pero el **servicio** se llama `ms-tickets`, igual que en toda la
> documentación de diseño. Así el código y los documentos dicen lo mismo.

### Tablas de base de datos

Ninguna todavía. La primera base de datos llega en la **Fase 3**.

### Endpoints

| Método | Ruta | Público/Interno | Qué hace |
|---|---|---|---|
| `GET` | `/ping` | temporal | Dice qué servicio es, su puerto y la hora |
| `GET` | `/actuator/health` | temporal | Dice `UP` si el servicio está sano |

Los dos existen en **los 9 servicios**. Todavía no hay rutas `/api/**` ni `/internal/**`.

### Mensajes de RabbitMQ

Ninguno. La mensajería llega en la **Fase 5**.

---

## 3. Conceptos nuevos explicados

**`spring-boot-starter-webmvc`**
Un *starter* es un paquete que trae de golpe todas las librerías necesarias para una tarea. Este
en concreto trae **Tomcat** (un servidor web embebido) y **Spring MVC** (lo que permite escribir
`@RestController`). Sin él, una app de Spring Boot arranca, ve que no tiene nada que hacer y se
apaga. **Con él, Tomcat se queda escuchando en un puerto y el proceso sigue vivo.** Esa es la
única razón por la que antes los servicios morían al segundo.

**`spring-boot-starter-actuator`**
Agrega endpoints de diagnóstico sin que escribas código. El que usamos es `/actuator/health`, que
responde `{"status":"UP"}`. Parece poca cosa, pero en la **Fase 15** es lo que Docker usa para
saber si un contenedor está listo antes de arrancar los que dependen de él.

**`<dependencyManagement>` y el BOM de Spring Cloud**
Un *BOM* (Bill of Materials) es una lista de versiones compatibles entre sí. Al importar
`spring-cloud-dependencies:2025.1.3`, le dices a Maven: «cuando yo pida una librería de Spring
Cloud, tú decides qué versión usar». Así, en la Fase 2 puedes escribir
`spring-cloud-starter-gateway-server-webmvc` **sin poner versión**, y Maven elige la correcta.
Evita el clásico infierno de versiones incompatibles.

**`@RestController` y `@GetMapping`**
`@RestController` le dice a Spring «esta clase atiende peticiones HTTP y sus respuestas se
convierten a JSON automáticamente». `@GetMapping("/ping")` le dice «este método responde a
`GET /ping`». Devolver un `Map` se convierte solo en un objeto JSON.

**`@Value("${spring.application.name}")`**
Lee un valor del `application.properties` y lo mete en una variable Java. Por eso el
`PingController` sabe su propio nombre y puerto **sin tenerlos escritos en el código**. Esto
importa: en la Fase 15 esos valores vendrán de variables de entorno de Docker, y el código no
cambiará.

---

## 4. Qué puedo hacer ahora

- ✅ Arrancar los 9 servicios con **un solo comando** y que **se queden encendidos**
- ✅ Preguntarle a cada servicio quién es (`/ping`)
- ✅ Comprobar de un vistazo si los 9 están sanos (`verificar-servicios.ps1`)
- ✅ Detenerlos todos limpiamente (`detener-todo.ps1`)
- ✅ Compilar cualquier servicio con `.\mvnw.cmd clean package`
- ✅ Trabajar **sin conexión a internet**: todas las librerías están en tu caché de Maven

---

## 5. Cómo lo compruebo

### La forma rápida

```powershell
cd "F:\COMPUTACION_E_INFORMATICA_CIBERTEC\6 - SEXTO CICLO\MODULO 2\DESARROLLO DE APLICACIONES WEB II\Proyectos\eclipse-workspace"
.\arrancar-todo.ps1
```

Se abrirán 9 ventanas. La primera vez cada una tarda ~20 segundos en compilar y arrancar. Cuando
se calmen, en la ventana original:

```powershell
.\verificar-servicios.ps1
```

**Lo que debes ver:**

```text
  Teleticket - estado de los servicios
  ---------------------------------------------------------------
  PUERTO  ESPERADO             /health    /ping
  8090    api-gateway          UP         OK
  8081    ms-usuarios          UP         OK
  8082    ms-eventos           UP         OK
  8083    ms-cola              UP         OK
  8084    ms-inventario        UP         OK
  8085    ms-ordenes           UP         OK
  8086    ms-pagos             UP         OK
  8087    ms-tickets           UP         OK
  8088    ms-notificaciones    UP         OK
  ---------------------------------------------------------------
  9/9 servicios vivos. Fase 0 verificada.
```

Para apagar todo:

```powershell
.\detener-todo.ps1
```

### La forma manual (para entender qué pasa)

Un solo servicio:

```powershell
curl.exe -s http://localhost:8082/ping
```
→ `{"puerto":8082,"servicio":"ms-eventos","estado":"ok","hora":"2026-10-22T03:44:02.396Z"}`

```powershell
curl.exe -s http://localhost:8082/actuator/health
```
→ un JSON que termina en `"status":"UP"`

Arrancar uno solo (útil al desarrollar):

```powershell
.\arrancar-todo.ps1 ms-eventos
```

O desde Eclipse: clic derecho en `MsEventosApplication.java` → *Run As* → *Java Application*.

### Resultado real de esta implementación

Los 9 servicios se compilaron, arrancaron y respondieron correctamente:

| Verificación | Resultado |
|---|---|
| `mvnw clean package` en los 9 | ✅ `BUILD SUCCESS` (sin red, usando la caché de Maven) |
| `mvnw test` en 3 servicios de muestra | ✅ Las pruebas generadas pasan |
| `/ping` en los 9 puertos | ✅ Cada uno devolvió su nombre y puerto correctos |
| `/actuator/health` en los 9 puertos | ✅ Los 9 en `"status":"UP"` |
| Los procesos siguen vivos tras arrancar | ✅ 9 procesos Java escuchando en 8080–8088 |
| `verificar-servicios.ps1` | ✅ `9/9 servicios vivos` |
| `detener-todo.ps1` | ✅ Los 9 procesos detenidos |

---

## 6. Qué sigue sin funcionar

Para no confundirse, esto es lo que **todavía no existe**:

| No funciona | Llega en |
|---|---|
| Guardar o leer datos (no hay base de datos) | Fase 3 |
| El gateway **no enruta nada** todavía: solo responde su propio `/ping` | Fase 2 |
| PostgreSQL, RabbitMQ y MailHog no están levantados | Fase 1 |
| No hay mensajes asíncronos | Fase 5 |
| Ningún servicio llama a otro (no hay Feign) | Fase 8 |
| No hay eventos, usuarios, colas, órdenes ni entradas | Fases 3 a 14 |

**El `/ping` es temporal.** Es un andamio para poder verificar las primeras fases; se puede
borrar en la Fase 16, cuando ya exista la colección de Postman.

---

## 7. Decisiones tomadas y problemas encontrados

### Decisión 1 — Spring Boot 4.1.1 con Spring Cloud 2025.1.3

Los `pom.xml` venían con Spring Boot **4.1.1**, pero ninguno declaraba Spring Cloud. No cualquier
versión de Spring Cloud sirve con Boot 4.1: la **2025.1.2** fue la que introdujo esa
compatibilidad, así que las anteriores (2025.0.0, 2025.1.0) habrían fallado.

Se eligió **2025.1.3** porque:
1. Es compatible con Boot 4.1.
2. **Ya estaba descargada** en `~/.m2`, igual que `spring-cloud-starter-openfeign:5.0.3` y
   `spring-cloud-starter-gateway-server-webmvc:5.0.3`, las que harán falta en las fases 2 y 8.

**Se validó antes de tocar los otros 8 proyectos:** se modificó solo `ms-eventos`, se ejecutó
`mvnw -o dependency:tree` (con `-o` = modo sin conexión, para probar que todo estuviera en caché)
y dio `BUILD SUCCESS`. Recién entonces se aplicó a los demás.

> **Alternativa descartada:** bajar a Boot 4.0.6 + Cloud 2025.1.0, que es lo que usa el ejemplo
> `Feign/api` del profesor. No fue necesario, y habría significado tocar 9 archivos sin motivo.

### Decisión 2 — `spring-boot-starter-webmvc` en vez de `spring-boot-starter-web`

En Spring Boot 4 existen los dos. Se usó `webmvc` porque es el nombre nuevo y es el que usa el
ejemplo `Feign/api` del profesor, así que el proyecto queda alineado con el material del curso.

Además se **reemplazó** `spring-boot-starter` en lugar de agregar el nuevo al lado:
`spring-boot-starter-webmvc` ya lo incluye de forma transitiva, así que tenerlo dos veces sería
redundante.

### Decisión 3 — `ms-teletickets` mantiene su nombre de carpeta

La documentación de diseño llama a este servicio `ms-tickets`, pero la carpeta se creó como
`ms-teletickets`. Renombrar la carpeta habría ensuciado el historial de Git y obligado a
reimportar el proyecto en Eclipse.

**Solución:** la carpeta se queda como está, pero `spring.application.name=ms-tickets` y la ruta
pública será `/api/tickets/**` (Fase 2). Así el sistema en ejecución coincide con los documentos
entregados. Se verificó: `GET localhost:8087/ping` devuelve `"servicio":"ms-tickets"`.

### Problema 1 — El plan decía actualizar `.gitignore`; estaba casi resuelto

El plan asumía que las carpetas `target/` podían acabar en el repositorio. Al revisarlo con
`git check-ignore`, resultó que **cada microservicio ya trae su propio `.gitignore`** generado
por Spring Initializr, que cubre `target/`, `.settings`, `.classpath`, `.project`, `.idea` y
`.vscode`. Se confirmó que **0 archivos de compilación están versionados**.

Así que el `.gitignore` de la raíz solo se amplió como **red de seguridad para carpetas
futuras** (`libs/mensajeria-comun` en la Fase 5 y `pruebas/` en la Fase 16), que no vendrán con
uno propio.

> **Pendiente, no bloqueante:** los tres `.zip` de los ejemplos del curso **sí** están
> versionados, y `Circuit Break.zip` pesa **31 MB**. No afecta al funcionamiento, pero hace el
> repositorio innecesariamente pesado. Conviene decidir si se quitan del historial.

### Problema 2 — Bug en `detener-todo.ps1`: `$pid` es de solo lectura

El bucle estaba escrito como `foreach ($pid in ...)`. En PowerShell, **`$PID` es una variable
automática de solo lectura** (contiene el identificador de la propia consola), así que asignarle
un valor lanza un error. Se renombró a `$procId`.

### Problema 3 — Bug en `verificar-servicios.ps1`: claves numéricas en un diccionario ordenado

La primera versión usaba `[ordered]@{ 8080 = "api-gateway" }` y la columna «ESPERADO» salía
vacía, con el resultado `0/9 servicios vivos` aunque los 9 estuvieran respondiendo.

**La causa:** en PowerShell, indexar un `[ordered]` con un **número** se interpreta como
*posición*, no como *clave*. Es decir, `$SERVICIOS[8080]` buscaba el elemento número 8080 (que no
existe) y devolvía `$null`.

**La solución:** poner las claves entre comillas (`"8080"`). Después del cambio:
`9/9 servicios vivos. Fase 0 verificada.`

> Los dos bugs se encontraron **ejecutando los scripts**, no leyéndolos. Vale la pena recordarlo
> para las fases siguientes: un script que no se ha ejecutado no está terminado.

### Decisión 4 — Un script de parada, además del de arranque

El plan solo pedía `arrancar-todo.ps1`. Se agregó `detener-todo.ps1` porque Maven
(`spring-boot:run`) deja un proceso Java hijo que **a veces sobrevive al cerrar la ventana**, y
entonces la siguiente vez aparece el error `Port 8082 was already in use`, que es muy confuso
cuando estás empezando. El script mata lo que esté escuchando en los puertos del proyecto, sin
ambigüedad.

> **Nota añadida en la Fase 1:** este script se endureció después. Al moverse el gateway al 8090,
> el rango pasó a 8081–8088 y 8090, y se le agregó un seguro para que **solo detenga procesos
> `java`**. Sin ese seguro habría intentado matar `com.docker.backend`, que es lo que ahora
> escucha en el 8080, y habría tumbado Docker Desktop entero.

También se agregó un aviso en `arrancar-todo.ps1`: si un puerto ya está ocupado, lo informa y
omite ese servicio, en vez de abrir una ventana que falla y se cierra.

---

## 8. Relación con el informe

| Requisito del `.docx` | Cómo lo cumple esta fase |
|---|---|
| **OBJ 2** — «Implementar ocho microservicios Spring Boot» | Los 9 proyectos (8 de negocio + gateway) arrancan y responden. Es el punto de partida verificable |
| **Producto 3** — API Gateway | El proyecto existe, arranca en el 8090 y está preparado con el BOM de Spring Cloud. Enrutará en la Fase 2 |
| **Producto 4** — Ocho microservicios de negocio | Los 8 arrancan en los puertos 8081–8088 |
| **Sección 12.2 del diseño** — Puertos e infraestructura local | Los 9 puertos asignados exactamente como dice la tabla |
| **Sección 11 del diseño** — «Spring Actuator: nada que cambiar, se agrega la dependencia» | Hecho: `/actuator/health` activo en los 9 |
| **Sección 11 del diseño** — «Config Server: toda la configuración va en properties, sin valores escritos en el código» | Puerto y nombre se leen del `application.properties`, no están en el código Java |

---

## Siguiente paso

**[Fase 1 — Infraestructura local con Docker](../../PLAN-DE-IMPLEMENTACION.md)**: levantar
PostgreSQL con las 9 bases de datos, RabbitMQ con su consola web y MailHog, todo con un solo
`docker compose up`. No se toca ni una línea de Java.
