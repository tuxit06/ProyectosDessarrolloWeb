# Bibliografía de repaso — Clase 12: APIs REST, constantes de rol y JWT

Este documento es para releer después de la clase, con calma, usando como referencia el proyecto `cursosapp` y las slides que ya tenés. No repite el paso a paso del lab (eso está en `lab_clase12.md`) — acá el objetivo es que entiendas **por qué** funciona cada cosa.

---

## 0. Qué es una API REST, y por qué HTTP es "sin estado"

Una API es un contrato: URLs que representan recursos (`/api/cursos`, `/api/cursos/3`), y verbos HTTP que definen la acción sobre ese recurso (`GET` leer, `POST` crear, `PUT` actualizar, `DELETE` eliminar).

HTTP es **stateless** por diseño: el servidor no recuerda nada entre un request y el siguiente, salvo que algo se lo recuerde artificialmente. Para las vistas HTML, ese "algo" es la cookie de sesión (`JSESSIONID`) que ya usás desde S10. Para una API, en vez de simular estado con cookies, cada request trae su propia credencial — hoy ese "algo" va a ser un JWT (ver sección 6).

Los verbos HTTP tienen semántica esperada, aunque Spring no la fuerce por sí solo:
- **`GET` es "seguro"** — no debería modificar nada en el servidor.
- **`PUT` y `DELETE` son "idempotentes"** — repetir el mismo request no cambia el resultado más allá de la primera vez (borrar el mismo id dos veces deja el mismo estado que borrarlo una vez).
- **`POST` no es ninguna de las dos cosas** — cada `POST` puede crear un recurso nuevo distinto.

Los códigos de estado HTTP vienen agrupados por familia: `2xx` éxito, `3xx` redirección, `4xx` error del cliente (el request está mal armado o no tiene permiso), `5xx` error del servidor. En este lab usaste `200`, `201`, `204` (éxito) y `400`, `401`, `403`, `404` (error del cliente).

**¿Qué significa REST?** Las siglas vienen de **REpresentational State Transfer**, un término acuñado por Roy Fielding en el año 2000. No es un protocolo ni una librería — es un **estilo** para diseñar APIs. "Representational" (representacional) porque el servidor nunca manda el recurso en sí (la fila de la base de datos): manda una **representación** de su estado, en este caso JSON. Las tres reglas de arriba (URLs=recursos, HTTP sin estado, verbos con semántica) son justamente los principios de ese estilo — REST es el nombre formal de algo que ya construiste. Una API "RESTful" es, en el fondo, una que sigue estas reglas de forma consistente.

---

## 1. De vistas HTML a JSON — dos formas de responder

Hasta S11, `CursoController` era un `@Controller`: cada método devuelve un `String` con el nombre de una vista Thymeleaf, y Spring se encarga de renderizar HTML completo.

`CursoRestController` es distinto: es un `@RestController`, que equivale a `@Controller` + `@ResponseBody` en **todos** sus métodos. Eso significa que lo que devuelve cada método no es el nombre de una vista — es el objeto en sí, que Spring convierte automáticamente a JSON (usando la librería Jackson, incluida en `spring-boot-starter-webmvc`).

Las dos clases conviven en el mismo proyecto, sirviendo los mismos datos de dos formas distintas: `/cursos` para navegadores, `/api/cursos` para cualquier cliente que hable JSON.

**¿Qué es JSON exactamente, y quién hace la conversión?** JSON (JavaScript Object Notation) es un formato de texto plano para representar datos: pares clave-valor, listas, objetos anidados. Pese al nombre, es independiente del lenguaje — cualquier lenguaje moderno lo lee y escribe. Ejemplo:

```json
{
  "id": 3,
  "nombre": "Fundamentos Web",
  "profesor": { "id": 1, "nombre": "Ana Lopez" }
}
```

La conversión Java ↔ JSON la hace **Jackson**, una librería que `spring-boot-starter-webmvc` ya trae incluida y configurada. Cuando un método de `@RestController` devuelve un objeto Java, Jackson lo **serializa** a JSON antes de mandar la respuesta. Cuando usás `@RequestBody`, Jackson hace el camino inverso: **deserializa** el JSON entrante y arma el objeto Java. Nunca la llamás directamente — por eso un `List<Curso>` se convierte "solo" en el JSON que viste al probar `/api/cursos`.

---

## 2. `@RequestBody` + `@Valid` — recibir y validar JSON

```java
@PostMapping
public ResponseEntity<Curso> crear(@Valid @RequestBody Curso curso) { ... }
```

`@RequestBody` le dice a Spring: "el body de este POST es JSON, convertilo a un objeto `Curso`". `@Valid` activa las validaciones Bean Validation que ya tenía la entidad `Curso` (`@NotBlank`, `@NotNull`, `@Min`, etc., si las agregaste en clases anteriores) — si algo no cumple, Spring devuelve automáticamente un 400 Bad Request, sin que el método `crear()` llegue siquiera a ejecutarse.

---

