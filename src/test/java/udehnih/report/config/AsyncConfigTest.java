package udehnih.report.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AsyncConfigTest {

    @Autowired
    private AsyncConfig asyncConfig;

    @Test
    void taskExecutor_ShouldHaveCorrectConfiguration() {
        Executor executor = asyncConfig.taskExecutor();
        assertTrue(executor instanceof ThreadPoolTaskExecutor);
        
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertEquals(2, taskExecutor.getCorePoolSize());
        assertEquals(4, taskExecutor.getMaxPoolSize());
        assertEquals(100, taskExecutor.getQueueCapacity());
        assertEquals("ReportThread-", taskExecutor.getThreadNamePrefix());
    }
} 