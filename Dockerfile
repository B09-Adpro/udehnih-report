FROM docker.io/library/openjdk:25-jdk-slim@sha256:41120b274ca88d5159b1670b3d1c478d458332ccc11e6af79110b7a67a288981 AS builder
 
WORKDIR /src/main/java/udehnih/report
COPY . .
RUN ./gradlew clean bootJar

FROM docker.io/library/openjdk:25-jdk-slim@sha256:41120b274ca88d5159b1670b3d1c478d458332ccc11e6af79110b7a67a288981 AS runner

ARG USER_NAME=udehnih
ARG USER_UID=1000
ARG USER_GID=${USER_UID}

RUN addgroup -g ${USER_GID} ${USER_NAME} && adduser -h /opt/udehnih -D -u ${USER_UID} -G ${USER_NAME} ${USER_NAME}

USER ${USER_NAME}

WORKDIR /opt/udehnih

COPY --from=builder --chown=${USER_UID}:${USER_GID} /src/main/java/udehnih/report/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java"]
CMD ["-jar", "app.jar"]