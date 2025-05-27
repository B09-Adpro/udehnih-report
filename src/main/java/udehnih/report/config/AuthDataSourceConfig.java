package udehnih.report.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class AuthDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "auth.datasource")
    public DataSourceProperties authDataSourceProperties() {
        log.info("Configuring auth datasource properties");
        DataSourceProperties properties = new DataSourceProperties();
        
        properties.setDriverClassName("org.postgresql.Driver");
        
        return properties;
    }

    @Bean(name = "authDataSource")
    public DataSource authDataSource() {
        log.info("Creating auth datasource");
        
        String url = System.getenv("AUTH_DB_URL");
        String username = System.getenv("AUTH_DB_USERNAME");
        String password = System.getenv("AUTH_DB_PASSWORD");
        
        log.info("Auth DB URL from env: {}", url);
        log.info("Auth DB Username from env: {}", username);
        log.info("Auth DB Password from env: [MASKED]");
        
        if (url != null && !url.isEmpty() && !url.startsWith("jdbc:")) {
            url = "jdbc:" + url;
            log.info("Added jdbc: prefix to URL: {}", url);
        }
        
        if (url == null || url.isEmpty() || url.contains("${AUTH_DB_URL}")) {
            url = "jdbc:postgresql://database-1.c5ksuf1g9b2c.us-east-1.rds.amazonaws.com:5432/udehnihauth";
            log.warn("No AUTH_DB_URL found in environment variables. Using default URL from .env file.");
        }
        
        if (username == null || username.isEmpty() || username.contains("${AUTH_DB_USERNAME}")) {
            username = "postgres";
            log.warn("No AUTH_DB_USERNAME found in environment variables. Using default username from .env file.");
        }
        
        if (password == null || password.isEmpty() || password.contains("${AUTH_DB_PASSWORD}")) {
            password = "grace01.";
            log.warn("No AUTH_DB_PASSWORD found in environment variables. Using default password from .env file.");
        }
        
        log.info("Auth datasource URL: {}", url);
        log.info("Auth datasource username: {}", username);
        log.info("Auth datasource driver: org.postgresql.Driver");
        
        com.zaxxer.hikari.HikariDataSource dataSource = new com.zaxxer.hikari.HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(2);
        dataSource.setIdleTimeout(30000);
        dataSource.setPoolName("AuthHikariPool");
        
        log.info("Configured auth datasource with HikariCP for PostgreSQL");
        
        return dataSource;
    }

    @Bean(name = "authJdbcTemplate")
    public JdbcTemplate authJdbcTemplate(@Qualifier("authDataSource") final DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}