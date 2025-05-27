package udehnih.report.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

@TestConfiguration
@Profile("test")
public class TestDatabaseConfig {

    @Primary
    @Bean
    public DataSourceProperties mainDataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setDriverClassName("org.h2.Driver");
        properties.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        properties.setUsername("sa");
        properties.setPassword("sa");
        return properties;
    }
    
    @Bean
    public DataSourceProperties authDataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setDriverClassName("org.h2.Driver");
        properties.setUrl("jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        properties.setUsername("sa");
        properties.setPassword("sa");
        return properties;
    }

    @Primary
    @Bean
    public DataSource mainDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb")
                .build();
    }

    @Bean(name = "authDataSource")
    public DataSource authDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("authdb")
                .build();
    }
}
