# Bibliografía de repaso — Clase 13: Deployment (Render + Aiven)

Esta semana no hay clase en vivo — este documento reemplaza esa explicación hablada. Leelo en paralelo con `lab_clase13.md` (o justo después de cada Parte), usando como referencia el proyecto `cursosapp` que estás desplegando. No repite el paso a paso del lab — acá el objetivo es que entiendas **por qué** funciona cada cosa, que es lo que normalmente se explica en vivo.

---

## 1. Por qué un profile separado para producción

Desde el día uno, `application.properties` apunta a `localhost:3306` — tu MySQL local. Si editaras ese mismo archivo para apuntar a Aiven, tu proyecto dejaría de funcionar en tu computadora sin internet o sin acceso a esa base específica.

`application-prod.properties` resuelve esto: Spring Boot permite tener varios archivos de configuración, uno por "profile", y activar solo uno a la vez con la variable `spring.profiles.active`. En tu computadora, ese profile nunca se activa (seguís usando `application.properties` de siempre). En Render, la variable de entorno `SPRING_PROFILES_ACTIVE=prod` le dice a Spring "usá `application-prod.properties` además del de siempre" — los valores del profile `prod` sobrescriben los que se repiten, y lo que no se toca queda igual.

---

## 2. Por qué las credenciales van en variables de entorno, no en el archivo

`application-prod.properties` tiene `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}` — no las credenciales reales. Esto es intencional y es una práctica estándar de la industria: el archivo se sube al repo de GitHub (público o privado, da igual), pero las credenciales reales viven SOLO en el panel de variables de entorno de Render, que nadie más ve.

Si escribieras la contraseña real dentro del archivo y lo subieras a GitHub, cualquiera con acceso al repo (o al historial de git, incluso si borrás el archivo después) tendría la contraseña de tu base de datos.

---

## 3. Por qué Render necesita un Dockerfile para Java

A diferencia de Node.js, Python, Ruby o Go, Render no tiene un runtime nativo para Java — no existe un botón "Java" con build/start command como en esos lenguajes. La forma de correr una app Java en Render es empaquetarla en un contenedor Docker, que es un formato estándar que cualquier proveedor de hosting puede ejecutar sin importar el lenguaje de adentro.

El `Dockerfile` que ya viene armado en el proyecto hace esto en dos pasos ("build en dos etapas"):

1. Una imagen con el JDK completo compila el proyecto con Maven (`./mvnw clean package`).
2. Una segunda imagen, más liviana, con solo el JRE (no hace falta el compilador para EJECUTAR un `.jar` ya compilado), copia el resultado de la etapa anterior y lo corre.

Esto no es tema de este curso — Docker en profundidad queda para otro momento — pero vale entender la idea general: el Dockerfile es la "receta" que le dice a Render cómo construir y correr tu aplicación.

---

## 4. El puerto no es fijo en producción

En tu computadora, `server.port=8080` siempre. En Render (y en la mayoría de los hostings), el puerto real donde tu app debe escuchar lo decide la plataforma, no vos — y te lo pasa en la variable de entorno `PORT`.

Por eso `application-prod.properties` tiene `server.port=${PORT:8080}`: usa el valor de la variable de entorno `PORT` si existe (en Render, siempre existe), y si no existe (por ejemplo, si corrieras este profile por error en tu computadora), cae al 8080 de siempre.

---

## 5. `ddl-auto=update` en producción — con cuidado

Igual que en local, `spring.jpa.hibernate.ddl-auto=update` hace que Hibernate cree o ajuste las tablas solo, según las entidades `@Entity`. Es cómodo para un proyecto de curso, pero en un proyecto real de producción esto se considera riesgoso: un cambio mal pensado en una entidad podría alterar una tabla con datos reales de forma inesperada. La práctica recomendada en la industria es usar `validate` (Hibernate solo revisa que las tablas coincidan, nunca las modifica) combinado con herramientas de migración con control de versiones como Flyway o Liquibase — un tema que queda fuera del alcance de este curso, pero vale que sepas que existe.

---

## 6. SSL obligatorio con Aiven

`?sslMode=REQUIRED` al final de la URL JDBC no es opcional con Aiven — su plan gratuito exige que toda conexión use SSL/TLS (cifrado en tránsito). Sin ese parámetro, la conexión falla con un error de handshake. Esto es una buena práctica de seguridad que cada vez más proveedores de base de datos en la nube exigen por defecto (no solo Aiven).

---

## 7. Repaso rápido — dudas frecuentes

| Duda | Respuesta |
|---|---|
| ¿Por qué no simplemente cambio `application.properties` para que apunte a Aiven? | Porque entonces tu proyecto dejaría de funcionar en local contra tu MySQL. El profile `prod` separado te deja tener ambos configurados a la vez, sin pisarse. |
| ¿Puedo subir `application-prod.properties` a GitHub? | Sí — no tiene credenciales reales, solo nombres de variables de entorno (`${DB_URL}`, etc.). Las credenciales reales viven solo en Render. |
| ¿Por qué necesito un Dockerfile si nunca vi Docker en este curso? | Porque Render no soporta Java como runtime nativo — Docker es el mecanismo que usa la plataforma para correr cualquier lenguaje, incluido Java. El archivo ya viene armado, no hace falta escribirlo. |
| ¿Qué hago si Aiven "apagó" mi base? | Es normal en el plan gratuito tras inactividad — se reactiva manualmente desde el panel de Aiven. |
| ¿`server.port=8080` no debería ser siempre el mismo? | En producción, no — la plataforma de hosting (Render, en este caso) decide el puerto real vía la variable `PORT`. |

---

## Para seguir leyendo

| Tema | Enlace |
|---|---|
| Aiven — Primeros pasos con MySQL | https://aiven.io/docs/products/mysql/get-started |
| Render — Docker | https://render.com/docs/docker |
| Spring Boot — Profiles | https://docs.spring.io/spring-boot/reference/features/profiles.html |
| Spring Boot — Externalized Configuration | https://docs.spring.io/spring-boot/reference/features/external-config.html |
