# Bibliografía de repaso — Clase 11: Spring Security 2 (autorización)

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

## 9. Repaso rápido — dudas frecuentes

| Duda | Respuesta |
|---|---|
| Agregué `@PreAuthorize` pero sigue sin bloquear nada | Revisá que `@EnableMethodSecurity` esté descomentado en `SecurityConfig`. Es el error más común. |
| `hasRole("ADMIN")` y `hasAuthority("ROLE_ADMIN")`, ¿cuál debería usar en mi proyecto? | Cualquiera de las dos — dan el mismo resultado. `hasRole` es más corto de escribir. |
| Oculté el botón con `sec:authorize` pero no agregué `@PreAuthorize` en el Controller, ¿está protegido? | No. Cualquiera puede llamar la URL directo sin pasar por el botón. `sec:authorize` es solo visual. |
| ¿Por qué puedo ver la lista de cursos como `estudiante` pero no crear uno? | Porque `listar()` no tiene `@PreAuthorize` (lectura abierta), pero `mostrarFormNuevo()` sí (escritura restringida a ADMIN). |
| ¿Qué pasa si quito `/403` de la lista de rutas públicas? | Entrar a `/403` también exigiría estar logueado — puede generar comportamiento raro si alguien llega ahí sin sesión. Mejor dejarlo público. |

---

## Para seguir leyendo

| Tema | Enlace |
|---|---|
| Spring Security — Method Security (`@PreAuthorize`) | https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html |
| Baeldung — Intro to Spring Security Expressions | https://www.baeldung.com/spring-security-expressions |
| Baeldung — Spring Security Custom Access Denied Page | https://www.baeldung.com/spring-security-custom-access-denied-page |
| GeeksforGeeks — hasRole() vs hasAuthority() | https://www.geeksforgeeks.org/advance-java/difference-between-hasrole-and-hasauthority-in-spring-security/ |
