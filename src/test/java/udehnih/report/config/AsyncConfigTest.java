package udehnih.report.config;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AsyncConfigTest {

    @Autowired
    private AsyncConfig asyncConfig;

    @Test
    void taskExecutor_ShouldHaveCorrectConfiguration() throws Exception {
        Executor executor = asyncConfig.taskExecutor();
        assertTrue(
            executor instanceof DelegatingSecurityContextAsyncTaskExecutor
        );

        DelegatingSecurityContextAsyncTaskExecutor securityExecutor =
            (DelegatingSecurityContextAsyncTaskExecutor) executor;

        Method getDelegateMethod = DelegatingSecurityContextAsyncTaskExecutor.class.getDeclaredMethod("getDelegate");
        getDelegateMethod.setAccessible(true);
        ThreadPoolTaskExecutor taskExecutor = 
            (ThreadPoolTaskExecutor) getDelegateMethod.invoke(securityExecutor);

        assertEquals(2, taskExecutor.getCorePoolSize());
        assertEquals(4, taskExecutor.getMaxPoolSize());
        assertEquals(100, taskExecutor.getQueueCapacity());
        assertEquals("ReportThread-", taskExecutor.getThreadNamePrefix());
    }
}
