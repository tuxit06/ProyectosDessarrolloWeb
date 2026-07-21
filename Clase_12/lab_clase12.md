# Lab Clase 12 — APIs REST (JSON)

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

Hasta ahora, `cursosapp` solo habla con navegadores: cada URL devuelve una página HTML completa (Thymeleaf). Pero muchas aplicaciones reales necesitan que otros programas —un app móvil, un frontend en React, otro servicio— consuman los mismos datos sin recibir HTML, sino **JSON**.

En este lab agregás una **API REST** (`/api/cursos`) al lado de las vistas HTML que ya tenés (`/cursos`). Las dos conviven en el mismo proyecto: mismas entidades, mismo `CursoService`, distinto Controller.

---

## Objetivos de aprendizaje

Al terminar este lab vas a haber demostrado que sos capaz de:

1. Diferenciar `@RestController` de `@Controller` y entender qué hace `@ResponseBody` por debajo.
2. Recibir JSON en el body de un request con `@RequestBody` + validarlo con `@Valid`.
3. Devolver códigos de estado HTTP explícitos con `ResponseEntity` (200, 201, 204, 404).
4. Construir el header `Location` de una respuesta 201 Created.
5. Configurar CORS para que un frontend en otro origen pueda consumir la API.
6. Probar todos los endpoints con Postman.

---

## Material entregado

| Archivo/Carpeta | Descripción |
|---|---|
| `cursosapp/` | Mismo proyecto de S11 (login, roles, `@PreAuthorize`, 403), con los archivos nuevos de este lab descritos en `package-info.md`. |
| `postman-collection.json` | Colección base (Home, Cursos, Demo en clase, Despliegue). Vos vas a poder armar tus propias requests de API REST siguiendo el mismo patrón, o pedirle al profesor la colección ampliada de referencia. |

---

## Antes de empezar

Verificá que el proyecto base arranca sin problemas y que las vistas HTML (`/cursos`) siguen funcionando igual que en S11:

```bash
cd cursosapp
mvnw.cmd spring-boot:run     # Windows
./mvnw spring-boot:run       # Linux/Mac
```

Logueate como `admin` / `admin123` y confirmá que el CRUD de siempre sigue funcionando. Todo lo de hoy se agrega **al lado**, sin tocar nada de eso.

---

## Cómo está organizado el código pre-comentado

Esta clase agrega **dos archivos completamente nuevos**, así que no hay bloques `// CLASE 12 - PASO X.Y` para descomentar dentro de ellos — en cambio, el código a copiar está en un `package-info.md` dentro del paquete donde tenés que crear cada archivo. Sí hay pasos comentados dentro de `SecurityConfig.java`, que ya existe.

| Archivo a crear | Dónde está el código |
|---|---|
| `controller/CursoRestController.java` | `controller/package-info.md` (PASO A.1) |
| `config/CorsConfig.java` | `config/package-info.md` (PASO B.1) |

---

## Parte A — Crear el `CursoRestController`

1. Abrí `controller/package-info.md` y copiá el código completo a un archivo nuevo `controller/CursoRestController.java`.
2. Fijate en la anotación de clase: `@RestController` en vez de `@Controller`. Eso significa que **cada** método devuelve su resultado directo como body de la respuesta (JSON), no el nombre de una vista Thymeleaf.
3. Mirá el método `listar()`: usa `cursoService.listarConProfesor()` — el mismo método con `JOIN FETCH` que armaste en S9 — y no `listar()` a secas. Esto es a propósito: `profesor` es una relación `LAZY`, y si Jackson (la librería que convierte objetos Java a JSON) intenta serializar un proxy de Hibernate sin resolver, falla. Ya resolviste este problema en S9 para las vistas; hoy lo reusás para la API.
4. Reiniciá la app y probá en el navegador: `http://localhost:8080/api/cursos` — deberías ver JSON crudo con la lista de cursos.

---

