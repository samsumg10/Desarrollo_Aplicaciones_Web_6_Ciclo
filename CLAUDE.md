# Proyecto Teleticket (microservicios Spring Boot)

Plataforma de venta de entradas con API Gateway + 8 microservicios. Idioma: español de Perú (ver
`~/.claude/CLAUDE.md`).

## Dónde está todo (leer solo lo necesario)

- `PLAN-DE-IMPLEMENTACION.md` — las 19 fases con sus pruebas. El **mapa de fases** (tabla al
  inicio) dice cuáles están hechas (✅).
- `docs/fases/` — qué se construyó en cada fase ya terminada.
- `docs/decisiones/DEC-01-bases-de-datos.md` — una instancia de PostgreSQL, 9 bases, conexión por
  variables `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD`.
- `docs/definiciones/definiciones-de-codigo.md` — explica líneas de configuración difíciles.
- Diseño de referencia (fuera del repo): `../arquitectura-microservicios-teleticket.md`.

## Estado

Fases 0–8 hechas. **Siguiente: Fase 9** (`ms-pagos` + `POST /api/ordenes/{id}/pagar`). Orden
acordado hasta cumplir el OBJ 2: 9 → 10 → 14; luego 11, 16, 15. Se descartan 12, 13, 17
(ojo: sin la 12 las reservas sin pagar **no vencen** y retienen el stock; ver FASE-06).

## Datos que no se deducen del código

- **Gateway en 8090, no 8080** (el 8080 lo ocupa un contenedor de otro proyecto). PostgreSQL de
  Docker en **5433** (el 5432 es el PostgreSQL de Windows).
- Servicios: usuarios 8081, eventos 8082, cola 8083, inventario 8084, ordenes 8085, pagos 8086,
  tickets 8087 (carpeta `ms-teletickets`, nombre `ms-tickets`), notificaciones 8088.
- Usuarios de prueba: ids `00000000-0000-0000-0000-00000000000N` (1–3 compradores, 4 operador,
  5 controlador, 6 atención), contraseña `Teleticket123`.
- Infra: `cd infra ; docker compose up -d`. Docker Desktop tarda ~2 min en arrancar.
- RabbitMQ: `localhost:5672`, consola `15672` (guest/guest). Conexión por `RABBIT_HOST/PORT/USER/PASSWORD`.
  Todo nombre de exchange, routing key o cola va en `libs/mensajeria-comun` (`Mensajeria.java`);
  los servicios publican solo por `PublicadorEventos`, nunca con `RabbitTemplate`.
- `ms-cola`: la capacidad por ciclo se prueba con `$env:COLA_CAPACIDAD_POR_CICLO = "2"` en la consola
  desde la que arrancas (también `COLA_CICLO_SEGUNDOS`, `COLA_PASE_MINUTOS`). Para ver el sorteo se
  crea un evento con `inicioVenta` 2 minutos adelante (comandos en FASE-07).
- `ms-ordenes` llama por Feign a cola, eventos e inventario (`MS_COLA_URL`, `MS_EVENTOS_URL`,
  `MS_INVENTARIO_URL`, las mismas variables del gateway). Timeouts 1 s / 3 s. Si falla algo tras
  consumir el pase, lo devuelve con `POST /internal/pases/{codigo}/devolver` (ms-cola).
- Spring Boot 4.1.1 + Spring Cloud 2025.1.3 + **Jackson 3** (`tools.jackson`): la propiedad
  `spring.jackson.serialization.write-dates-as-timestamps` impide arrancar.
- `spring.jpa.defer-datasource-initialization` debe ir en `false` (las tablas las crea `schema.sql`).
- El gateway reenvía la ruta completa: los controladores van en `/api/<servicio>`, no en la raíz.

## Reglas de trabajo (cada una costó tiempo)

0. **`libs/mensajeria-comun` se instala antes que los servicios** (`cd libs\mensajeria-comun ;
   .\mvnw.cmd -o clean install`): `ms-eventos` e `ms-inventario` la leen de `~/.m2`. Si cambias la
   librería, reinstálala y recompila los servicios.
1. **Detener los servicios antes de `mvnw clean package`** (`.\detener-todo.ps1`): los `.jar`
   en uso bloquean el `clean`. Compilar siempre con `-o` (sin red).
2. Un `404` no prueba que el enrutamiento falle: apagar el destino y ver si el error cambia.
3. Compilar no basta: los fallos de configuración solo aparecen **al arrancar**.
4. Archivos en **LF** (`.gitattributes`). Para generar/editar varios archivos, usar un script Python
   que escriba en modo binario; los heredocs largos de Bash fallan en este entorno.
5. Probar con `curl.exe` (no `curl`) y cuerpos JSON en archivos (`-d "@x.json"`).
6. Verificar al cerrar cada bloque con `.\verificar-servicios.ps1` y `.\verificar-infra.ps1`.
7. Documento de fase: ~100–120 líneas (qué se creó, cómo probarlo, decisiones y problemas, relación
   con el informe). No repetir conceptos ya explicados en `docs/definiciones/`.
8. No hacer commit salvo que se pida.
