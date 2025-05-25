package udehnih.report.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JwtConfigTest {

    @Autowired
    private JwtConfig jwtConfig;

    @Test
    void getSecretKey_ShouldReturnConfiguredValue() {
        // Verify the secret key is not null or empty
        assertNotNull(jwtConfig.getSecretKey());
        assertFalse(jwtConfig.getSecretKey().isEmpty());
    }

    @Test
    void getExpiration_ShouldReturnConfiguredValue() {
        // Verify the expiration is positive
        assertNotNull(jwtConfig.getExpiration());
        assertTrue(jwtConfig.getExpiration() > 0);
    }
}
