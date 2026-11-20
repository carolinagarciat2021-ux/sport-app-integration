# ============================================================
# Etapa 1: compilar el proyecto con Maven
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Descarga las dependencias primero (se cachea si el pom.xml no cambió,
# así las siguientes construcciones son más rápidas)
RUN mvn dependency:go-offline -B
COPY src ./src
# Se ejecutan las pruebas unitarias; si alguna falla, la imagen no se construye
RUN mvn clean package -B

# ============================================================
# Etapa 2: imagen final, liviana, solo con el .jar ya compilado
# ============================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/tiendasport-backend.jar app.jar

# La aplicación escucha en el puerto 8081 (server.port=${PORT:8081}).
# Si la plataforma inyecta la variable PORT, Spring Boot la usa.
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