## 3. `ResponseEntity` — el código de estado importa

Un `@Controller` normal siempre devuelve 200 (o una redirección). Una API REST necesita ser más precisa:

| Situación | Código | Cómo se logra |
|---|---|---|
| Recurso encontrado | 200 OK | `ResponseEntity.ok(curso)` |
| Recurso creado | 201 Created | `ResponseEntity.created(location).body(curso)` |
| Recurso eliminado | 204 No Content | `ResponseEntity.noContent().build()` |
| Recurso no encontrado | 404 Not Found | `ResponseEntity.notFound().build()` |

`ResponseEntity<T>` envuelve el body Y el código de estado (y también permite headers custom) — por eso los métodos de la API lo usan como tipo de retorno en vez de devolver `Curso` directo.

---

## 4. El header `Location` en el 201 Created

```java
URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(guardado.getId())
        .toUri();
return ResponseEntity.created(location).body(guardado);
```

Es una convención REST bien establecida: cuando creás un recurso, la respuesta 201 debería incluir un header `Location` con la URL donde ese recurso nuevo puede consultarse (`GET`). `ServletUriComponentsBuilder.fromCurrentRequest()` arma esa URL automáticamente a partir del request actual (`/api/cursos`), agregando el `id` recién generado.

---

## 5. Por qué `listar()` usa `listarConProfesor()` — el callback a S9

En S9 aprendiste que `Curso.profesor` es `@ManyToOne(fetch = FetchType.LAZY)`, y que acceder a esa relación fuera de una sesión activa de Hibernate produce el problema N+1 (o un error de proxy sin resolver). Para las vistas HTML lo resolviste con `JOIN FETCH` en `listarConProfesor()`.

Para la API pasa exactamente lo mismo, pero el síntoma cambia: en vez de un error al renderizar HTML, Jackson falla al intentar serializar un proxy Hibernate no inicializado a JSON. La solución es la misma: usar el método con `JOIN FETCH` en vez del que trae `profesor` sin resolver.

**Conclusión importante:** el problema de LAZY loading no es exclusivo de las vistas Thymeleaf — aparece en cualquier lugar donde se intente "leer" una relación LAZY fuera de una transacción activa, incluida la serialización JSON.

---

## 6. CORS — solo afecta navegadores

```java
config.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:5500"));
```

CORS (Cross-Origin Resource Sharing) es una restricción que aplican **los navegadores**, no los servidores. Cuando JavaScript corriendo en una página de un origen (protocolo + dominio + puerto) intenta llamar a una API en otro origen, el navegador primero verifica si el servidor autoriza ese origen específico — si no, bloquea la respuesta antes de que tu código JavaScript pueda leerla.

**Punto clave para no confundirse:** Postman, curl, o cualquier programa que no sea un navegador ejecutando JavaScript de una página web **no están sujetos a CORS**. Si tu API funciona en Postman pero un frontend en el navegador tira error de CORS en la consola, el problema no es tu API en sí — es que falta autorizar ese origen específico.

---

## 7. Por qué `/api/**` empezó pública, y por qué eso cambió en la Parte E

Al principio de la clase (Partes A-C), dejar `/api/**` pública fue una decisión de alcance temporal: permite entender `@RestController`/JSON/CORS sin la complejidad de tokens desde el minuto uno. Eso NO es la decisión final — en la Parte E la reemplazaste por JWT, exactamente porque en un proyecto real no dejarías una API de escritura (POST/PUT/DELETE) completamente pública.

---

## 8. Tabla de constantes de rol — el enum `Rol`

Antes de esta clase, el rol de un usuario era un `String` suelto ("ADMIN", "USER") escrito a mano en varios lugares. Nada impedía un typo ("Admin", "ADMN") que la aplicación aceptaba sin quejarse — ese usuario quedaba con un rol que nunca iba a coincidir con ningún `hasRole(...)`.

El enum `Rol { ADMIN, USER }` no cambia cómo funciona `@PreAuthorize("hasRole('ADMIN')")` (esa expresión SpEL sigue siendo un string) — pero sí evita el error en cualquier código Java que use `Rol.ADMIN` en vez de escribir el string a mano, y sirve como fuente única de verdad sobre qué roles existen (por eso `RolRestController` los expone vía `GET /api/roles` — útil para armar un `<select>` en un formulario de creación de usuarios, por ejemplo). `UsuarioService.validarRol()` usa ese mismo enum para rechazar un rol inválido con un error claro, en vez de guardarlo silenciosamente.

---

## 9. JWT (JSON Web Token) — la estructura y el flujo completo

Un JWT (JSON Web Token) tiene tres partes separadas por puntos, cada una en Base64: `header.payload.signature`.

- **Header:** qué algoritmo de firma se usó (en este lab, HMAC-SHA256).
- **Payload (claims):** los datos que el servidor decidió incluir — acá, el `username` (como *subject*) y el `rol`.
- **Signature:** una firma criptográfica calculada con una clave secreta que solo el servidor conoce. Si alguien modifica el payload sin volver a firmarlo con esa clave, la firma deja de coincidir y el servidor rechaza el token.

