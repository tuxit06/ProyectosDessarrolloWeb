# Lab Clase 11 — Spring Security 2 (autorización, usuarios y sesiones)

## Información general

| Dato | Valor |
|------|-------|
| Curso | SC-403 Desarrollo de Aplicaciones Web y Patrones |
| Universidad | Fidélitas |
| Modalidad | En clase, guiado por el profesor |
| Evaluación | Ninguna (el lab en sí) |
| Tiempo estimado | 100 minutos |

---

## Propósito

Desde S10, `cursosapp` exige login — pero cualquier usuario logueado, sea `admin`, `profesor` o `estudiante`, puede crear, editar y eliminar cursos por igual. Eso no está bien: en la mayoría de las apps reales, solo ciertos roles pueden modificar datos.

En este lab cerrás el círculo de seguridad del proyecto en cuatro frentes:

1. **Autorización por rol** con `@PreAuthorize`: solo `ADMIN` modifica cursos.
2. **Página de acceso denegado (403)** propia, en vez del error en blanco de Spring Boot.
3. **Gestión de usuarios** (CRUD): hasta ahora los usuarios solo existían por seed SQL — hoy el `ADMIN` los administra desde la propia aplicación, con el mismo patrón MVC + `@PreAuthorize` que ya usaste para cursos.
4. **Sesiones y recuperación de contraseña**: qué pasa cuando el mismo usuario se loguea desde dos navegadores, y qué pasa cuando se olvida la contraseña — con Spring Mail de por medio.

Los primeros dos temas son "roles y permisos" (lo que el programa oficial del curso llama Unidad 4 - Semana 10). Los últimos dos son "correo y usuarios" (Unidad 4 - Semana 11 del programa). Esta clase junta ambos bloques.

---

## Objetivos de aprendizaje

Al terminar este lab vas a haber demostrado que sos capaz de:

1. Habilitar `@PreAuthorize` en un proyecto Spring Boot con `@EnableMethodSecurity`, y explicar qué es SpEL (Spring Expression Language) y por qué `hasRole(...)` es una expresión, no una anotación mágica.
2. Restringir métodos de un Controller por rol con `@PreAuthorize`, tanto a nivel de método como a nivel de clase.
3. Distinguir `hasRole()` de `hasAuthority()`, y distinguir un `AuthenticationEntryPoint` (401, no logueado) de un `AccessDeniedHandler` (403, logueado pero sin permiso).
4. Configurar una página de acceso denegado (403) propia.
5. Usar `sec:authorize` para ocultar botones según el rol, entendiendo que es solo cosmético.
6. Construir un CRUD completo de usuarios protegido con `@PreAuthorize` a nivel de clase, reutilizando el mismo patrón MVC del resto del curso.
7. Configurar control de sesiones concurrentes (`maximumSessions`) y explicar qué es la fijación de sesión (session fixation).
8. Implementar un flujo de recuperación de contraseña con tokens de un solo uso y Spring Mail, y explicar por qué nunca hay que revelar si un email existe en el sistema.

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

Logueate con `estudiante` / `estudiante123` y confirmá que HOY podés crear/editar/eliminar cursos sin ninguna restricción — eso es lo que cambia en la Parte B.

**Para las Partes F y G** vas a necesitar las credenciales de un inbox de Mailtrap (SMTP de prueba) — el profesor las comparte en clase. No hace falta cuenta propia: todo el grupo manda correos al mismo inbox compartido, así que van a ver los correos de sus compañeros también (es solo para practicar, no hay nada privado ahí).

---

## Cómo está organizado el código pre-comentado

Igual que en S9 y S10: bloques `// CLASE 11 - PASO X.Y` (o `<!-- CLASE 11 - PASO X.Y -->` en HTML) dentro de archivos que YA existen, listos para descomentar. Para los archivos completamente nuevos de las Partes F y G (`UsuarioService.java`, `EmailService.java`, `UsuarioController.java`, `PasswordResetController.java`), vas a encontrar un `package-info.md` dentro del paquete correspondiente (`service/` o `controller/`) con el código para copiar — el mismo patrón que usaste en S9 para crear `Profesor.java`. Las plantillas HTML nuevas (`usuarios.html`, `usuarios/form.html`, `olvide-password.html`, `restablecer-password.html`) ya vienen completas, como `cursos.html` desde S6 — no son código Java, así que no hay riesgo de romper la compilación si las tenés desde el inicio.

