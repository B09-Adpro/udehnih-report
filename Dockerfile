FROM openjdk:21-jdk-slim@sha256:7072053847a8a05d7f3a14ebc778a90b38c50ce7e8f199382128a53385160688
 
WORKDIR /app

COPY build/libs/udehnih-report.jar udehnih-report.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "udehnih-report.jar"]