El flujo completo que implementaste:

1. `POST /api/auth/login` con `username`+`password` → `AuthController` reutiliza el `AuthenticationManager` de Spring Security (el mismo mecanismo interno que usa `formLogin()`) para validar la credencial.
2. Si es válida, `JwtService.generarToken(...)` arma el JWT con el username y el rol, firmado y con expiración (1 hora en este lab).
3. El cliente guarda ese token y lo manda en cada request futuro: header `Authorization: Bearer <token>`.
4. `JwtAuthFilter` (un filtro que corre en cada request) lee ese header, valida la firma y la expiración, y si todo está bien arma un `Authentication` — el mismo tipo de objeto que `formLogin()` deja en el `SecurityContext` para una sesión de navegador.
5. `@PreAuthorize("hasRole('ADMIN')")` en `CursoRestController` evalúa ese `Authentication` exactamente igual que lo hace en `CursoController` para las vistas HTML — no le importa si vino de una sesión o de un JWT.

**Detalle de seguridad para tener en cuenta:** si alguien roba un JWT válido, puede seguir usándolo hasta que expire, aunque cambies tu contraseña — a diferencia de invalidar una sesión del lado del servidor (que sí se puede hacer al instante). Por eso la expiración corta (1 hora) es una decisión de seguridad, no solo un detalle técnico.

---

## 10. Bonus — probar la API sin Postman (springdoc-openapi)

**springdoc-openapi** (dependencia `springdoc-openapi-starter-webmvc-ui`) lee automáticamente las clases `@RestController` de tu proyecto y genera documentación OpenAPI más una página interactiva en `/swagger-ui.html`, donde podés ver y probar cada endpoint desde el navegador sin instalar nada aparte — es el equivalente de Spring Boot a "Swagger". No se usó en este lab (Postman alcanza), pero vale la pena conocerlo para proyectos propios: documentación y pruebas "gratis" a partir del mismo código.

---

## 11. Repaso rápido — dudas frecuentes

| Duda | Respuesta |
|---|---|
| ¿`@RestController` reemplaza a `@Controller`? | No — conviven. Usás `@Controller` cuando querés devolver vistas HTML, `@RestController` cuando querés devolver JSON. `cursosapp` tiene ambos. |
| ¿Por qué mi POST devuelve 400 y no entiendo por qué? | Revisá el JSON del body — probablemente falta un campo validado, o el `profesor` no trae un `id` válido. |
| ¿Postman necesita estar logueado para llamar la API? | Sí, desde la Parte E — hay que pedir un JWT primero (`POST /api/auth/login`) y mandarlo en cada request (`Authorization: Bearer <token>`). |
| ¿Necesito manejar CSRF para llamar la API con Postman? | No manejás nada vos — pero el servidor SÍ necesita tener CSRF deshabilitado para `/api/**` (`.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))` en `SecurityConfig`, PASO E.4). CSRF y JWT son cosas distintas: CSRF protege formularios con sesión/cookie, JWT es la forma de autenticarse sin sesión. Si esa línea falta, el login de la API falla con un 405 confuso en vez de un error claro — ver "Problemas comunes" en `lab_clase12.md`. |
| Mi frontend en React no puede llamar la API, dice error de CORS | Revisá que el origen exacto (con el puerto) esté en `setAllowedOrigins(...)` de `CorsConfig`, y que `.cors(cors -> {})` esté activo en `SecurityConfig`. |
| ¿Por qué `listar()` no puede usar el mismo método que ya tenía el `CursoController`? | Sí puede, pero ese método (`listar()` a secas) no resuelve `profesor` — por eso la API usa `listarConProfesor()`, igual que la vista HTML desde S9. |
| ¿`hasRole('ADMIN')` deja de funcionar si uso el enum `Rol`? | No — siguen siendo compatibles. El enum ayuda en el código Java (evita typos), pero `@PreAuthorize` sigue leyendo un string. |
| ¿Qué pasa si mi JWT expiró? | El request falla (401) y hay que volver a pedir un token con `POST /api/auth/login` — no hay renovación automática en este lab. |

---

## Para seguir leyendo

| Tema | Enlace |
|---|---|
| Spring — Building a RESTful Web Service | https://spring.io/guides/gs/rest-service |
| Baeldung — Exploring the New Spring Boot 3 HTTP Interface | https://www.baeldung.com/spring-boot-restcontroller-controller |
| Spring — CORS Support | https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html |
| Baeldung — Spring Boot Bean Validation | https://www.baeldung.com/spring-boot-bean-validation |
| MDN — Cross-Origin Resource Sharing (CORS) | https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS |
| jwt.io — Introduction to JSON Web Tokens | https://jwt.io/introduction |
| Baeldung — JWT with Spring Security | https://www.baeldung.com/spring-security-oauth-jwt |
| springdoc-openapi — documentación oficial | https://springdoc.org/ |