---

## Parte A — Habilitar `@PreAuthorize`

1. Abrí `config/SecurityConfig.java`. Descomentá el import de `EnableMethodSecurity` (**PASO A.1**) y la anotación `@EnableMethodSecurity` sobre la clase.
2. Sin este paso, `@PreAuthorize` en la Parte B se va a ignorar silenciosamente — no da error, simplemente no protege nada. Es el error más común de esta clase, así que confirmá este paso antes de seguir.

**Para pensar:** `@PreAuthorize("hasRole('ADMIN')")` no es una anotación con lógica propia — es una expresión de SpEL (Spring Expression Language) que Spring Security evalúa ANTES de ejecutar el método (de ahí el "Pre"). `hasRole(...)` es una función que esa expresión puede llamar, no una palabra reservada. Ver `bibliografia_clase11.md` para más detalle.

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

**Para pensar:** cuando alguien SIN loguearse entra a `/cursos`, Spring Security responde con un `AuthenticationEntryPoint` (lo manda a `/login` — error 401, "no sabemos quién sos"). Cuando alguien YA logueado pero sin el rol correcto entra a `/cursos/nuevo`, responde con un `AccessDeniedHandler` (lo manda a `/403` — error 403, "sabemos quién sos, pero no podés"). Son dos mecanismos distintos para dos preguntas distintas: "¿quién sos?" vs "¿qué podés hacer?". Ver `bibliografia_clase11.md`.

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

## Parte F — CRUD de Usuarios (solo ADMIN)

Hasta ahora los 3 usuarios de prueba (`admin`, `profesor`, `estudiante`) solo existen porque los insertaste a mano con `seed-data.sql`. En una app real, alguien tiene que poder crear, editar y eliminar usuarios desde la propia aplicación — típicamente el `ADMIN`. Vas a construir exactamente ese CRUD, reutilizando el mismo patrón MVC + `@PreAuthorize` de las Partes A-B, aplicado a un dominio nuevo.

1. **PASO F.1** — En `entity/Usuario.java`, descomentá los campos `email`, `resetToken` y `resetTokenExpiracion` (y sus imports), más los 3 getters/setters correspondientes al final de la clase. `email` lo usás en esta Parte F (y en la G); `resetToken`/`resetTokenExpiracion` los vas a usar recién en la Parte G, pero como es la misma clase, se descomentan juntos.
2. **PASO F.2** — En `repository/UsuarioRepository.java`, descomentá `findByEmail(...)`.
3. **PASO F.4** — Abrí el `package-info.md` del paquete `service/` y creá `UsuarioService.java` copiando el código de ahí (sección "PARTE F.4").
4. **PASO F.5** — Abrí el `package-info.md` del paquete `controller/` y creá `UsuarioController.java` copiando el código de ahí (sección "PARTE F.5").
5. **PASO F.3** — En `templates/fragments/header.html`, descomentá el link "Usuarios" del navbar (solo visible para `ADMIN`).
6. Reiniciá la app. Con `Usuario.java` ya teniendo el campo `email`, Hibernate agrega la columna sola (`ddl-auto=update`) — pero las filas que ya tenías de S10 quedan con `email` en blanco. Corré el `UPDATE` que está comentado al final de `seed-data.sql` (justo después del `PASO F.1`) para completarlos.
7. Logueate como `admin`, entrá a `/usuarios` y probá crear un usuario nuevo, editarlo (dejando la contraseña en blanco para no cambiarla) y eliminarlo. Confirmá que logueado como `profesor` o `estudiante`, `/usuarios` te manda a la página 403.

