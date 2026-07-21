# Bibliografía de repaso — Clase 12: APIs REST (JSON)

Este documento es para releer después de la clase, con calma, usando como referencia el proyecto `cursosapp` y las slides que ya tenés. No repite el paso a paso del lab (eso está en `lab_clase12.md`) — acá el objetivo es que entiendas **por qué** funciona cada cosa.

---

## 1. De vistas HTML a JSON — dos formas de responder

Hasta S11, `CursoController` era un `@Controller`: cada método devuelve un `String` con el nombre de una vista Thymeleaf, y Spring se encarga de renderizar HTML completo.

`CursoRestController` es distinto: es un `@RestController`, que equivale a `@Controller` + `@ResponseBody` en **todos** sus métodos. Eso significa que lo que devuelve cada método no es el nombre de una vista — es el objeto en sí, que Spring convierte automáticamente a JSON (usando la librería Jackson, incluida en `spring-boot-starter-webmvc`).

Las dos clases conviven en el mismo proyecto, sirviendo los mismos datos de dos formas distintas: `/cursos` para navegadores, `/api/cursos` para cualquier cliente que hable JSON.

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

## 7. Por qué `/api/**` queda público en este lab

Es una decisión de alcance, no un descuido: proteger una API sin sesión (donde el cliente no es un navegador con cookies) normalmente requiere un esquema distinto a `formLogin()` — típicamente tokens (JWT) o API keys, enviados en cada request vía header `Authorization`. Ese tema queda fuera del alcance de este curso; lo que importa hoy es entender `@RestController`/JSON/CORS. En un proyecto real, no dejarías una API de escritura (POST/PUT/DELETE) completamente pública.

---

## 8. Repaso rápido — dudas frecuentes

| Duda | Respuesta |
|---|---|
| ¿`@RestController` reemplaza a `@Controller`? | No — conviven. Usás `@Controller` cuando querés devolver vistas HTML, `@RestController` cuando querés devolver JSON. `cursosapp` tiene ambos. |
| ¿Por qué mi POST devuelve 400 y no entiendo por qué? | Revisá el JSON del body — probablemente falta un campo validado, o el `profesor` no trae un `id` válido. |
| ¿Postman necesita estar logueado para llamar la API? | No, en este lab `/api/**` es público. Si más adelante se protegiera, necesitaría un mecanismo distinto al login por formulario (JWT, API key). |
| Mi frontend en React no puede llamar la API, dice error de CORS | Revisá que el origen exacto (con el puerto) esté en `setAllowedOrigins(...)` de `CorsConfig`, y que `.cors(cors -> {})` esté activo en `SecurityConfig`. |
| ¿Por qué `listar()` no puede usar el mismo método que ya tenía el `CursoController`? | Sí puede, pero ese método (`listar()` a secas) no resuelve `profesor` — por eso la API usa `listarConProfesor()`, igual que la vista HTML desde S9. |

---

## Para seguir leyendo

| Tema | Enlace |
|---|---|
| Spring — Building a RESTful Web Service | https://spring.io/guides/gs/rest-service |
| Baeldung — Exploring the New Spring Boot 3 HTTP Interface | https://www.baeldung.com/spring-boot-restcontroller-controller |
| Spring — CORS Support | https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html |
| Baeldung — Spring Boot Bean Validation | https://www.baeldung.com/spring-boot-bean-validation |
| MDN — Cross-Origin Resource Sharing (CORS) | https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS |
