# Lab Clase 12 — APIs REST, constantes de rol y autenticación JWT

## Información general

| Dato | Valor |
|------|-------|
| Curso | SC-403 Desarrollo de Aplicaciones Web y Patrones |
| Universidad | Fidélitas |
| Modalidad | En clase, guiado por el profesor |
| Evaluación | Ninguna (el lab en sí) |
| Tiempo estimado | 130 minutos |

---

## Propósito

Hasta ahora, `cursosapp` solo habla con navegadores: cada URL devuelve una página HTML completa (Thymeleaf), y cada acción (crear un curso, gestionar usuarios) exige haber iniciado sesión con un formulario. Pero muchas aplicaciones reales necesitan que **otros programas** —una app móvil, un frontend en React, otro servicio— consuman los mismos datos sin recibir HTML, sino **JSON**, y sin depender de una sesión de navegador con cookies.

Esta clase resuelve las cuatro piezas de ese problema:

1. **Cómo responder en JSON** en vez de HTML (`@RestController`, `@RequestBody`, `ResponseEntity`).
2. **Cómo permitir que un frontend en otro origen consuma la API** (CORS).
3. **Cómo evitar strings sueltos para los roles** ("ADMIN", "USER" repetidos y propensos a typos) usando un enum como fuente única de verdad.
4. **Cómo autenticar una API sin sesión** — el patrón que se usa en el mundo real (JWT), en vez de dejarla completamente pública.

---

## Objetivos de aprendizaje

Al terminar este lab vas a haber demostrado que sos capaz de:

1. Explicar qué es una API REST, por qué existen, y qué significa que HTTP sea "sin estado" (stateless).
2. Diferenciar `@RestController` de `@Controller` y entender qué hace `@ResponseBody` por debajo.
3. Recibir JSON en el body de un request con `@RequestBody` + validarlo con `@Valid`.
4. Devolver códigos de estado HTTP explícitos con `ResponseEntity` (200, 201, 204, 400, 401, 403, 404).
5. Configurar CORS para que un frontend en otro origen pueda consumir la API.
6. Usar un enum como tabla de constantes de rol, en vez de strings sueltos.
7. Explicar cómo funciona un JWT (firma, claims, expiración) y por qué es la forma estándar de autenticar una API sin sesión.
8. Implementar un flujo completo de login por JWT: emitir el token, validarlo en cada request, y proteger endpoints con `@PreAuthorize` igual que en las vistas HTML.
9. Probar todo el flujo (login, token, endpoints protegidos) con Postman.

---

## Material entregado

| Archivo/Carpeta | Descripción |
|---|---|
| `cursosapp/` | Proyecto de S11 actualizado (login, roles, `@PreAuthorize`, 403, CRUD de Usuarios, sesiones, recuperación de contraseña), con los archivos nuevos de este lab descritos en `package-info.md`. |
| `postman-collection.json` | Colección base (Home, Cursos, Demo en clase, Despliegue). Vos vas a ir armando tus propias requests de API REST/Autenticación siguiendo el mismo patrón, o pedirle al profesor la colección ampliada de referencia (con las secciones "Autenticación (JWT)" y "API REST (JSON)" ya armadas). |

---

## Antes de empezar

Verificá que el proyecto base arranca sin problemas y que las vistas HTML (`/cursos`, `/usuarios`) siguen funcionando igual que en S11:

```bash
cd cursosapp
mvnw.cmd spring-boot:run     # Windows
./mvnw spring-boot:run       # Linux/Mac
```

Logueate como `admin` / `admin123` y confirmá que el CRUD de cursos y el CRUD de usuarios siguen funcionando. Todo lo de hoy se agrega **al lado**, sin tocar nada de eso.

---

## Cómo está organizado el código pre-comentado

Esta clase agrega **varios archivos completamente nuevos**, así que no hay bloques `// CLASE 12 - PASO X.Y` para descomentar dentro de ellos — el código a copiar está en un `package-info.md` dentro del paquete donde tenés que crear cada archivo. Sí hay pasos comentados dentro de archivos que ya existen (`SecurityConfig.java`, `UsuarioService.java`, y el propio `CursoRestController.java` una vez creado en la Parte A).

| Archivo a crear/modificar | Dónde está el código |
|---|---|
| `controller/CursoRestController.java` (nuevo) | `controller/package-info.md` (PASO A.1) |
| `config/CorsConfig.java` (nuevo) | `config/package-info.md` (PASO B.1) |
| `security/Rol.java` (nuevo) | `security/package-info.md` (PASO D.1) |
| `controller/RolRestController.java` (nuevo) | `controller/package-info.md` (PASO D.2) |
| `service/UsuarioService.java` (existente) | bloques comentados dentro del archivo (PASO D.3) |
| `security/JwtService.java` (nuevo) | `security/package-info.md` (PASO E.1) |
| `security/JwtAuthFilter.java` (nuevo) | `security/package-info.md` (PASO E.2) |
| `controller/AuthController.java` (nuevo) | `controller/package-info.md` (PASO E.3) |
| `config/SecurityConfig.java` (existente) | bloques comentados dentro del archivo (PASO C.1, C.2, E.4) |
| `controller/CursoRestController.java` (ya creado en Parte A) | bloques comentados dentro del propio archivo (PASO E.5) |

---

## Antes de la Parte A — Qué es una API y por qué existe

Una **API** (Application Programming Interface) es, en el fondo, un contrato: un conjunto de URLs, verbos HTTP y formatos de datos que un programa expone para que **otro programa** (no necesariamente un navegador) pueda usarlo. Una API REST en particular sigue un puñado de convenciones bien establecidas:

