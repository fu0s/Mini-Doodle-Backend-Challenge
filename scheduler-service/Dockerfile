FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY scheduler-service/target/scheduler-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