**Para pensar:** en `UsuarioController` el `@PreAuthorize("hasRole('ADMIN')")` va UNA VEZ, a nivel de **clase** — no en cada método como en `CursoController`. ¿Por qué la diferencia? En `CursoController`, `listar()` y `detalle()` quedan abiertos a cualquier autenticado (ver cursos es parte del uso normal de la app). En `UsuarioController`, ni siquiera LISTAR usuarios debería ser visible para un `USER` — es información sensible del sistema (usernames, roles, emails de otras personas), no del dominio del curso.

---

## Parte G — Manejo de sesiones y recuperación de contraseña

### G.1 — Sesiones concurrentes

Ahora mismo, si te logueás como `admin` en dos navegadores distintos (o uno normal y uno incógnito), las DOS sesiones quedan activas al mismo tiempo. Vas a limitar eso a una sola sesión por usuario.

1. **PASO G.3** — En `config/SecurityConfig.java`, descomentá el `@Bean httpSessionEventPublisher()` (junto con su import). Sin este bean, Spring Security no se entera cuándo una sesión vieja se cierra de verdad (por ejemplo, cerrando la pestaña del navegador), y el conteo de sesiones activas queda desincronizado.
2. **PASO G.3 (continua)** — En la lista de rutas públicas (`permitAll()`), agregá `"/olvide-password"` y `"/restablecer-password"` (las vas a necesitar en el punto G.2 de más abajo).
3. **PASO G.4** — Descomentá el bloque `.sessionManagement(...)` al final de la cadena — igual que en el PASO C.2, el punto y coma final se mueve a esta línea.
4. Reiniciá la app. Logueate como `admin` en un navegador, y despues logueate como `admin` en OTRO navegador (o uno en incógnito). Volvé al primer navegador y hacé click en cualquier link: deberías caer de nuevo en `/login` — esa sesión quedó invalidada porque `maxSessionsPreventsLogin(false)` prioriza al login más reciente.

**Para pensar:** esto es una defensa parcial contra la **fijación de sesión** (session fixation): un ataque donde alguien consigue que la víctima use un `sessionId` que el atacante ya conoce, para después "heredar" esa sesión ya autenticada. Spring Security ya regenera el `sessionId` automáticamente en cada login (protección por defecto, no la tuviste que configurar) — `maximumSessions` es una capa adicional sobre eso. Ver `bibliografia_clase11.md`.

### G.2 — Recuperación de contraseña con Spring Mail

1. **PASO G.2** — En `repository/UsuarioRepository.java`, descomentá `findByResetToken(...)`.
2. **PASO G.8** — Abrí el `package-info.md` de `service/` y creá `EmailService.java` (sección "PARTE G.8").
3. **PASO G.9** — Abrí el `package-info.md` de `controller/` y creá `PasswordResetController.java` (sección "PARTE G.9").
4. **PASO G.5** — En `templates/login.html`, descomentá el link "¿Olvidaste tu contraseña?".
5. Antes de probar, pedile al profesor las credenciales del inbox de Mailtrap compartido y completá `${MAILTRAP_USERNAME}` / `${MAILTRAP_PASSWORD}` como variables de entorno (mismo mecanismo que ya usás para `${MYSQL_PASSWORD}` desde S4).
6. Reiniciá la app. Andá a `/login` → "¿Olvidaste tu contraseña?" → escribí el email de `admin` (`admin@ufide.ac.cr`, si ya corriste el `UPDATE` de la Parte F). Revisá el inbox de Mailtrap: debería llegar un correo con un link. Abrí ese link, poné una contraseña nueva, y logueate con ella.
7. Probá también con un email que NO existe en la base — fijate que el mensaje que te muestra la app es EXACTAMENTE el mismo que con un email real. Eso es a propósito (ver PASO G.6 en `olvide-password.html`).

**Para pensar:** si la app dijera "ese email no existe" cuando el email no está registrado, y "te enviamos un enlace" cuando sí lo está, cualquiera podría usar ese formulario para averiguar qué usuarios existen en el sistema, probando emails uno por uno (enumeración de cuentas). Por eso el mensaje de éxito es idéntico siempre, exista o no el email — ver `PasswordResetController.procesarOlvide()` y `bibliografia_clase11.md`.