- **Cada URL representa un recurso** (`/api/cursos`, `/api/cursos/3`), no una acción — el verbo HTTP (`GET`/`POST`/`PUT`/`DELETE`) es el que define la acción sobre ese recurso.
- **HTTP es "sin estado" (stateless):** el servidor no recuerda nada entre un request y el siguiente por sí solo — cada request tiene que traer toda la información necesaria para procesarse (por eso, para las vistas HTML, se usa una cookie de sesión: es un "parche" que simula estado sobre un protocolo que no lo tiene. Para una API, en vez de simular estado con cookies, cada request va a traer su propia credencial: el JWT de la Parte E).
- **Los verbos tienen semántica esperada:** `GET` no debería modificar nada ("seguro"/*safe*), `PUT`/`DELETE` deberían poder repetirse sin cambiar el resultado más allá de la primera vez ("idempotentes"), `POST` no es ni lo uno ni lo otro (cada `POST` puede crear un recurso nuevo).
- **Los códigos de estado vienen agrupados por familia:** 2xx = éxito, 3xx = redirección, 4xx = error del cliente (el request está mal armado o no tiene permiso), 5xx = error del servidor. Hoy vas a usar 200, 201, 204 (éxito), 400, 401, 403, 404 (error del cliente).

**Para pensar:** ¿por qué les parece que una API "sin estado" es más fácil de escalar (correr en varios servidores al mismo tiempo) que una que depende de sesiones guardadas en memoria? (Pista: si el servidor no recuerda nada entre requests, no importa a CUÁL servidor llega cada request.)

### Qué significa REST (el acrónimo)

**REST = REpresentational State Transfer**, un término acuñado por Roy Fielding en el año 2000. No es un protocolo ni una librería — es un **estilo** para diseñar APIs. "Representational" (representacional) porque el servidor nunca manda el recurso en sí (la fila de la base de datos): manda una **representación** de su estado, en nuestro caso JSON. Los tres puntos de arriba (URLs=recursos, HTTP sin estado, verbos con semántica) son, justamente, las reglas de ese estilo — REST es el nombre formal de algo que ya venías construyendo. Una API "RESTful" es simplemente una que sigue estas reglas de forma consistente.

### Qué es JSON y quién lo convierte (Jackson)

**JSON** (JavaScript Object Notation) es un formato de texto plano para representar datos con pares clave-valor, listas y objetos anidados — pese al nombre, es independiente del lenguaje: cualquier lenguaje moderno lo puede leer y escribir. Por ejemplo:

```json
{
  "id": 3,
  "nombre": "Fundamentos Web",
  "profesor": { "id": 1, "nombre": "Ana Lopez" }
}
```

Spring Boot no te obliga a convertir esto a mano: trae incluida y configurada por defecto una librería llamada **Jackson**, que hace la conversión en ambas direcciones. Cuando un método de un `@RestController` devuelve un objeto Java, Jackson lo **serializa** a JSON antes de mandar la respuesta. Cuando usás `@RequestBody` (Parte A/E), Jackson hace el camino inverso: **deserializa** el JSON entrante y arma el objeto Java. Nunca llamás a Jackson directamente — por eso un `List<Curso>` se convierte "solo" en el JSON que vas a ver en la Parte A.

---

## Parte A — Crear el `CursoRestController`

1. Abrí `controller/package-info.md` y copiá el código completo a un archivo nuevo `controller/CursoRestController.java`.
2. Fijate en la anotación de clase: `@RestController` en vez de `@Controller`. Eso significa que **cada** método devuelve su resultado directo como body de la respuesta (JSON), no el nombre de una vista Thymeleaf.
3. Mirá el método `listar()`: usa `cursoService.listarConProfesor()` — el mismo método con `JOIN FETCH` que armaste en S9 — y no `listar()` a secas. Esto es a propósito: `profesor` es una relación `LAZY`, y si Jackson (la librería que convierte objetos Java a JSON) intenta serializar un proxy de Hibernate sin resolver, falla. Ya resolviste este problema en S9 para las vistas; hoy lo reusás para la API.
4. Vas a notar líneas `@PreAuthorize` comentadas arriba de `crear()`, `actualizar()` y `eliminar()` — todavía no las toques, son para la Parte E.
5. Reiniciá la app y probá en el navegador: `http://localhost:8080/api/cursos` — deberías ver JSON crudo con la lista de cursos.

**Para pensar:** técnicamente podrías usar `@Controller` + `@ResponseBody` método por método, en vez de `@RestController` a nivel de clase. `@RestController` es solo el atajo cuando TODOS los métodos de la clase van a devolver datos serializados, nunca vistas — que es exactamente el caso de un controller dedicado a una API.

---

## Parte B — CORS (Cross-Origin Resource Sharing)

**CORS** son las siglas de **Cross-Origin Resource Sharing** ("compartición de recursos entre orígenes"): la política que usa el navegador para decidir si un script de JavaScript, corriendo en un origen (dominio+puerto), puede leer la respuesta de un request a OTRO origen distinto. Importante: **CORS es una restricción del navegador, no del servidor** — Postman, `curl`, o una app móvil no están sujetos a ella.

1. Abrí `config/package-info.md` y copiá el código completo a un archivo nuevo `config/CorsConfig.java`.
2. Este bean define qué orígenes (dominios/puertos distintos al de tu API) pueden llamarla desde JavaScript corriendo en un navegador.
3. En `config/SecurityConfig.java`, descomentá `.cors(cors -> {})` al principio de la cadena de filtros (**PASO C.1**) — esto le dice a Spring Security que use el bean `CorsConfigurationSource` que acabás de crear.

### Bonus — ver un error real de CORS (opcional)

Si querés ver el error en carne propia en vez de solo leer sobre él:

1. Creá un archivo `test.html` con un `<script>` que haga `fetch('http://localhost:8080/api/cursos').then(r => r.json()).then(console.log).catch(console.error)`.
2. Serví ese HTML con un servidor simple en un puerto que **no** esté en `CorsConfig.java` (por ejemplo `python -m http.server 8000`) y abrilo en el navegador. En la consola de DevTools vas a ver algo como: `"...has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present..."` — el navegador bloqueó **leer** la respuesta, aunque el servidor haya contestado 200.
3. Ahora serví el mismo HTML en un puerto que sí esté permitido (`CorsConfig.java` trae `http://127.0.0.1:5500` de ejemplo — funciona bien con la extensión Live Server de VS Code) y confirmá que ahora sí se ve el JSON en consola.

---

## Parte C — Dejar pública la API (temporal)

1. Todavía en `SecurityConfig.java`, descomentá la línea `.requestMatchers("/api/**").permitAll()` (**PASO C.2**).
2. Sin este paso, cualquier request a `/api/cursos` pediría estar logueado (con `formLogin`, que ni siquiera tiene sentido para un cliente sin navegador como Postman) y vas a recibir una redirección a `/login` en vez de JSON.
3. Reiniciá la app y confirmá que `/api/cursos` responde sin pedir login.

**Nota importante:** esto es un punto de partida deliberadamente simple — el foco de las Partes A-C es `@RestController`/JSON/CORS, no autenticación todavía. En la Parte E vas a **reemplazar** este `permitAll()` amplio por un esquema real (JWT), así que no te preocupes por dejarlo "inseguro" por ahora: es un paso intermedio a propósito, para poder probar el CRUD JSON sin la complejidad de tokens desde el primer minuto.

---

## Antes de la Parte D — Por qué una tabla de constantes

Hasta ahora, el rol de un usuario es un `String` simple ("ADMIN", "USER") escrito a mano en varios lugares: `seed-data.sql`, formularios, `@PreAuthorize`. Nada impide que alguien escriba `"Admin"` o `"ADMN"` por error — la aplicación lo acepta sin quejarse, y ese usuario queda con un rol que nunca va a matchear ningún `hasRole(...)`.

Un **enum** (`Rol { ADMIN, USER }`) no resuelve esto en la anotación `@PreAuthorize` en sí (que sigue siendo un string SpEL), pero sí en cualquier código Java que lo use: el compilador rechaza `Rol.ADMINN` de inmediato, y tenés una única fuente de verdad sobre qué roles existen — útil también para exponerlos vía API (`GET /api/roles`) a quien necesite armar un formulario dinámico.

## Parte D — Tabla de constantes y roles

1. Abrí `security/package-info.md` y copiá el bloque del **PASO D.1** a un archivo nuevo `security/Rol.java`.
2. Copiá el bloque del **PASO D.2** (en `controller/package-info.md`) a un archivo nuevo `controller/RolRestController.java`.
3. En `service/UsuarioService.java`, descomentá el import de `Rol` y `Arrays`, las dos llamadas a `validarRol(...)` (en `crear()` y `actualizar()`) y el método `validarRol()` completo al final del archivo (**PASO D.3**).
4. Reiniciá la app. Probá crear un usuario con un rol inválido (por ejemplo `"Admin"` con minúscula) desde `/usuarios/nuevo` — debería fallar con un error, en vez de guardarse silenciosamente.
5. Probá `GET /api/roles` en el navegador (todavía sin JWT, porque `/api/**` sigue público desde la Parte C) — deberías ver `["ADMIN","USER"]`.

---

## Antes de la Parte E — Cómo funciona un JWT

Un **JWT** (JSON Web Token) es un token que el servidor firma digitalmente y le entrega al cliente después de un login exitoso. Tiene tres partes separadas por puntos (`header.payload.signature`), cada una codificada en Base64:

- **Header:** qué algoritmo de firma se usó.
- **Payload (claims):** datos del usuario — en este lab, el `username` y su `rol`.
- **Signature:** una firma criptográfica que garantiza que nadie modificó el contenido sin conocer la clave secreta del servidor.

El cliente manda ese token en cada request futuro, en el header `Authorization: Bearer <token>`. El servidor no necesita "recordar" nada (no hay sesión guardada en memoria ni en base de datos) — simplemente vuelve a verificar la firma con la misma clave secreta, y si es válida, confía en los datos del payload. Por eso es el patrón típico para autenticar una API sin sesión: es **stateless**, coherente con cómo funciona HTTP.

**Para pensar:** si alguien roba un JWT válido, ¿puede usarlo hasta que expire, aunque cambies tu contraseña? (Respuesta: sí — a diferencia de invalidar una sesión del lado del servidor, un JWT ya emitido sigue siendo válido hasta su fecha de expiración, salvo que se implemente una lista de revocación aparte. Es una de las razones por las que los tokens suelen tener una expiración corta, como la de este lab: 1 hora.)

## Parte E — Autenticación JWT

1. Abrí `security/package-info.md` y copiá el bloque del **PASO E.1** a un archivo nuevo `security/JwtService.java`.
2. Copiá el bloque del **PASO E.2** a un archivo nuevo `security/JwtAuthFilter.java`.
3. Abrí `controller/package-info.md` y copiá el bloque del **PASO E.3** a un archivo nuevo `controller/AuthController.java`.
4. En `config/SecurityConfig.java`, hacé el **PASO E.4** completo: descomentá los imports de arriba del archivo, el campo `jwtAuthFilter`, el bean `authenticationManager(...)`, la línea `.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))` (ver el "Para pensar" de abajo — sin esto el login de la API no funciona), volvé a **comentar** la línea del PASO C.2 (`/api/**` público — ya cumplió su propósito), descomentá las dos líneas nuevas (`/api/auth/login` público + resto de `/api/**` autenticado), y descomentá `.addFilterBefore(...)` al final de la cadena (recordá borrar el punto y coma que quedó después de `.sessionManagement(...)` y dejarlo solo al final, después de `addFilterBefore`).
5. En `controller/CursoRestController.java` (creado en la Parte A), descomentá las 3 líneas `@PreAuthorize("hasRole('ADMIN')")` (**PASO E.5**) — una arriba de `crear()`, una arriba de `actualizar()`, una arriba de `eliminar()`.
6. Reiniciá la app.

**Para pensar:** `@PreAuthorize("hasRole('ADMIN')")` en `CursoRestController` es EXACTAMENTE la misma anotación que ya usaste en `CursoController` (S11) para las vistas HTML. No le importa si el `Authentication` en el `SecurityContext` vino de una sesión de navegador (`formLogin`) o de un JWT válido (`JwtAuthFilter`, recién armado) — es el mismo mecanismo de autorización, alimentado por dos formas distintas de autenticarse.

**Por qué hace falta la línea de CSRF:** Spring Security protege por defecto cualquier `POST`/`PUT`/`DELETE` con un token CSRF (por eso `login.html` ya lo trae como campo oculto — es una protección pensada para formularios con sesión/cookie). Postman no manda ningún token CSRF, así que sin esa línea el `CsrfFilter` rechaza el login de la API — y no vas a ver un error claro: Spring Security redirige ese rechazo hacia `/403`, que es una página `GET`, así que el resultado es un confuso 405 "POST no soportado" en vez de un mensaje sobre CSRF. `/api/**` no necesita esta protección porque no usa cookies de sesión — se autentica con el JWT en el header `Authorization`.

---

## Parte F — Probar todo con Postman

**Importante antes de empezar:** un endpoint `POST` (como `/api/auth/login` o `POST /api/cursos`) **no se puede probar tipeando la URL directo en la barra de direcciones del navegador** — eso siempre manda un `GET`, nunca un `POST`, sin importar qué haya del otro lado. Vas a ver el error `HttpRequestMethodNotSupportedException: Request method 'GET' is not supported` si lo intentás así. Para mandar `POST`/`PUT`/`DELETE` con un body JSON necesitás Postman (o `curl`, o un `fetch` con `method: 'POST'`).

Con la app corriendo, probá el flujo completo:

1. **Pedir un token:** `POST /api/auth/login` con body `{"username": "admin", "password": "admin123"}` → 200 + `{"token": "..."}`.
2. **Probar con credenciales incorrectas:** mismo endpoint con un password equivocado → 401.
3. **Llamar la API sin token:** `GET /api/cursos` sin header `Authorization` → 401 (antes, en la Parte C, esta misma request daba 200 — ahora ya no).
4. **Llamar la API con token:** agregá el header `Authorization: Bearer <token que copiaste del paso 1>` y repetí `GET /api/cursos` → 200 + JSON.

   **Cómo hacerlo en Postman, paso a paso (dos formas, elegí una):**
   - **Opción A — pestaña Authorization:** en la request, pestaña `Authorization` → Auth Type → **`Bearer Token`** (⚠️ NO elijas `JWT Bearer` — ese modo hace que Postman **genere y firme su propio token** con un secreto que vos pongas, no usa el que ya te dio el login). En el campo `Token` que aparece, pegá el valor de `token` que te devolvió el login (el string largo, sin las comillas).
   - **Opción B — pestaña Headers:** agregá a mano un header `Key: Authorization`, `Value: Bearer <token>` (con un espacio entre `Bearer` y el token).

   Cualquiera de las dos opciones termina mandando el mismo header por debajo — es solo una diferencia de qué tan cómodo te resulta escribirlo.
5. **Probar los roles:** `GET /api/roles` con el token → `["ADMIN","USER"]`.
6. **Probar la escritura con ADMIN:** `POST /api/cursos` con el token de `admin` y un body válido → 201 + header `Location`.
7. **Probar la escritura con USER:** pedí un token para `profesor` o `estudiante` (`POST /api/auth/login`) y repetí el `POST /api/cursos` con ESE token → 403 (`@PreAuthorize` los bloquea, aunque el JWT sea válido).
8. **Casos ya conocidos:** `GET /api/cursos/999` → 404. `POST /api/cursos` con `"nombre": ""` → 400 (`@Valid`).

Si tu profesor comparte la colección de Postman ampliada, la request de "Login" ya incluye un script que guarda el token automáticamente en una variable de colección (`jwt_token`) — las demás requests lo usan solas, sin que tengas que copiar/pegar el token a mano en cada una.

---

## Bonus — Probar la API sin Postman (springdoc-openapi)

No es parte obligatoria del lab, pero vale la pena que sepas que existe: **springdoc-openapi** (dependencia `springdoc-openapi-starter-webmvc-ui` en el `pom.xml`) lee automáticamente tus clases `@RestController` y genera una página interactiva en `/swagger-ui.html` donde podés ver y probar cada endpoint desde el navegador, sin instalar Postman. Es el equivalente de Spring Boot a "Swagger". Hoy no lo vamos a instalar (Postman ya cubre lo que necesitamos), pero es una herramienta común en proyectos reales — documentación y pruebas "gratis" a partir del mismo código.

---

## Problemas comunes

| Síntoma | Solución |
|---|---|
| `GET /api/cursos` devuelve un error 500 o de serialización | Revisá que `listar()` use `cursoService.listarConProfesor()` y no `listar()` — `profesor` es LAZY. |
| Cualquier request a `/api/cursos` redirige a `/login` en vez de dar JSON (Partes A-C) | Falta agregar `/api/**` a `.permitAll()` en `SecurityConfig` (Parte C, PASO C.2). |
| El front en otro origen recibe un error de CORS en la consola del navegador | Confirmá que `CorsConfig.java` existe, que `.cors(cors -> {})` está descomentado, y que el origen del front está en `setAllowedOrigins(...)`. |
| Creaste un usuario con un rol con typo y no tiró error | Falta el PASO D.3 completo (`validarRol()` descomentado y llamado desde `crear()`/`actualizar()`). |
| `POST /api/auth/login` da 401 con credenciales que sí existen | Revisá que el body sea `{"username": "...", "password": "..."}` (contraseña en texto plano, no el hash), y que coincida con `seed-data.sql`. |
| `GET /api/cursos` da 401 aunque mandaste el header | El header tiene que llamarse exactamente `Authorization`, con valor `Bearer <token>` (con un espacio después de `Bearer`). Confirmá también que hiciste el PASO E.4 completo. |
| `GET /api/cursos` da 401 y en la pestaña Authorization elegiste "JWT Bearer" | Ese Auth Type NO es lo que necesitás — hace que Postman genere y firme un token nuevo con un secreto que vos pongas, ignorando el token real que te dio el login. Cambiá a Auth Type **"Bearer Token"** y pegá ahí el token que copiaste, o agregá el header `Authorization: Bearer <token>` a mano en la pestaña Headers. |
| `POST /api/cursos` da 403 con un token válido | El JWT es válido pero el usuario no es ADMIN, o falta el PASO E.5 (`@PreAuthorize` descomentado en `CursoRestController`). |
| Error de compilación en `SecurityConfig.java` después del PASO E.4 | Revisá que quede un solo punto y coma, al final de toda la cadena (después de `.addFilterBefore(...)`), no después de `.sessionManagement(...)`. |
| `POST`/`PUT` devuelven 400 sin mensaje claro | Revisá el body: falta un campo validado (`@NotBlank`, `@NotNull`, `@Min`/`@Max`) o `profesor` no trae `id`. |
| Postman con `Content-Type` mal configurado | El body tiene que ser `raw` + `JSON`, con el header `Content-Type: application/json`. |
| `HttpRequestMethodNotSupportedException: Request method 'GET' is not supported` al probar el login | Estás tipeando la URL en la barra de direcciones del navegador — eso siempre manda `GET`. Usá Postman con el método `POST` seleccionado. |
| `HttpRequestMethodNotSupportedException: Request method 'POST' is not supported`, y el header `Allow` de la respuesta dice `GET` | Esto casi siempre es CSRF, no un problema de reinicio: si no descomentaste la línea `.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))` del PASO E.4, `CsrfFilter` rechaza el POST y Spring Security lo reenvía internamente a `/403` (que es `GET`-only) — de ahí el 405 confuso. Revisá que esa línea esté descomentada en `SecurityConfig.java`. |
| `HttpRequestMethodNotSupportedException: Request method 'POST' is not supported` en Postman, con método y `Content-Type` ya correctos, y ya descartaste lo de CSRF de arriba | La app no se reinició después de crear `AuthController.java`/terminar el PASO E.4 — un `mapping` nuevo no aparece hasta el próximo reinicio. Reiniciá la app por completo y volvé a probar; si sigue igual, confirmá que no quedó otro proceso viejo escuchando en el puerto 8080. |

---

## Recursos de consulta

| Tema | Enlace |
|---|---|
| Spring — Building a RESTful Web Service | https://spring.io/guides/gs/rest-service |
| Baeldung — Exploring the New Spring Boot 3 HTTP Interface (RestController/ResponseEntity) | https://www.baeldung.com/spring-boot-restcontroller-controller |
| Spring — CORS Support | https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html |
| Baeldung — Spring Boot Bean Validation (`@Valid`) | https://www.baeldung.com/spring-boot-bean-validation |
| jwt.io — Introduction to JSON Web Tokens | https://jwt.io/introduction |
| Baeldung — JWT with Spring Security | https://www.baeldung.com/spring-security-oauth-jwt |
| springdoc-openapi — documentación oficial | https://springdoc.org/ |

---

## Preguntas

Cualquier duda durante el lab podés consultarla en:

- El canal `Consultas` del equipo de Teams.
- Directamente en clase, levantando la mano.
