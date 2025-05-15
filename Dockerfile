FROM openjdk:21-jdk-slim
 
WORKDIR /app

COPY build/libs/udehnih-report.jar udehnih-report.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "udehnih-report.jar"]