---

## Problemas comunes

| Síntoma | Solución |
|---|---|
| `@PreAuthorize` no bloquea nada, cualquier usuario puede crear/editar/eliminar | Falta `@EnableMethodSecurity` en `SecurityConfig` (Parte A). Sin esa anotación, `@PreAuthorize` se ignora sin avisar. |
| En vez de la página 403 propia, aparece la pantalla de error blanca de Spring Boot | Revisá el PASO C.1 (`/403` en `permitAll()`) y el PASO C.2 (`exceptionHandling`). Si `/403` no es público, entrar ahí también pide login y genera un loop. |
| Error de compilación en `SecurityConfig.java` después del PASO C.2 o G.4 | Revisá el punto y coma: tiene que quedar solo UNO, al final de toda la cadena, no después de la línea anterior. |
| El botón "Nuevo curso" no se oculta para `estudiante` | Revisá el PASO E.2 y que el namespace `xmlns:sec` esté en la etiqueta `<html>` de `cursos.html`. |
| `estudiante` sigue pudiendo eliminar cursos aunque no vea el botón | Revisá el PASO B.5 en `CursoController` — la restricción real es el `@PreAuthorize`, no el HTML. |
| `/usuarios` da un error de columna desconocida ("email") | Reiniciá la app DESPUÉS de descomentar el campo `email` en `Usuario.java` (PASO F.1) - Hibernate necesita arrancar para crear la columna con `ddl-auto=update`. |
| Al editar un usuario sin cambiar la contraseña, el login deja de funcionar | Revisá `UsuarioService.actualizar()`: si el campo password llega vacío, NO debe hashearlo ni sobreescribir el existente. |
| El correo de recuperación nunca llega | Revisá las variables de entorno `MAILTRAP_USERNAME`/`MAILTRAP_PASSWORD` y que `/olvide-password` y `/restablecer-password` estén en `permitAll()` (PASO G.3). |
| El link de recuperación siempre dice "invalido o vencido" | Los tokens duran 30 minutos y se usan una sola vez (`UsuarioService.cambiarPassword()` los limpia). Pedí un enlace nuevo. |
| Dos sesiones del mismo usuario siguen activas a la vez | Revisá que el `@Bean httpSessionEventPublisher()` (PASO G.3) y el `.sessionManagement(...)` (PASO G.4) estén los dos descomentados - hace falta el bean para que el conteo de sesiones sea preciso. |

---

## Recursos de consulta

| Tema | Enlace |
|---|---|
| Spring Security — Method Security (`@PreAuthorize`) | https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html |
| Spring Security — Authorization Architecture | https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html |
| Spring Security — Expression-Based Access Control (SpEL) | https://docs.spring.io/spring-security/reference/servlet/authorization/expression-based.html |
| Baeldung — Spring Security Custom Access Denied Page | https://www.baeldung.com/spring-security-custom-access-denied-page |
| Baeldung — Intro to Spring Security Expressions (hasRole/hasAuthority) | https://www.baeldung.com/spring-security-expressions |
| Spring Security — Session Management | https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html |
| Spring Boot — Sending Email (Spring Mail) | https://docs.spring.io/spring-boot/reference/io/email.html |
| Mailtrap — Email Testing | https://mailtrap.io/email-testing/ |
| OWASP — Forgot Password Cheat Sheet | https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html |

---

## Preguntas

Cualquier duda durante el lab podés consultarla en:

- El canal `Consultas` del equipo de Teams.
- Directamente en clase, levantando la mano.

---

## Cronograma de cierre del curso

Al principio de esta clase el profesor mostró cómo cierra el curso, semana por semana: S12 (APIs REST), S13 (Deployment — al final de esa clase se presenta el enunciado de Caso Práctico #2), S14 (defensa del proyecto — 9 grupos por sección, todos exponen, ~20 minutos por grupo) y S15 (entrega de Caso Práctico #2 + artículo IEEE a mediados de semana, cierre de portafolio y notas al final).