## Parte B — CORS

1. Abrí `config/package-info.md` y copiá el código completo a un archivo nuevo `config/CorsConfig.java`.
2. Este bean define qué orígenes (dominios/puertos distintos al de tu API) pueden llamarla desde JavaScript corriendo en un navegador.
3. En `config/SecurityConfig.java`, descomentá `.cors(cors -> {})` al principio de la cadena de filtros (**PASO C.1**) — esto le dice a Spring Security que use el bean `CorsConfigurationSource` que acabás de crear.

---

## Parte C — Dejar pública la API

1. Todavía en `SecurityConfig.java`, agregá `"/api/**"` a la lista de `.requestMatchers(...).permitAll()` (**PASO C.2**).
2. Sin este paso, cualquier request a `/api/cursos` pediría estar logueado (con `formLogin`, que ni siquiera tiene sentido para un cliente sin navegador como Postman) y vas a recibir una redirección a `/login` en vez de JSON.
3. Reiniciá la app.

**Nota importante:** dejar `/api/**` público es una decisión deliberada para este lab — el foco de hoy es `@RestController`/JSON/CORS, no resolver autenticación para una API. En un proyecto real, una API se protege típicamente con un esquema sin sesión (JWT, API keys), que queda fuera del alcance de este curso.

---

## Parte D — Probar todo con Postman

Con la app corriendo, probá los 5 endpoints:

| Verbo | URL | Qué esperar |
|---|---|---|
| GET | `/api/cursos` | 200 + array JSON con todos los cursos |
| GET | `/api/cursos/1` | 200 + el curso con id=1 |
| GET | `/api/cursos/999` | 404 (no existe) |
| POST | `/api/cursos` | 201 + header `Location` con la URL del nuevo curso. Body de ejemplo: `{"nombre": "Testing", "descripcion": "Prueba", "creditos": 3, "profesor": {"id": 1}}` |
| PUT | `/api/cursos/1` | 200 + el curso actualizado |
| DELETE | `/api/cursos/6` | 204 (sin body) |

Probá también mandar un POST con `"nombre": ""` (vacío) — deberías recibir 400, porque `@Valid` rechaza el body antes de que el método se ejecute.

---

## Problemas comunes

| Síntoma | Solución |
|---|---|
| `GET /api/cursos` devuelve un error 500 o de serialización | Revisá que `listar()` use `cursoService.listarConProfesor()` y no `listar()` — `profesor` es LAZY. |
| Cualquier request a `/api/cursos` redirige a `/login` en vez de dar JSON | Falta agregar `/api/**` a `.permitAll()` en `SecurityConfig` (Parte C). |
| El front en otro origen recibe un error de CORS en la consola del navegador | Confirmá que `CorsConfig.java` existe, que `.cors(cors -> {})` está descomentado, y que el origen del front está en `setAllowedOrigins(...)`. |
| `POST`/`PUT` devuelven 400 sin mensaje claro | Revisá el body: falta un campo validado (`@NotBlank`, `@NotNull`, `@Min`/`@Max`) o `profesor` no trae `id`. |
| Postman con `Content-Type` mal configurado | El body tiene que ser `raw` + `JSON`, con el header `Content-Type: application/json`. |

---

## Recursos de consulta

| Tema | Enlace |
|---|---|
| Spring — Building a RESTful Web Service | https://spring.io/guides/gs/rest-service |
| Baeldung — Exploring the New Spring Boot 3 HTTP Interface (RestController/ResponseEntity) | https://www.baeldung.com/spring-boot-restcontroller-controller |
| Spring — CORS Support | https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html |
| Baeldung — Spring Boot Bean Validation (`@Valid`) | https://www.baeldung.com/spring-boot-bean-validation |

---

## Preguntas

Cualquier duda durante el lab podés consultarla en:

- El canal `Consultas` del equipo de Teams.
- Directamente en clase, levantando la mano.
