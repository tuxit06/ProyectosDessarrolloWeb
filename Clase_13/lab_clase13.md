# Lab Clase 13 — Deployment (Render + Aiven)

## Información general

| Dato | Valor |
|------|-------|
| Curso | SC-403 Desarrollo de Aplicaciones Web y Patrones |
| Universidad | Fidélitas |
| Modalidad | **Asincrónico / autoguiado esta semana** — no hay sesión en vivo, seguí esta guía por tu cuenta. |
| Evaluación | Ninguna — no hay que entregar nada. El objetivo es que lo hagas y te quede aplicado a tu proyecto. |
| Tiempo estimado | 2 a 3 horas — reservate ese bloque de una sola vez, igual que si fuera la clase. |

---

## Cómo trabajar esta clase sin el profesor presente

Esta semana no hay clase en vivo, así que esta guía tiene que alcanzar por sí sola. Algunas recomendaciones para que te rinda:

- **Hacela de corrido, en un solo bloque de 2-3 horas.** Crear cuentas, esperar a que un servicio "arranque", y diagnosticar un primer deploy roto son cosas que cuestan más si las cortás en pedacitos a lo largo de varios días.
- **Leé `bibliografia_clase13.md` (mismo `Para_Estudiantes/`) en paralelo, no después.** Ese documento explica el "por qué" de cada paso (por qué un profile de producción, por qué las credenciales van en variables de entorno, por qué Render necesita Docker para Java, etc.) — normalmente eso se explica hablado en clase; hoy está por escrito para que no te quede como una receta sin sentido.
- **Si algo no funciona, revisá primero la tabla "Problemas comunes"** al final de este documento — cubre los errores más típicos de un primer deploy.
- **Dudas que la guía no resuelve:** canal `Consultas` del equipo de Teams. No hay clase en vivo, pero el canal sigue activo — no te quedes trabado en silencio.
- Al final vas a tener tu propio `cursosapp` corriendo en una URL pública real — es un buen momento para aplicar lo mismo a tu **proyecto grupal**, no solo a este ejercicio.

---

## Propósito

Hasta ahora, `cursosapp` corrió siempre en tu computadora, contra un MySQL local. Hoy la sacás de ahí y la ponés en internet, de verdad: cualquier persona con la URL va a poder abrirla desde su navegador o celular.

Vas a usar dos servicios gratuitos:

- **Aiven** — hosting de la base de datos MySQL en la nube.
- **Render** — hosting de la aplicación Spring Boot.

Esta clase es distinta a las anteriores: no vas a escribir código Java nuevo. El trabajo de hoy es crear cuentas, configurar servicios y variables de entorno — la parte de "infraestructura" de poner una app en producción.

---

## Objetivos de aprendizaje

Al terminar este lab vas a haber demostrado que sos capaz de:

1. Crear un servicio de base de datos MySQL en la nube (Aiven).
2. Desplegar una aplicación Spring Boot en un hosting externo (Render), usando un Dockerfile ya provisto.
3. Configurar variables de entorno para separar credenciales de producción del código fuente.
4. Activar un profile de Spring (`application-prod.properties`) sin tocar el profile de desarrollo.
5. Diagnosticar los errores más comunes de un primer deploy (conexión a base de datos, variables de entorno, SSL).

---

## Material entregado

| Archivo/Carpeta | Descripción |
|---|---|
| `cursosapp/` | Mismo proyecto de S12 (CRUD + login + roles + API REST), con `Dockerfile` y `.dockerignore` ya incluidos. |

---

## Antes de empezar

Confirmá que el proyecto sigue funcionando igual que siempre en tu computadora:

```bash
cd cursosapp
mvnw.cmd spring-boot:run     # Windows
./mvnw spring-boot:run       # Linux/Mac
```

Necesitás también tu proyecto ya subido a un repo de GitHub (el mismo que venís usando desde S1) — Render se conecta directo al repo, no se sube código a mano.

---

## Parte A — Crear la base de datos en Aiven

