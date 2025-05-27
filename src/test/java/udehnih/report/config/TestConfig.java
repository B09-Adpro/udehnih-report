package udehnih.report.config;

import javax.sql.DataSource;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import udehnih.report.client.AuthServiceClient;
import udehnih.report.service.CustomUserDetailsService;
import udehnih.report.service.ReportService;
import udehnih.report.util.JwtUtil;

@TestConfiguration
public class TestConfig implements WebMvcConfigurer {
    @Override

    public void configureAsyncSupport(final AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(5000);
    }
    @Bean

    public DataSource mainDataSource() {
        org.springframework.jdbc.datasource.DriverManagerDataSource dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUsername("sa");
        dataSource.setPassword("sa");
        
        org.springframework.jdbc.datasource.init.ResourceDatabasePopulator populator = 
            new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator();
        populator.addScript(new org.springframework.core.io.ClassPathResource("schema.sql"));
        populator.execute(dataSource);
        
        return dataSource;
    }
    @Bean

    public DataSource authDataSource() {
        org.springframework.jdbc.datasource.DriverManagerDataSource dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUsername("sa");
        dataSource.setPassword("sa");
        
        org.springframework.jdbc.datasource.init.ResourceDatabasePopulator populator = 
            new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator();
        populator.addScript(new org.springframework.core.io.ClassPathResource("auth-schema.sql"));
        populator.execute(dataSource);
        
        return dataSource;
    }
    @Bean(name = "mainJdbcTemplate")

    public JdbcTemplate mainJdbcTemplate() {
        return new JdbcTemplate(mainDataSource());
    }
    @Bean(name = "authJdbcTemplate")

    public JdbcTemplate authJdbcTemplate() {
        return new JdbcTemplate(authDataSource());
    }
    @Bean

    public JwtConfig jwtConfig() {
        return new TestJwtConfig();
    }
    private static class TestJwtConfig extends JwtConfig {
        @Override

        public String getSecretKey() {
            return "testSecretKeyWithAtLeast32Characters12345";
        }
        @Override

        public Long getExpiration() {
            return 3600000L; 
        }
    }
    @Bean

    public JwtUtil jwtUtil() {
        return Mockito.mock(JwtUtil.class);
    }
    @Bean

    public CustomUserDetailsService customUserDetailsService() {
        return Mockito.mock(CustomUserDetailsService.class);
    }

    @Bean
    public AuthServiceClient authServiceClient() {
        return Mockito.mock(AuthServiceClient.class);
    }

    @Bean
    public ReportService reportService() {
        return Mockito.mock(ReportService.class);
    }
    @Bean

    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor taskExecutor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ReportThread-");
        executor.initialize();
        return executor;
    }
} 