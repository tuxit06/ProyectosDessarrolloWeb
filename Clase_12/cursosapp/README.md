# cursosapp — ProyectoBase S12 (punto de partida para el lab)

> Esta es la versión **sin resolver** — la que reciben los estudiantes (junto con `Para_Estudiantes/lab_clase12.md`). La versión completa (referencia para corregir) está en `Recursos_Profesor/ProyectoFinal/cursosapp`.

Parte de la base de S11 (login + roles + `@PreAuthorize` + página 403 ya aplicados). En esta clase se agrega una **API REST en JSON** (`/api/cursos`) al lado de las vistas HTML de siempre (`/cursos`), usando `@RestController`, `@RequestBody`, `ResponseEntity` y CORS.

---

## Qué ya viene resuelto (de S9-S11)

- CRUD completo de `Curso` con vistas Thymeleaf (`/cursos`).
- Login, logout, BCrypt, tabla `usuarios` con roles.
- `@PreAuthorize` protegiendo crear/editar/eliminar, página 403 propia.
- `listarConProfesor()` en `CursoRepository`/`CursoService` (JOIN FETCH de S9) — se va a reutilizar en la API.

## Qué falta resolver en esta clase (S12)

Todo marcado con `CLASE 12 - PASO X.Y`, distribuido en:

| Archivo | Qué hay que hacer |
|---|---|
| `controller/package-info.md` | PASO A.1 — crear `CursoRestController.java` (CRUD completo en JSON) |
| `config/package-info.md` | PASO B.1 — crear `CorsConfig.java` (bean `CorsConfigurationSource`) |
| `config/SecurityConfig.java` | PASO C.1 — activar `.cors(cors -> {})` en la cadena de filtros |
| `config/SecurityConfig.java` | PASO C.2 — agregar `"/api/**"` a la lista de rutas públicas |

Ver el paso a paso completo en `Para_Estudiantes/lab_clase12.md`.

## Qué NO se resuelve en esta clase (fuera de alcance)

- Proteger la API con login/token (`/api/**` queda público a propósito — el foco es `@RestController`/JSON/CORS, no autenticación de APIs sin sesión).
- Documentación autogenerada (Swagger/OpenAPI).
- Paginación de resultados.

---

## Cómo probar (una vez resuelto el lab)

1. Tener MySQL corriendo y la base cargada (igual que S9-S11).
2. Arrancar la app.
3. Importar `postman-collection.json` en Postman y probar `GET /api/cursos`, `GET /api/cursos/{id}`, `POST /api/cursos`, `PUT /api/cursos/{id}`, `DELETE /api/cursos/{id}`.
4. Confirmar que `listar()` usa `cursoService.listarConProfesor()` — si usa `listar()` a secas, la serialización del `profesor` (LAZY) puede fallar.

```bash
mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run        # Linux/Mac
```

---

## Si algo falla

- **`GET /api/cursos` tira un error de serialización:** revisar que `CursoRestController.listar()` llame a `cursoService.listarConProfesor()` (JOIN FETCH), no a `listar()`.
- **`POST /api/cursos` devuelve 403 en vez de 201:** falta agregar `/api/**` a `.permitAll()` en `SecurityConfig` (PASO C.2).
- **El front en otro origen no puede llamar la API (error de CORS en consola del navegador):** confirmar que `CorsConfig` existe y que `.cors(cors -> {})` está descomentado en `SecurityConfig` (PASO C.1).
- **`POST`/`PUT` devuelven 400 sin razón aparente:** revisar el body JSON — falta algún campo validado (`@NotBlank`, `@NotNull`, etc.) o el `profesor` no trae `id`.
