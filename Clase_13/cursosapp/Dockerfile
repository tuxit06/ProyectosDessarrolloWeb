# ============================================================
# Dockerfile - cursosapp (S13: deployment en Render)
# ============================================================
# Render no tiene un runtime nativo para Java (a diferencia de Node, Python,
# Ruby, Go) - la forma de desplegar una app Java en Render es empaquetarla
# en un contenedor Docker. Este Dockerfile no es tema de la clase (Docker
# esta fuera del alcance de SC-403) - se entrega ya armado y solo hace
# falta que exista en la raiz del repo para que Render lo detecte solo.
#
# Build en 2 etapas: la primera compila con Maven + JDK completo (pesada),
# la segunda copia SOLO el .jar ya compilado a una imagen con JRE (liviana,
# sin herramientas de compilacion) - la imagen final que corre en Render
# es mucho mas chica que si se copiara todo el proyecto.
# ============================================================

# Etapa 1: compilar el proyecto con Maven Wrapper + JDK 25
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Etapa 2: imagen final, solo el JRE + el .jar ya compilado
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/cursosapp-0.0.1-SNAPSHOT.jar app.jar

# Render inyecta la variable de entorno PORT en runtime - application-prod.properties
# ya usa server.port=${PORT:8080} para escuchar en el puerto correcto.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
