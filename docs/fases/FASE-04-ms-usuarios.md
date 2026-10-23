# Fase 4 — `ms-usuarios`: registro y roles

**Estado:** ✅ Completada · **Fecha:** 23/10/2026 · **Cubre:** CU-01 (registro; el login con JWT llega con Spring Security)

## 1. En una frase

> Un comprador ya puede registrarse. Su contraseña se guarda cifrada con BCrypt, nunca en texto
> plano, y el sistema queda con 6 usuarios de prueba listos para las fases siguientes.

## 2. Qué se creó

Mismo patrón que `ms-eventos` (Fase 3): entidad → repositorio → DTO → servicio → controlador.

| Archivo (`ms-usuarios/src/main/...`) | Para qué |
|---|---|
| `resources/schema.sql` | Tabla `usuario`, con `UNIQUE(dni)`, `UNIQUE(email)` y `CHECK` de DNI (8 dígitos) y de rol |
| `entity/Usuario.java`, `Rol.java` | Sin setters: un usuario creado no se modifica desde fuera |
| `dto/RegistroRequest.java` | Validaciones: DNI de 8 dígitos, correo válido, contraseña de 8 a 72 caracteres |
| `dto/UsuarioResponse.java` | **Nunca** incluye la contraseña ni su hash |
| `service/UsuarioService.java` | Reglas del registro |
| `config/SeguridadConfig.java` | `BCryptPasswordEncoder` (solo `spring-security-crypto`, no Security completo) |
| `config/DatosIniciales.java` | Siembra 6 usuarios si la tabla está vacía |
| `controller/UsuarioController.java` | API pública |
| `controller/internal/UsuarioInternalController.java` | API interna |
| `exception/*` | 404, 409, 401 y el manejador global (mismo formato JSON que `ms-eventos`) |

| Método | Ruta | Acceso | Qué hace |
|---|---|---|---|
| `POST` | `/api/usuarios` | público | Registra un **COMPRADOR** → `201` |
| `GET` | `/api/usuarios/me` | público | Perfil del `X-Usuario-Id`. Sin cabecera o inválida → `401` |
| `GET` | `/internal/usuarios/{id}/contacto` | 🔒 interno | `{nombres, email}`. Lo usará `ms-notificaciones` (Fase 13) |

**Usuarios sembrados** (contraseña de todos: `Teleticket123`). Sus ids son fijos para usarlos como
`X-Usuario-Id` en las pruebas:

| Id | Rol | Correo |
|---|---|---|
| `00000000-0000-0000-0000-000000000001` … `03` | COMPRADOR | ana@ / luis@ / rosa@`teleticket.test` |
| `…0004` | OPERADOR | operador@teleticket.test |
| `…0005` | CONTROLADOR | controlador@teleticket.test |
| `…0006` | ATENCION | atencion@teleticket.test |

## 3. Cómo lo compruebo

Con `ms-usuarios` y el gateway arriba (`.\arrancar-todo.ps1 ms-usuarios,ms-api-gateway`):

```powershell
'{"dni":"70123456","nombres":"Daniel Fuentes","email":"Daniel@Correo.com","password":"Secreta123"}' | Set-Content r.json
curl.exe -s -X POST http://localhost:8090/api/usuarios -H "Content-Type: application/json" -d "@r.json"
```
→ `201`. La respuesta tiene `id, dni, nombres, email, rol, creadoEn` y **ninguna contraseña**. El
correo vuelve en minúsculas.

| Prueba | Comando / acción | Resultado esperado |
|---|---|---|
| Hash, no contraseña | `docker exec teleticket-postgres psql -U teleticket -d db_usuarios -c "SELECT email, password_hash FROM usuario;"` | `$2a$10$...` (60 caracteres) |
| DNI repetido | Repetir el `POST` | `409 DNI_DUPLICADO` |
| Correo repetido (otra capitalización) | Mismo correo con `DANIEL@correo.com` y otro DNI | `409 EMAIL_DUPLICADO` |
| Validaciones | `{"dni":"123","nombres":"","email":"x","password":"corta"}` | `400` con los 4 campos |
| JSON roto | Cuerpo `{roto` | `400 JSON_INVALIDO` |
| **No se elige el rol** | Registrar con `"rol":"OPERADOR"` | Queda `COMPRADOR` |
| `/me` | Con `-H "X-Usuario-Id: <id>"` / sin cabecera / `abc` | `200` / `401` / `401` |
| Interno | `GET localhost:8081/internal/usuarios/<id>/contacto` y lo mismo por `:8090` | `200` / `404` |

**Resultado real:** las 8 comprobaciones pasaron. Compila y los tests pasan (`mvnw clean package`).

## 4. Decisiones y problemas

**Decisión 1 — El registro público no acepta `rol`.** El plan original lo incluía en el cuerpo. Se
quitó: si el cliente pudiera elegirlo, cualquiera se registraría como OPERADOR y publicaría eventos.
Se verificó enviándolo a propósito: el `"rol":"OPERADOR"` se ignora y queda `COMPRADOR`. El personal
se crea por siembra hoy y, cuando llegue Spring Security, por un endpoint restringido a OPERADOR.

**Decisión 2 — Siembra por código y no con `data.sql`.** Un `data.sql` obligaría a pegar un hash
BCrypt calculado a mano. Con un `CommandLineRunner` el hash lo calcula BCrypt de verdad y la siembra
solo ocurre si la tabla está vacía. Se apaga con `SEMBRAR_USUARIOS=false`.

**Decisión 3 — Dos defensas contra el duplicado.** El servicio comprueba antes (`existsByDni`) para
dar un mensaje claro, pero dos registros simultáneos pueden pasar esa comprobación a la vez. La
restricción `UNIQUE` de la base rechaza al segundo y el manejador la traduce a `409
DATOS_DUPLICADOS`. Sin esta segunda red, esa carrera acabaría en un `500`.

**Decisión 4 — Contraseña con tope de 72 caracteres.** BCrypt solo lee los primeros 72 bytes: una
contraseña más larga se truncaría sin avisar. Mejor rechazarla.

**Problema — JSON mal formado devolvía `500`.** No se produjo aquí (se añadió el manejador desde el
inicio), pero es un defecto latente de `ms-eventos`: allí un cuerpo `{roto` cae en el error genérico.
Queda anotado para corregirlo al volver a tocar `ms-eventos` en el Bloque B.

## 5. Qué sigue sin funcionar

| No funciona | Llega en |
|---|---|
| No hay login: la identidad es la cabecera `X-Usuario-Id`, que cualquiera puede escribir | Spring Security (JWT) |
| Los roles se guardan pero no se comprueban en ningún endpoint | Spring Security |
| `ms-notificaciones` aún no consume `/internal/.../contacto` | Fase 13 |

## 6. Relación con el informe

| Requisito | Cómo se cumple |
|---|---|
| **CU-01** Registrarse / iniciar sesión | Registro ✅. Inicio de sesión con JWT: etapa posterior (anexo C: «el login con JWT llega con Spring Security») |
| **RN-04** Sesión requerida para la sala de espera | La base está puesta: el `X-Usuario-Id` identifica a la persona. Lo exigirá `ms-cola` (Fase 7) |
| **Ley 29733** Protección de datos personales | Contraseña cifrada; el DNI no se expone en el endpoint interno de contacto |
| **Sección 7.1 del diseño** — tabla `usuario` | ✅ Con las restricciones indicadas |
| **DEC-01** | ✅ Plantilla de conexión con `DB_NAME=db_usuarios` |

**Siguiente:** Bloque B → Fases 5 y 6 juntas (mensajería con RabbitMQ + `ms-inventario`).