1. Entrá a [aiven.io](https://aiven.io/) y creá una cuenta gratuita (no pide tarjeta).
2. Creá un nuevo servicio: elegí **MySQL**, plan **Free**, y una región cualquiera (el plan gratuito no permite elegir proveedor de nube).
3. Esperá a que el servicio pase a estado "Running" (puede tardar uno o dos minutos).
4. En la página del servicio, abrí **Quick connect** (o la pestaña de conexión) y anotá: **host**, **puerto**, **usuario**, **password** y **nombre de la base** (por defecto `defaultdb`).

**Importante:** el plan gratuito de Aiven apaga el servicio automáticamente después de un tiempo sin uso (te avisan antes por email). Si tu base "desapareció" un tiempo después, entrá al panel de Aiven y reactivala.

---

## Parte B — Crear `application-prod.properties`

1. Dentro de `src/main/resources/`, creá un archivo nuevo llamado `application-prod.properties`.
2. Copiá este contenido, reemplazando únicamente los valores de Aiven donde corresponda (los `${...}` NO se editan acá — se configuran como variables de entorno en Render en la Parte D):

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.thymeleaf.cache=true

server.port=${PORT:8080}
```

3. Hacé commit y push de este archivo — a diferencia de `application-local.properties`, este SÍ se sube al repo (no tiene credenciales reales adentro, solo nombres de variables).

---

## Parte C — Conectar el repo a Render

1. Entrá a [render.com](https://render.com/) y creá una cuenta gratuita.
2. Creá un nuevo **Web Service** y conectá tu repo de GitHub.
3. En **Environment**, elegí **Docker** (Render detecta el `Dockerfile` que ya viene en la raíz del proyecto — no hace falta escribir build command ni start command a mano).
4. Elegí el plan **Free**.

---

## Parte D — Variables de entorno en Render

En la pestaña **Environment** del Web Service, agregá estas variables (los valores de `DB_*` salen de Aiven, Parte A):

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:mysql://<host-de-aiven>:<puerto>/defaultdb?sslMode=REQUIRED` |
| `DB_USERNAME` | (usuario de Aiven) |
| `DB_PASSWORD` | (password de Aiven) |

**Aiven exige conexión SSL** — por eso `?sslMode=REQUIRED` al final de la URL, sin esto la conexión falla.

---

## Parte E — Deploy y verificación

1. Guardá las variables y disparar el deploy (Render lo hace automático al guardar, o con el botón "Manual Deploy").
2. Mirá los logs en vivo — la primera vez, vas a ver a Hibernate creando las tablas (`ddl-auto=update`).
3. Una vez que el deploy termine, copiá la URL pública que te da Render (algo como `https://cursosapp-xxxx.onrender.com`).
4. Corré `seed-data.sql` contra la base de Aiven (conectate con MySQL Workbench o el cliente que prefieras, usando los datos de la Parte A) para cargar los datos de ejemplo.
5. Abrí la URL pública en el navegador — deberías ver la home de `cursosapp`. Probá `/cursos`, logueate, probá `/api/cursos`.
6. En Postman, cambiá la variable `base_url` de la colección a tu URL de Render y probá la sección "Despliegue cloud".

---

## Problemas comunes

| Síntoma | Solución |
|---|---|
| El build falla en Render con un error relacionado a Docker | Confirmá que `Dockerfile` esté en la raíz del repo (mismo nivel que `pom.xml`) y que el Web Service tenga Environment: Docker seleccionado. |
| La app arranca pero no conecta a la base de datos | Revisá que `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` estén bien copiados desde Aiven, sin espacios de más. |
| Error de conexión relacionado a SSL/handshake | Confirmá que `DB_URL` termine en `?sslMode=REQUIRED` — Aiven exige SSL siempre. |
| `SPRING_PROFILES_ACTIVE=prod` no parece tener efecto | Revisá el nombre exacto de la variable (mayúsculas) y que el archivo se llame exactamente `application-prod.properties`. |
| El deploy funciona pero `/cursos` muestra la lista vacía | Falta correr `seed-data.sql` contra la base de Aiven — las tablas se crean solas, pero los datos de ejemplo no. |
| Aiven dice que el servicio está apagado | Es normal en el plan gratuito tras un tiempo sin uso — reactivalo desde el panel de Aiven. |

---

## Recursos de consulta

| Tema | Enlace |
|---|---|
| Aiven — Primeros pasos con MySQL | https://aiven.io/docs/products/mysql/get-started |
| Render — Docker | https://render.com/docs/docker |
| Render — Runtimes nativos vs Docker | https://render.com/docs/native-runtimes |
| Spring Boot — Profiles | https://docs.spring.io/spring-boot/reference/features/profiles.html |

---

## Preguntas

No hay clase en vivo esta semana, así que cualquier duda durante el lab consultala en:

- El canal `Consultas` del equipo de Teams — es el canal activo mientras resolvés esto por tu cuenta.
