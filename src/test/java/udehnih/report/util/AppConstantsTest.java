package udehnih.report.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class AppConstantsTest {

    @Test
    void constants_ShouldHaveCorrectValues() {
        // Test authentication related constants
        assertEquals("Authorization", AppConstants.AUTHORIZATION_HEADER);
        assertEquals("Bearer ", AppConstants.BEARER_PREFIX);
        assertEquals("email", AppConstants.EMAIL_FIELD);
        assertEquals("error", AppConstants.ERROR_KEY);
        assertEquals("ROLE_", AppConstants.ROLE_PREFIX);
        assertEquals("STUDENT", AppConstants.STUDENT_ROLE);
        
        // Test report related constants
        assertEquals("Report not found with id: ", AppConstants.REPORT_NOT_FOUND_MSG);
        
        // Test test constants
        assertEquals("12345", AppConstants.TEST_STUDENT_ID);
    }
    
    @Test
    void constructor_ShouldBePrivate() throws NoSuchMethodException {
        Constructor<AppConstants> constructor = AppConstants.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        
        // Try to invoke the private constructor
        constructor.setAccessible(true);
        Exception exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
        assertEquals("Utility class", exception.getCause().getMessage());
    }
}
