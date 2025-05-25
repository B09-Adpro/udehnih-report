package udehnih.report.config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class AuthDataSourceConfig {
    
    /**
     * Default constructor for Spring configuration class.
     */
    public AuthDataSourceConfig() {
        // Default constructor required by Spring
    }

    @Bean
    @ConfigurationProperties("auth.datasource")
    public DataSourceProperties authDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "authDataSource")
    public DataSource authDataSource() {
        final DataSourceProperties properties = authDataSourceProperties();
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "authJdbcTemplate")
    public JdbcTemplate authJdbcTemplate(@Qualifier("authDataSource") final DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}