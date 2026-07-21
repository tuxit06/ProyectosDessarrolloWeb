# cursosapp — ProyectoBase S11 (punto de partida para el lab)

> Esta es la versión **sin resolver** — la que reciben los estudiantes (junto con `Para_Estudiantes/lab_clase11.md`). La versión completa (referencia para corregir) está en `Recursos_Profesor/ProyectoFinal/cursosapp`.

Parte de la base con login de S10 ya aplicado (Curso, Profesor, Usuario, `SecurityConfig` con `formLogin`/`logout`). En esta clase se agrega **autorización por rol**: solo `ADMIN` puede crear, editar o eliminar cursos, con una página 403 propia.

---

## Qué ya viene resuelto (de S10)

- Login, logout, BCrypt, tabla `usuarios` con el campo `rol` (sin usar todavía).
- Navbar dinámico con `sec:authorize` (usuario logueado / link de login).

## Qué falta resolver en esta clase (S11)

Todo marcado con `CLASE 11 - PASO X.Y`, distribuido en:

| Archivo | Qué hay que hacer |
|---|---|
| `config/SecurityConfig.java` | PASO A.1 — habilitar `@EnableMethodSecurity` |
| `controller/CursoController.java` | PASO B.1 a B.5 — agregar `@PreAuthorize` en crear/editar/eliminar |
| `config/SecurityConfig.java` | PASO C.1/C.2 — permitir `/403` y redirigir ahí con `exceptionHandling` |
| `controller/HomeController.java` | PASO C.3 — agregar el método `GET /403` |
| `templates/403.html` | PASO C.4 — ya viene armado, solo confirmar que existe |
| `config/SecurityConfig.java` | PASO D.1 (opcional) — logout más completo |
| `templates/cursos.html` | PASO E.1 a E.3 — ocultar botones de admin con `sec:authorize` |

Ver el paso a paso completo en `Para_Estudiantes/lab_clase11.md`.

## Qué NO se resuelve en esta clase (fuera de alcance)

- Registro de usuarios nuevos o cambio de rol desde la UI.
- Auditoría de cambios.

---

## Cómo correr (una vez resuelto el lab)

1. Tener MySQL corriendo y la base cargada (igual que S10).
2. Arrancar la app.
3. Loguearse como `estudiante` o `profesor` (rol USER) e intentar entrar a `/cursos/nuevo` a mano por la URL → debe redirigir a `/403`.
4. Loguearse como `admin` → el CRUD completo funciona como siempre.

```bash
mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run        # Linux/Mac
```

---

## Si algo falla

- **`@PreAuthorize` no bloquea nada:** falta `@EnableMethodSecurity` en `SecurityConfig` (PASO A.1) — sin esa anotación, `@PreAuthorize` se ignora silenciosamente.
- **En vez de la página 403 propia, aparece la página de error blanca de Spring Boot:** revisar el PASO C.1 (agregar `/403` a `permitAll()`) y el PASO C.2 (`exceptionHandling`).
- **El botón "Nuevo curso" no se oculta para el usuario USER:** revisar el PASO E.2 en `cursos.html`.
