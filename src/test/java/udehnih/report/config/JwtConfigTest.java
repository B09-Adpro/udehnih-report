package udehnih.report.config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@org.springframework.context.annotation.Import(TestDatabaseConfig.class)
class JwtConfigTest {
    @Autowired
    private JwtConfig jwtConfig;
    @Test

    void getSecretKeyShouldReturnConfiguredValue() {
        assertNotNull(jwtConfig.getSecretKey());
        assertFalse(jwtConfig.getSecretKey().isEmpty());
    }
    @Test

    void getExpirationShouldReturnConfiguredValue() {
        assertNotNull(jwtConfig.getExpiration());
        assertTrue(jwtConfig.getExpiration() > 0);
    }
}
