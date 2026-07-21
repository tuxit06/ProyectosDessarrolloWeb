# Bibliografía de repaso — Clase 11: Spring Security 2 (autorización, usuarios y sesiones)

Este documento es para releer después de la clase, con calma, usando como referencia el proyecto `cursosapp` y las slides que ya tenés. No repite el paso a paso del lab (eso está en `lab_clase11.md`) — acá el objetivo es que entiendas **por qué** funciona cada cosa.

---

## 1. De autenticación a autorización

La clase pasada (S10) resolviste "¿quién sos?" — login, logout, contraseñas seguras. Hoy resolviste la pregunta que quedaba pendiente: "¿qué podés hacer?"

Hasta ahora, cualquier usuario logueado (`admin`, `profesor` o `estudiante`) podía crear, editar y eliminar cursos por igual. Eso ya no es así: solo `ADMIN` puede modificar datos.

---

## 2. `@EnableMethodSecurity` — sin esto, `@PreAuthorize` no hace nada

`@PreAuthorize` es una anotación que necesita que Spring la "escuche". Ese interruptor es `@EnableMethodSecurity`, en `SecurityConfig`:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // sin esto, @PreAuthorize se ignora sin avisar
public class SecurityConfig { ... }
```

**El detalle más importante de la clase:** si te olvidás esta anotación, no hay ningún error. La app arranca normal, pero `@PreAuthorize` nunca se evalúa — cualquiera puede hacer cualquier cosa, como si no hubieras escrito nada. Es el bug más fácil de cometer y más difícil de notar en esta clase, así que si algo no bloquea como esperás, este es el primer lugar para revisar.

---

## 3. `@PreAuthorize("hasRole('ADMIN')")` — cómo se lee

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/nuevo")
public String mostrarFormNuevo(Model modelo) { ... }
```

Se lee: "antes de ejecutar este método, verificá que el usuario logueado tenga el rol ADMIN. Si no lo tiene, no ejecutes el método — lanzá una excepción en su lugar."

Esa excepción (`AccessDeniedException`) es la que `SecurityConfig` intercepta y redirige a `/403`.

**¿De dónde sale ese texto entre paréntesis?** No es un mini-lenguaje inventado para Spring Security — es **SpEL** (Spring Expression Language), el mismo lenguaje de expresiones que usa Thymeleaf por debajo en cosas como `th:if="${curso.creditos > 3}"`. `@PreAuthorize` recibe un `String` con una expresión SpEL y Spring la evalúa antes de correr el método. `hasRole(...)`, `hasAuthority(...)`, `isAuthenticated()`, `isAnonymous()` son funciones que esa expresión puede llamar. Esto explica por qué `sec:authorize="hasRole('ADMIN')"` en un `.html` (Thymeleaf) se escribe IGUAL que `@PreAuthorize("hasRole('ADMIN')")` en un `.java`: es el mismo lenguaje en los dos lugares, no una coincidencia de sintaxis.

---

## 4. `hasRole()` vs `hasAuthority()` — dos formas, mismo resultado

En el lab, `CursoController.eliminar()` usa `hasAuthority("ROLE_ADMIN")` en vez de `hasRole("ADMIN")`. Son **exactamente equivalentes**:

- `hasRole("ADMIN")` — Spring agrega el prefijo `ROLE_` por vos. Termina evaluando `ROLE_ADMIN`.
- `hasAuthority("ROLE_ADMIN")` — vos escribís el string completo, con el prefijo incluido. También evalúa `ROLE_ADMIN`.

No hay ninguna diferencia de comportamiento — es la misma regla escrita de dos formas distintas. El lab usa ambas a propósito para que lo veas funcionar igual en los dos casos.

---

## 5. Por qué `listar()` y `detalle()` no tienen `@PreAuthorize`

Es una decisión de diseño: separar **lectura** (cualquier usuario logueado puede ver los cursos) de **escritura** (solo ADMIN puede modificarlos). Es un patrón común: pensá en cualquier sistema donde todos pueden consultar información, pero solo algunos pueden cambiarla.

---

## 6. La página 403

Cuando `@PreAuthorize` bloquea a alguien, Spring Security redirige a la URL que configuraste con `accessDeniedPage("/403")`. Armaste:

- Una ruta `GET /403` en `HomeController`.
- Un template `403.html` con el mensaje y un botón para volver.

