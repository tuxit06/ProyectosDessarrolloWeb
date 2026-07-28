# cursosapp — ProyectoBase S12 (punto de partida para el lab)

> Esta es la versión **sin resolver** — la que reciben los estudiantes (junto con `Para_Estudiantes/lab_clase12.md`). La versión completa (referencia para corregir) está en `Recursos_Profesor/ProyectoFinal/cursosapp`.

Parte de la base de S11 actualizada (login + roles + `@PreAuthorize` + página 403 + CRUD de Usuarios + sesiones concurrentes + recuperación de contraseña, todo ya aplicado). En esta clase se agrega una **API REST en JSON** (`/api/cursos`) al lado de las vistas HTML de siempre (`/cursos`, `/usuarios`), usando `@RestController`, `@RequestBody`, `ResponseEntity`, CORS, una tabla de constantes de rol y autenticación **JWT**.

---

## Qué ya viene resuelto (de S9-S11)

- CRUD completo de `Curso` con vistas Thymeleaf (`/cursos`).
- Login, logout, BCrypt, tabla `usuarios` con roles.
- `@PreAuthorize` protegiendo crear/editar/eliminar, página 403 propia.
- CRUD de Usuarios (`/usuarios`, solo ADMIN), sesiones concurrentes (`maximumSessions`), recuperación de contraseña por email (Mailtrap).
- `listarConProfesor()` en `CursoRepository`/`CursoService` (JOIN FETCH de S9) — se va a reutilizar en la API.

## Qué falta resolver en esta clase (S12)

Todo marcado con `CLASE 12 - PASO X.Y`, distribuido en:

| Archivo | Qué hay que hacer |
|---|---|
| `controller/package-info.md` | PASO A.1 — crear `CursoRestController.java` (CRUD completo en JSON) |
| `config/package-info.md` | PASO B.1 — crear `CorsConfig.java` (bean `CorsConfigurationSource`) |
| `config/SecurityConfig.java` | PASO C.1 — activar `.cors(cors -> {})` |
| `config/SecurityConfig.java` | PASO C.2 — dejar `/api/**` público (temporal, se reemplaza en la Parte E) |
| `security/package-info.md` | PASO D.1 — crear `Rol.java` (enum de constantes) |
| `controller/package-info.md` | PASO D.2 — crear `RolRestController.java` (`GET /api/roles`) |
| `service/UsuarioService.java` | PASO D.3 — descomentar `validarRol()` y sus 2 llamadas |
| `security/package-info.md` | PASO E.1 — crear `JwtService.java` (generar/validar JWT) |
| `security/package-info.md` | PASO E.2 — crear `JwtAuthFilter.java` (filtro que lee el Bearer token) |
| `controller/package-info.md` | PASO E.3 — crear `AuthController.java` (`POST /api/auth/login`) |
| `config/SecurityConfig.java` | PASO E.4 — reemplazar el `/api/**` público por reglas JWT + registrar el filtro |
| `controller/CursoRestController.java` | PASO E.5 — descomentar `@PreAuthorize` en los métodos de escritura |

Ver el paso a paso completo en `Para_Estudiantes/lab_clase12.md`.

## Qué NO se resuelve en esta clase (fuera de alcance)

- Refresh tokens (el JWT expira y hay que volver a loguearse).
- Documentación autogenerada (Swagger/OpenAPI) — se menciona como bonus (springdoc-openapi), no se implementa.
- Paginación de resultados.
- Microservicios — no se cubre en esta clase (hueco del programa oficial, ver `_gestion/plan_semanal.md`).

---

## Cómo probar (una vez resuelto el lab)

1. Tener MySQL corriendo y la base cargada (igual que S9-S11).
2. Arrancar la app.
3. Importar `postman-collection.json` en Postman.
4. Pedir un JWT: `POST /api/auth/login` con `{"username":"admin","password":"admin123"}`.
5. Usar ese token (header `Authorization: Bearer <token>`) para probar `GET/POST/PUT/DELETE /api/cursos` y `GET /api/roles`.
6. Confirmar que `listar()` usa `cursoService.listarConProfesor()` — si usa `listar()` a secas, la serialización del `profesor` (LAZY) puede fallar.

```bash
mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run        # Linux/Mac
```

---

## Si algo falla

- **`GET /api/cursos` tira un error de serialización:** revisar que `CursoRestController.listar()` llame a `cursoService.listarConProfesor()` (JOIN FETCH), no a `listar()`.
- **`GET /api/cursos` devuelve 401 aunque mandé un token:** revisar que el header se llame `Authorization` con valor `Bearer <token>` (con espacio), y que hayas hecho el PASO E.4 completo en `SecurityConfig`.
- **`POST /api/cursos` devuelve 403 en vez de 201:** el JWT es válido pero el usuario no es ADMIN, o falta el PASO E.5 (`@PreAuthorize` en `CursoRestController`).
- **El front en otro origen no puede llamar la API (error de CORS en consola del navegador):** confirmar que `CorsConfig` existe y que `.cors(cors -> {})` está descomentado en `SecurityConfig` (PASO C.1).
- **`POST`/`PUT` devuelven 400 sin razón aparente:** revisar el body JSON — falta algún campo validado (`@NotBlank`, `@NotNull`, etc.) o el `profesor` no trae `id`.
- **Crear un usuario con un rol con typo tira un error 500:** es esperado si ya hiciste el PASO D.3 — `validarRol()` rechaza cualquier valor que no sea `ADMIN` o `USER`.
