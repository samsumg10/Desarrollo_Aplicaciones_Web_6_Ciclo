# Teleticket

Plataforma de venta de entradas: un API Gateway y 8 microservicios Spring Boot, con PostgreSQL,
RabbitMQ y MailHog en Docker.

## Requisitos

- **JDK 21**
- **Docker Desktop** abierto y en marcha (tarda ~2 min en arrancar)
- **Git**

Maven no hace falta: cada proyecto trae `mvnw`. La primera vez necesita internet.

## Instalación y arranque manual

### 1. Obtener el proyecto

Descomprime el archivo recibido (o clona el repositorio con `git clone <url>`) y abre una
terminal PowerShell dentro de la carpeta raíz del proyecto, la que contiene `infra`, `libs` y
los `ms-*`.

### 2. Levantar la infraestructura (PostgreSQL, RabbitMQ, MailHog)

```powershell
cd infra
docker compose up -d
cd ..
```

Comprueba que los 3 contenedores estén `healthy` con `docker compose ps`.

### 3. Instalar la librería compartida (solo la primera vez)

```powershell
cd libs\mensajeria-comun
.\mvnw.cmd clean install -DskipTests
cd ..\..
```

### 4. Levantar los microservicios

Abre **una terminal por servicio**, entra a su carpeta y ejecuta `.\mvnw.cmd spring-boot:run`
(en Linux/macOS: `./mvnw spring-boot:run`). Déjalas abiertas.

| Orden | Carpeta | Puerto |
| --- | --- | --- |
| 1 | `ms-usuarios` | 8081 |
| 2 | `ms-eventos` | 8082 |
| 3 | `ms-cola` | 8083 |
| 4 | `ms-inventario` | 8084 |
| 5 | `ms-ordenes` | 8085 |
| 6 | `ms-pagos` | 8086 |
| 7 | `ms-teletickets` | 8087 |
| 8 | `ms-notificaciones` | 8088 |
| 9 | `ms-api-gateway` (al final) | **8090** |

Ejemplo para el primero:

```powershell
cd ms-usuarios
.\mvnw.cmd spring-boot:run
```

### 5. Comprobar que todo está vivo

Cada servicio responde en `http://localhost:<puerto>/actuator/health` con `"status":"UP"`:

```powershell
curl.exe http://localhost:8081/actuator/health
```

La puerta de entrada para las pruebas es el gateway: `http://localhost:8090`.

## Detener todo

Cierra cada terminal de servicio con `Ctrl+C`. Para la infraestructura:
`cd infra ; docker compose down`.

## Si algo falla

- **Puerto ocupado:** cambia el puerto con una variable de entorno en la terminal del servicio,
  antes de arrancarlo; por ejemplo `$env:MS_COLA_PORT = "9083"` o `$env:GATEWAY_PORT = "8095"`.
  Las variables disponibles (puertos, base de datos, RabbitMQ) están en [.env.example](.env.example).
  Para la infraestructura, copia ese archivo a `.env` y usa `docker compose --env-file ../.env up -d`.
- **Un servicio no compila por `mensajeria-comun`:** repite el paso 3.
- **Docker no responde:** abre Docker Desktop y espera a que termine de arrancar.

Con el montaje por defecto no necesitas configurar nada: los servicios se conectan solos a
`localhost` (PostgreSQL en el puerto **5433**, RabbitMQ en el 5672).
