package udehnih.report.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableJpaRepositories(
    basePackages = "udehnih.report.repository",
    entityManagerFactoryRef = "mainEntityManagerFactory",
    transactionManagerRef = "mainTransactionManager"
)
@EntityScan(basePackages = "udehnih.report.model")
public class MainDataSourceConfig {
    
    private boolean testEnvironmentOverride = false;
    
    public void setTestEnvironmentOverride(boolean override) {
        this.testEnvironmentOverride = override;
    }
    
    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties mainDataSourceProperties() {
        log.info("Configuring main datasource properties");
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource mainDataSource() {
        log.info("Creating main datasource");
        
        DataSourceProperties properties = mainDataSourceProperties();
        
        String url = properties.getUrl();
        String username = properties.getUsername();
        String password = properties.getPassword();
        
        boolean isTestEnvironment = testEnvironmentOverride;
        
        if (!testEnvironmentOverride) {
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
        }
        
        if (isTestEnvironment) {
            log.info("Running in test environment, using test database configuration");
            if (url == null || url.isEmpty()) {
                url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
                log.info("Using H2 in-memory database for main tests: {}", url);
            }
            
            if (username == null || username.isEmpty()) {
                username = "sa";
            }
            
            if (password == null || password.isEmpty()) {
                password = "sa";
            }
        } else {
            if (url == null || url.isEmpty()) {
                url = System.getProperty("DB_URL");
                if (url != null && !url.startsWith("jdbc:")) {
                    url = "jdbc:" + url;
                }
                
                if (url == null || url.isEmpty()) {
                    url = System.getenv("DB_URL");
                    if (url != null && !url.startsWith("jdbc:")) {
                        url = "jdbc:" + url;
                    }
                }
                if (url == null || url.isEmpty()) {
                    url = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres";
                    log.warn("Database URL not found in environment variables. Using default URL from .env file.");
                }
            }
            
            if (username == null || username.isEmpty()) {
                username = System.getProperty("DB_USERNAME");
                if (username == null || username.isEmpty()) {
                    username = System.getenv("DB_USERNAME");
                }
                if (username == null || username.isEmpty()) {
                    username = "postgres.khzxprnbkmhnlrwjcxjw";
                    log.warn("Database username not found in environment variables. Using default username from .env file.");
                }
            }
            
            if (password == null || password.isEmpty()) {
                password = System.getProperty("DB_PASSWORD");
                if (password == null || password.isEmpty()) {
                    password = System.getenv("DB_PASSWORD");
                }
                if (password == null || password.isEmpty()) {
                    password = "postgres";
                    log.warn("Database password not found in environment variables. Using default password from .env file.");
                }
            }
        }

        log.info("Main datasource URL: {}", url);
        log.info("Main datasource username: {}", username);
        
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
        
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(5);
        dataSource.setIdleTimeout(30000);
        dataSource.setPoolName("MainHikariPool");
        
        log.info("Configured main datasource with HikariCP for PostgreSQL");
        
        return dataSource;
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean mainEntityManagerFactory(
            final EntityManagerFactoryBuilder builder) {
        Map<String, String> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", "true");
        
        return builder
                .dataSource(mainDataSource())
                .packages("udehnih.report.model")
                .persistenceUnit("main")
                .properties(properties)
                .build();
    }
    
    @Primary
    @Bean
    public JpaTransactionManager mainTransactionManager(
            @Qualifier("mainEntityManagerFactory") final EntityManagerFactory emFactory) {
        return new JpaTransactionManager(emFactory);
    }
}
