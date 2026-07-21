# Lab Clase 11 — Spring Security 2 (autorización por rol)

## Información general

| Dato | Valor |
|------|-------|
| Curso | SC-403 Desarrollo de Aplicaciones Web y Patrones |
| Universidad | Fidélitas |
| Modalidad | En clase, guiado por el profesor |
| Evaluación | Ninguna (el lab en sí) |
| Tiempo estimado | 75 minutos |

---

## Propósito

Desde S10, `cursosapp` exige login — pero cualquier usuario logueado, sea `admin`, `profesor` o `estudiante`, puede crear, editar y eliminar cursos por igual. Eso no está bien: en la mayoría de las apps reales, solo ciertos roles pueden modificar datos.

En este lab agregás **autorización por rol** con `@PreAuthorize`: solo el rol `ADMIN` va a poder crear, editar o eliminar cursos. Cualquier otro usuario que lo intente va a caer en una página de **acceso denegado (403)** propia.

---

## Objetivos de aprendizaje

Al terminar este lab vas a haber demostrado que sos capaz de:

1. Habilitar `@PreAuthorize` en un proyecto Spring Boot con `@EnableMethodSecurity`.
2. Restringir métodos de un Controller por rol con `@PreAuthorize("hasRole('ADMIN')")`.
3. Distinguir `hasRole()` de `hasAuthority()` y entender que son equivalentes.
4. Configurar una página de acceso denegado (403) propia.
5. Usar `sec:authorize` para ocultar botones según el rol, entendiendo que es solo cosmético.

---

## Material entregado

| Archivo/Carpeta | Descripción |
|---|---|
| `cursosapp/` | Mismo proyecto de S10 (login, BCrypt, tabla `usuarios` con roles), con los cambios de este lab pre-comentados. |

---

## Antes de empezar

Verificá que el proyecto base arranca sin problemas y que el login de S10 sigue funcionando:

```bash
cd cursosapp
mvnw.cmd spring-boot:run     # Windows
./mvnw spring-boot:run       # Linux/Mac
```

Logueate con `estudiante` / `estudiante123` y confirmá que HOY podés crear/editar/eliminar cursos sin ninguna restricción — eso es lo que cambia en este lab.

---

## Cómo está organizado el código pre-comentado

Igual que en S9 y S10: bloques `// CLASE 11 - PASO X.Y` (o `<!-- CLASE 11 - PASO X.Y -->` en HTML) dentro de archivos que YA existen, listos para descomentar. Esta clase no agrega ningún archivo Java nuevo (a diferencia de S9 y S10) — solo modifica archivos existentes, salvo `templates/403.html`, que ya viene armado completo (no requiere edición, solo confirmar que existe).

---

## Parte A — Habilitar `@PreAuthorize`

1. Abrí `config/SecurityConfig.java`. Descomentá el import de `EnableMethodSecurity` (**PASO A.1**) y la anotación `@EnableMethodSecurity` sobre la clase.
2. Sin este paso, `@PreAuthorize` en la Parte B se va a ignorar silenciosamente — no da error, simplemente no protege nada. Es el error más común de esta clase, así que confirmá este paso antes de seguir.

---

## Parte B — Restringir `CursoController` por rol

Abrí `controller/CursoController.java`. Vas a descomentar `@PreAuthorize` en 5 lugares:

1. **PASO B.2** — `mostrarFormNuevo` (formulario de creación).
2. **PASO B.3** — `guardar` (POST que crea el curso).
3. **PASO B.4** — `mostrarFormEditar` y `actualizar` (formulario y POST de edición).
4. **PASO B.5** — `eliminar`. Fijate que este usa `hasAuthority("ROLE_ADMIN")` en vez de `hasRole("ADMIN")` — es a propósito, para que compares ambas sintaxis. Logran exactamente el mismo resultado.

Los métodos `listar` y `detalle` **no llevan `@PreAuthorize`** — cualquier usuario autenticado (ADMIN o USER) puede seguir viendo los cursos. Solo modificar está restringido.

Reiniciá la app y probá: logueado como `estudiante`, entrá a `/cursos/nuevo` escribiendo la URL a mano. Deberías ver un error (todavía sin la página propia — eso es la Parte C).

---

