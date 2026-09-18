FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd -u 1001 appuser

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# appuser needs to own /app so it can both read app.jar and create the
# uploads/ directory LocalStorageService writes to at runtime.
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]