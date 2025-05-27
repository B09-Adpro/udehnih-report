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
        
        if (url == null || url.isEmpty()) {
            // Try to get from environment directly
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

        log.info("Main datasource URL: {}", url);
        log.info("Main datasource username: {}", username);
        log.info("Main datasource driver: org.postgresql.Driver");
        
        com.zaxxer.hikari.HikariDataSource dataSource = new com.zaxxer.hikari.HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        
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
