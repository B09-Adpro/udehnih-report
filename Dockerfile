FROM docker.io/library/eclipse-temurin:21-jdk-alpine AS builder
 
WORKDIR /src/main/java/udehnih/report
COPY . .
RUN # Expose ports for Nginx (main), Spring Boot, Prometheus, and Grafana
EXPOSE 8000 8080 9090 3001 ./gradlew clean bootJar

FROM docker.io/library/eclipse-temurin:21-jre-alpine AS runner

# Install required packages for Prometheus, Grafana, and Nginx
RUN apk add --no-cache wget curl supervisor bash nginx

# Add Prometheus user
RUN addgroup -S prometheus && adduser -S -G prometheus prometheus

# Install Prometheus
RUN mkdir -p /etc/prometheus /var/lib/prometheus
COPY prometheus-render.yml /etc/prometheus/prometheus.yml
RUN wget -q https://github.com/prometheus/prometheus/releases/download/v2.45.0/prometheus-2.45.0.linux-amd64.tar.gz \
    && tar xf prometheus-2.45.0.linux-amd64.tar.gz \
    && cp prometheus-2.45.0.linux-amd64/prometheus /usr/local/bin/ \
    && cp prometheus-2.45.0.linux-amd64/promtool /usr/local/bin/ \
    && cp -r prometheus-2.45.0.linux-amd64/consoles /etc/prometheus/ \
    && cp -r prometheus-2.45.0.linux-amd64/console_libraries /etc/prometheus/ \
    && rm -rf prometheus-2.45.0.linux-amd64.tar.gz prometheus-2.45.0.linux-amd64 \
    && chown -R prometheus:prometheus /etc/prometheus /var/lib/prometheus

# Install Grafana
RUN wget -q https://dl.grafana.com/enterprise/release/grafana-enterprise-10.0.3.linux-amd64.tar.gz \
    && tar -zxf grafana-enterprise-10.0.3.linux-amd64.tar.gz \
    && mv grafana-10.0.3 /usr/share/grafana \
    && rm grafana-enterprise-10.0.3.linux-amd64.tar.gz \
    && addgroup -S grafana && adduser -S -G grafana grafana \
    && mkdir -p /var/lib/grafana /etc/grafana \
    && chown -R grafana:grafana /usr/share/grafana /var/lib/grafana /etc/grafana

# Add Spring Boot application user
ARG USER_NAME=udehnih
ARG USER_UID=1000
ARG USER_GID=${USER_UID}

RUN addgroup -g ${USER_GID} ${USER_NAME} || addgroup -S ${USER_NAME} && \
    adduser -h /opt/udehnih -D -u ${USER_UID} -G ${USER_NAME} ${USER_NAME} || adduser -S -G ${USER_NAME} ${USER_NAME}

WORKDIR /opt/udehnih
COPY --from=builder /src/main/java/udehnih/report/build/libs/*.jar app.jar

# Configure Grafana datasource and dashboards
COPY grafana-datasource.yml /etc/grafana/provisioning/datasources/datasource.yml
COPY grafana-dashboard.yml /etc/grafana/provisioning/dashboards/dashboard.yml
RUN mkdir -p /var/lib/grafana/dashboards

# Configure Nginx as reverse proxy
RUN mkdir -p /etc/nginx/conf.d
RUN echo 'server {
    listen 8080;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    location /prometheus/ {
        proxy_pass http://localhost:9090/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    location /grafana/ {
        proxy_pass http://localhost:3001/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}' > /etc/nginx/conf.d/default.conf

# Setup supervisor to manage all processes
RUN mkdir -p /etc/supervisor.d
COPY <<EOF /etc/supervisor.d/services.ini
[supervisord]
nodaemon=true
user=root

[program:spring-boot]
command=java -jar /opt/udehnih/app.jar
user=${USER_NAME}
autostart=true
autorestart=true
stdout_logfile=/var/log/spring-boot.log
stderr_logfile=/var/log/spring-boot-err.log

[program:prometheus]
command=/usr/local/bin/prometheus --config.file=/etc/prometheus/prometheus.yml --storage.tsdb.path=/var/lib/prometheus --web.listen-address=:9090
user=prometheus
autostart=true
autorestart=true
stdout_logfile=/var/log/prometheus.log
stderr_logfile=/var/log/prometheus-err.log

[program:grafana]
command=/usr/share/grafana/bin/grafana-server --homepath=/usr/share/grafana --config=/etc/grafana/grafana.ini cfg:server.http_port=3000
user=grafana
autostart=true
autorestart=true
stdout_logfile=/var/log/grafana.log
stderr_logfile=/var/log/grafana-err.log

[program:nginx]
command=/usr/sbin/nginx -g "daemon off;"
user=root
autostart=true
autorestart=true
stdout_logfile=/var/log/nginx.log
stderr_logfile=/var/log/nginx-err.log
EOF

RUN mkdir -p /var/log

# Expose ports for Spring Boot, Prometheus, and Grafana
EXPOSE 8080 9090 3001

# Start supervisor which will start all services
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf", "-i", "/etc/supervisor.d"]