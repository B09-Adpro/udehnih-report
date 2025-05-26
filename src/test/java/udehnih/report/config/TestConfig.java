package udehnih.report.config;
import javax.sql.DataSource;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
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
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("testdb")
            .build();
    }
    @Bean
    public DataSource authDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("authdb")
            .build();
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