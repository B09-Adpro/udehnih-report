FROM openjdk:25-jdk-slim@sha256:dc3a2c86b324877db7231b7821fcf01251ee67d3104dc23683535d1e6854eb9e
 
WORKDIR /app

COPY build/libs/udehnih-report.jar udehnih-report.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "udehnih-report.jar"]