FROM openjdk:25-jdk-slim@sha256:41120b274ca88d5159b1670b3d1c478d458332ccc11e6af79110b7a67a288981
 
WORKDIR /app

COPY build/libs/udehnih-report.jar udehnih-report.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "udehnih-report.jar"]