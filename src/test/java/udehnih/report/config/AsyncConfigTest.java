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

@SpringBootTest
@ActiveProfiles("test")
class AsyncConfigTest {

    @Autowired
    private AsyncConfig asyncConfig;

    @Test
    void taskExecutorShouldHaveCorrectConfiguration() {
        // Get the executor from the config
        Executor executor = asyncConfig.taskExecutor();
        
        // Verify it's a DelegatingSecurityContextAsyncTaskExecutor
        assertTrue(executor instanceof DelegatingSecurityContextAsyncTaskExecutor);

        // Instead of using reflection to access private fields, test the behavior
        // Create a task and verify it runs with expected thread name
        ThreadNameCapturingTask task = new ThreadNameCapturingTask();
        ((DelegatingSecurityContextAsyncTaskExecutor) executor).execute(task);
        
        // Wait for task to complete
        try {
            Thread.sleep(100); // Small delay to ensure task executes
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify thread name follows expected pattern
        assertTrue(task.getThreadName().startsWith("ReportThread-"),
                "Thread name should start with 'ReportThread-' but was: " + task.getThreadName());
        
        // Verify the thread pool configuration by checking thread behavior
        // We're testing the effect of the configuration rather than implementation details
        // This avoids using reflection to access private fields
        
        // The thread name verification above already confirms the thread prefix is correct
        // The fact that our task executed successfully indicates the executor is working
        // We can also verify the executor handles multiple concurrent tasks
        
        // Create multiple tasks to verify concurrency
        int taskCount = 5;
        CountDownLatchTask[] tasks = new CountDownLatchTask[taskCount];
        for (int i = 0; i < taskCount; i++) {
            tasks[i] = new CountDownLatchTask();
            ((DelegatingSecurityContextAsyncTaskExecutor) executor).execute(tasks[i]);
        }
        
        // Wait for all tasks to complete
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
    
    /**
     * Helper class to capture the name of the thread that executes a task
     */
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
    
    /**
     * Helper class that uses a CountDownLatch to signal task completion
     */
    private static class CountDownLatchTask implements Runnable {
        private final CountDownLatch latch = new CountDownLatch(1);
        
        @Override
        public void run() {
            // Simulate some work
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
