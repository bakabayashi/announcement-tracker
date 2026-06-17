# --- Build stage: compile and package the Spring Boot fat jar ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies first: copy only the POM, resolve, then copy sources.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests run in CI, not in the image build.
RUN mvn -B -q clean package -DskipTests

# --- Runtime stage: slim JRE with a non-root user ---
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /build/target/*.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