Sin esto, verías la pantalla de error genérica (blanca, técnica) de Spring Boot en vez de una página con el estilo de tu app.

---

## 7. `sec:authorize` sigue siendo solo cosmético (repaso de S10)

En `cursos.html` agregaste `sec:authorize="hasRole('ADMIN')"` a los botones de crear/editar/eliminar. Esto **oculta el botón en el HTML**, pero no protege nada por sí solo.

Prueba mental (y prueba real, si querés hacerla): logueate como `estudiante`, no vas a ver el botón "Nuevo curso" — pero si escribís `/cursos/nuevo` directo en la barra de direcciones, `@PreAuthorize` te sigue bloqueando y te manda a `/403`. Esa es la prueba de que la protección real vive en el backend, no en el botón oculto.

**Regla general de seguridad (no solo de Spring):** nunca confiar en que el frontend "no muestra" algo como si fuera protección. Cualquiera puede escribir la URL a mano, o usar Postman.

---

## 8. Logout más completo

Agregaste `invalidateHttpSession(true)` y `deleteCookies("JSESSIONID")` al logout. No cambia lo que ves en pantalla, pero es más prolijo: invalida la sesión en el servidor de forma explícita y le dice al navegador que borre la cookie vieja, en vez de dejarla dando vueltas sin uso.

---

## 9. 401 vs 403 — dos preguntas distintas

Cuando algo bloquea un request, hay dos preguntas posibles y Spring Security usa un mecanismo distinto para cada una:

- **"¿Quién sos?"** — si NO estás logueado y pedís algo protegido, responde el `AuthenticationEntryPoint` (código HTTP 401 en teoría; en este proyecto lo ves como una redirección a `/login`, porque `formLogin()` lo configura así). Ya lo tenías desde S10, aunque no se había nombrado.
- **"Ya sé quién sos, ¿podés hacer esto?"** — si SÍ estás logueado pero tu rol no alcanza, responde el `AccessDeniedHandler` (código HTTP 403). Es exactamente lo que armaste hoy con `exceptionHandling(ex -> ex.accessDeniedPage("/403"))`.

Truco para no confundirte: 401 = "no sé quién sos", 403 = "sé quién sos, pero no podés".

---

## 10. Por qué existe un CRUD de Usuarios

Desde S10, los 3 usuarios de prueba (`admin`, `profesor`, `estudiante`) existían porque se insertaron a mano con SQL (`seed-data.sql`). Eso no escala en una app real: alguien necesita poder crear, editar y dar de baja usuarios desde la aplicación misma, sin tocar la base de datos directamente.

`UsuarioController` usa el mismo patrón MVC que `CursoController` (Entity → Repository → Service → Controller → Template), pero con una diferencia importante: la anotación `@PreAuthorize("hasRole('ADMIN')")` va a nivel de **clase**, no de método. Eso significa que ni siquiera `listar()` es de lectura abierta acá — a diferencia de cursos, donde cualquier autenticado puede ver la lista, en usuarios ni ADMIN puede ver esa pantalla si no es ADMIN. Tiene sentido: la lista de usuarios (con sus roles) es información más sensible que la lista de cursos.

También notaste que `eliminar()` no deja que un admin se borre a sí mismo. Eso no es un rol ni un permiso — es una validación de lógica de negocio dentro del método (`usuario.getUsername().equals(auth.getName())`). La seguridad de una app no es solo "roles y permisos": también incluye reglas defensivas como esta.

---

## 11. Manejo de sesiones

Una `HttpSession` es el mecanismo con el que el servidor "recuerda" que ya te autenticaste, normalmente mediante una cookie (`JSESSIONID`).

- **Fijación de sesión (session fixation):** un ataque donde alguien te hace usar un `sessionId` que ya conoce de antemano, para heredar tu sesión una vez que te logueás. Spring Security ya te protege de esto automáticamente — regenera el `sessionId` en cada login exitoso, sin que tengas que configurar nada.
- **Sesiones concurrentes:** por default, un mismo usuario puede tener varias sesiones activas a la vez (ej. logueado en el celular y en la laptop al mismo tiempo). `sessionManagement().maximumSessions(1)` limita eso a una sola sesión.
- `maxSessionsPreventsLogin(false)` decide qué pasa cuando alguien ya logueado en un dispositivo se loguea en otro: en vez de bloquear el login nuevo, invalida la sesión vieja. (La alternativa, `true`, haría lo contrario: bloquear el login nuevo hasta cerrar la sesión vieja.)
- El bean `HttpSessionEventPublisher` es "plomería" necesaria: sin él, Spring Security no se entera cuándo una sesión realmente termina (por ejemplo, cuando cerrás la pestaña sin hacer logout), y el conteo de sesiones activas queda mal.

