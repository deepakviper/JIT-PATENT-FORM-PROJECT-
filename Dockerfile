# Build Stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
# Copy the pom.xml and source code of the backend project
COPY patentform-backend-main/demo/pom.xml patentform-backend-main/demo/
COPY patentform-backend-main/demo/src patentform-backend-main/demo/src
COPY patentform-backend-main/demo/mvnw patentform-backend-main/demo/
COPY patentform-backend-main/demo/.mvn patentform-backend-main/demo/.mvn

WORKDIR /app/patentform-backend-main/demo
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Run Stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/patentform-backend-main/demo/target/demo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
