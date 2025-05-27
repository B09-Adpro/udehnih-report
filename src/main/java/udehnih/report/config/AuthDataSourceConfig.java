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
        
        boolean isTestEnvironment = false;
        
        if (url != null && url.contains("h2:mem")) {
            isTestEnvironment = true;
            log.info("Detected H2 database URL, assuming test environment");
        }
        
        try {
            if (System.getProperty("spring.profiles.active") != null && 
                System.getProperty("spring.profiles.active").contains("test")) {
                isTestEnvironment = true;
                log.info("Detected test profile, assuming test environment");
            }
        } catch (Exception e) {
            log.warn("Could not check spring.profiles.active: {}", e.getMessage());
        }
        
        try {
            Class.forName("org.junit.jupiter.api.Test");
            isTestEnvironment = true;
            log.info("Detected JUnit classes, assuming test environment");
        } catch (ClassNotFoundException e) {
        }
        
        if (!isTestEnvironment) {
            if (url == null || url.isEmpty() || url.contains("${AUTH_DB_URL}")) {
                log.error("AUTH_DB_URL not found in environment variables or contains unresolved placeholder");
                throw new IllegalStateException("AUTH_DB_URL not found or not resolved. Please check your .env file.");
            }
            
            if (username == null || username.isEmpty() || username.contains("${AUTH_DB_USERNAME}")) {
                log.error("AUTH_DB_USERNAME not found in environment variables or contains unresolved placeholder");
                throw new IllegalStateException("AUTH_DB_USERNAME not found or not resolved. Please check your .env file.");
            }
            
            if (password == null || password.isEmpty() || password.contains("${AUTH_DB_PASSWORD}")) {
                log.error("AUTH_DB_PASSWORD not found in environment variables or contains unresolved placeholder");
                throw new IllegalStateException("AUTH_DB_PASSWORD not found or not resolved. Please check your .env file.");
            }
        } else {
            log.info("Running in test environment, using test database configuration");
            if (url == null || url.isEmpty() || url.contains("${AUTH_DB_URL}")) {
                url = "jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
                log.info("Using H2 in-memory database for auth tests: {}", url);
            }
            
            if (username == null || username.isEmpty() || username.contains("${AUTH_DB_USERNAME}")) {
                username = "sa";
            }
            
            if (password == null || password.isEmpty() || password.contains("${AUTH_DB_PASSWORD}")) {
                password = "sa";
            }
        }
        
        log.info("Auth datasource URL: {}", url);
        log.info("Auth datasource username: {}", username);
        
        com.zaxxer.hikari.HikariDataSource dataSource = new com.zaxxer.hikari.HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        
        if (url != null && url.contains("h2")) {
            log.info("Using H2 driver for test environment");
            dataSource.setDriverClassName("org.h2.Driver");
        } else {
            log.info("Using PostgreSQL driver for production environment");
            dataSource.setDriverClassName("org.postgresql.Driver");
        }
        
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