---

## 12. Recuperación de contraseña — el flujo completo

El flujo que armaste: el usuario pide recuperación con su email → se genera un token de un solo uso (`UUID`) con expiración de 30 minutos → se manda un correo con un link que incluye ese token → al hacer click, se valida el token → si es válido y no venció, el usuario elige una contraseña nueva → el token se invalida (no sirve una segunda vez).

**El detalle de seguridad más importante:** el mensaje que ves en `/olvide-password` es EXACTAMENTE el mismo, exista o no ese email en la base de datos. Esto se hace a propósito, para evitar la **enumeración de cuentas** — si el mensaje fuera distinto ("no encontramos ese email" vs "revisá tu correo"), cualquiera podría usar el formulario para descubrir qué emails están registrados en el sistema, probando uno por uno. Es un error de seguridad real y común, no una exageración académica.

Para la demo/prueba en clase se usa **Mailtrap** (un servidor SMTP de prueba, con un inbox compartido para todo el grupo) en vez de un proveedor de correo real. Nunca uses credenciales de un correo personal en un proyecto de clase.

---

## 13. Repaso rápido — dudas frecuentes

| Duda | Respuesta |
|---|---|
| Agregué `@PreAuthorize` pero sigue sin bloquear nada | Revisá que `@EnableMethodSecurity` esté descomentado en `SecurityConfig`. Es el error más común. |
| `hasRole("ADMIN")` y `hasAuthority("ROLE_ADMIN")`, ¿cuál debería usar en mi proyecto? | Cualquiera de las dos — dan el mismo resultado. `hasRole` es más corto de escribir. |
| Oculté el botón con `sec:authorize` pero no agregué `@PreAuthorize` en el Controller, ¿está protegido? | No. Cualquiera puede llamar la URL directo sin pasar por el botón. `sec:authorize` es solo visual. |
| ¿Por qué puedo ver la lista de cursos como `estudiante` pero no crear uno? | Porque `listar()` no tiene `@PreAuthorize` (lectura abierta), pero `mostrarFormNuevo()` sí (escritura restringida a ADMIN). |
| ¿Qué pasa si quito `/403` de la lista de rutas públicas? | Entrar a `/403` también exigiría estar logueado — puede generar comportamiento raro si alguien llega ahí sin sesión. Mejor dejarlo público. |
| ¿Por qué `/usuarios` está más restringido que `/cursos` (ni siquiera lectura libre)? | Decisión de diseño: la lista de usuarios y roles es más sensible que la de cursos, así que se protegió a nivel de clase completa. |
| Edité un usuario sin tocar la contraseña y dejó de poder loguearse | El service tiene que preservar el password existente si el campo llega vacío del formulario, en vez de hashear un string vacío. |
| El correo de recuperación nunca llega | Revisá las variables de entorno de Mailtrap y que `/olvide-password` y `/restablecer-password` estén en `permitAll()`. |
| Logueo el mismo usuario en dos navegadores y el primero no se cierra | Falta el bean `HttpSessionEventPublisher` — sin él, `maximumSessions` no cuenta bien las sesiones activas. |

---

## Para seguir leyendo

| Tema | Enlace |
|---|---|
| Spring Security — Method Security (`@PreAuthorize`) | https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html |
| Baeldung — Intro to Spring Security Expressions | https://www.baeldung.com/spring-security-expressions |
| Baeldung — Spring Security Custom Access Denied Page | https://www.baeldung.com/spring-security-custom-access-denied-page |
| GeeksforGeeks — hasRole() vs hasAuthority() | https://www.geeksforgeeks.org/advance-java/difference-between-hasrole-and-hasauthority-in-spring-security/ |
| Spring Security — Session Management | https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html |
| Baeldung — Spring Boot Sending Email | https://www.baeldung.com/spring-email |
| Mailtrap — Email Testing | https://mailtrap.io/ |
| OWASP — Forgot Password Cheat Sheet | https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html |
