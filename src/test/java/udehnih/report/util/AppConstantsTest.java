package udehnih.report.util;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import static org.junit.jupiter.api.Assertions.*;
class AppConstantsTest {
    @Test

    void constantsShouldHaveCorrectValues() {
        assertEquals("Authorization", AppConstants.AUTHORIZATION_HEADER);
        assertEquals("Bearer ", AppConstants.BEARER_PREFIX);
        assertEquals("email", AppConstants.EMAIL_FIELD);
        assertEquals("error", AppConstants.ERROR_KEY);
        assertEquals("ROLE_", AppConstants.ROLE_PREFIX);
        assertEquals("STUDENT", AppConstants.STUDENT_ROLE);
        assertEquals("Report not found with id: ", AppConstants.REPORT_NOT_FOUND_MSG);
        assertEquals("12345", AppConstants.TEST_STUDENT_ID);
    }
    @Test
    void constructorShouldBePrivate() throws NoSuchMethodException {
        Constructor<AppConstants> constructor = AppConstants.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }
    @Test
    void constructorShouldThrowUnsupportedOperationException() throws Exception {
        Constructor<AppConstants> constructor = AppConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(
            InvocationTargetException.class,
            () -> constructor.newInstance()
        );

        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
        assertEquals("Utility class", exception.getCause().getMessage());
    }
}
