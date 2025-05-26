package udehnih.report.config;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
class AsyncConfigTest {
    @Autowired
    private AsyncConfig asyncConfig;
    @Test

    void taskExecutorShouldHaveCorrectConfiguration() {
        Executor executor = asyncConfig.taskExecutor();
        assertTrue(executor instanceof DelegatingSecurityContextAsyncTaskExecutor);
        ThreadNameCapturingTask task = new ThreadNameCapturingTask();
        ((DelegatingSecurityContextAsyncTaskExecutor) executor).execute(task);
        try {
            Thread.sleep(100); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertTrue(task.getThreadName().startsWith("ReportThread-"),
                "Thread name should start with 'ReportThread-' but was: " + task.getThreadName());
        int taskCount = 5;
        CountDownLatchTask[] tasks = new CountDownLatchTask[taskCount];
        for (int i = 0; i < taskCount; i++) {
            tasks[i] = new CountDownLatchTask();
            ((DelegatingSecurityContextAsyncTaskExecutor) executor).execute(tasks[i]);
        }
        try {
            for (CountDownLatchTask latchTask : tasks) {
                assertTrue(latchTask.await(500, java.util.concurrent.TimeUnit.MILLISECONDS),
                        "Task did not complete in time, suggesting executor configuration issue");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test was interrupted while waiting for tasks to complete");
        }
    }
    private static class ThreadNameCapturingTask implements Runnable {
        private volatile String threadName;
        @Override

        public void run() {
            threadName = Thread.currentThread().getName();
        }

        public String getThreadName() {
            return threadName;
        }
    }
    private static class CountDownLatchTask implements Runnable {
        private final CountDownLatch latch = new CountDownLatch(1);
        @Override

        public void run() {
            try {
                Thread.sleep(10); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        }

        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }
}
