# Etapa 1: compila el .jar con Maven (no necesitas Maven instalado en tu PC)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# Etapa 2: imagen liviana solo con el JRE y el .jar
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=America/Santiago
RUN useradd --system --uid 1001 spring
COPY --from=build /build/target/ms-usuarios.jar app.jar
USER spring
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