## Parte C — Página de acceso denegado (403)

1. En `config/SecurityConfig.java`, agregá `"/403"` a la lista de rutas públicas (**PASO C.1**).
2. Descomentá la línea `.exceptionHandling(...)` al final de la cadena (**PASO C.2**) — fijate que el punto y coma final se mueve de lugar, seguí la instrucción del comentario con cuidado.
3. En `controller/HomeController.java`, descomentá el método `accesoDenegado()` (**PASO C.3**).
4. Confirmá que existe `templates/403.html` (**PASO C.4** — ya viene armado, no hace falta tocarlo).
5. Reiniciá la app y repetí la prueba de la Parte B: ahora deberías caer en la página 403 propia, con estilo Bootstrap.

---

## Parte D — Logout más completo (opcional)

En `config/SecurityConfig.java`, descomentá las dos líneas dentro de `.logout(...)` (**PASO D.1**): `invalidateHttpSession(true)` y `deleteCookies("JSESSIONID")`. No cambia el comportamiento visible, pero es una buena práctica adicional para dejar la sesión completamente limpia en el servidor.

---

## Parte E — Ocultar botones según el rol

1. En `templates/cursos.html`, el namespace `xmlns:sec` ya está agregado (**PASO E.1**).
2. Agregá `sec:authorize="hasRole('ADMIN')"` al botón "Nuevo curso" (**PASO E.2**).
3. Agregá el mismo atributo al link de editar y al botón de eliminar de cada tarjeta de curso (**PASO E.3**).
4. Logueate como `estudiante` y confirmá que esos botones ya no aparecen. Logueate como `admin` y confirmá que siguen ahí.

**Importante:** esto es solo cosmético. Aunque el botón esté oculto, si un usuario `estudiante` escribe la URL `/cursos/nuevo` a mano, `@PreAuthorize` lo sigue bloqueando (Parte B) — probalo para confirmarlo.

---

## Problemas comunes

| Síntoma | Solución |
|---|---|
| `@PreAuthorize` no bloquea nada, cualquier usuario puede crear/editar/eliminar | Falta `@EnableMethodSecurity` en `SecurityConfig` (Parte A). Sin esa anotación, `@PreAuthorize` se ignora sin avisar. |
| En vez de la página 403 propia, aparece la pantalla de error blanca de Spring Boot | Revisá el PASO C.1 (`/403` en `permitAll()`) y el PASO C.2 (`exceptionHandling`). Si `/403` no es público, entrar ahí también pide login y genera un loop. |
| Error de compilación en `SecurityConfig.java` después del PASO C.2 | Revisá el punto y coma: tiene que quedar solo UNO, al final de toda la cadena (después de `.exceptionHandling(...)`), no después de `.logout(...)`. |
| El botón "Nuevo curso" no se oculta para `estudiante` | Revisá el PASO E.2 y que el namespace `xmlns:sec` esté en la etiqueta `<html>` de `cursos.html`. |
| `estudiante` sigue pudiendo eliminar cursos aunque no vea el botón | Revisá el PASO B.5 en `CursoController` — la restricción real es el `@PreAuthorize`, no el HTML. |

---

## Recursos de consulta

| Tema | Enlace |
|---|---|
| Spring Security — Method Security (`@PreAuthorize`) | https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html |
| Spring Security — Authorization Architecture | https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html |
| Baeldung — Spring Security Custom Access Denied Page | https://www.baeldung.com/spring-security-custom-access-denied-page |
| Baeldung — Intro to Spring Security Expressions (hasRole/hasAuthority) | https://www.baeldung.com/spring-security-expressions |

---

## Preguntas

Cualquier duda durante el lab podés consultarla en:

- El canal `Consultas` del equipo de Teams.
- Directamente en clase, levantando la mano.

---

## Cronograma de cierre del curso

Al principio de esta clase el profesor mostró cómo cierra el curso, semana por semana: S12 (APIs REST), S13 (Deployment — al final de esa clase se presenta el enunciado de Caso Práctico #2), S14 (defensa del proyecto — 9 grupos por sección, todos exponen, ~20 minutos por grupo) y S15 (entrega de Caso Práctico #2 + artículo IEEE a mediados de semana, cierre de portafolio y notas al final).
