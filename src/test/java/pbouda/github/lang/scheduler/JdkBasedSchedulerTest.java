package pbouda.github.lang.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JdkBasedSchedulerTest {

    @Mock
    ScheduledExecutorService executor;

    @Test
    public void start() {
        Runnable task = () -> {
        };

        Duration period = Duration.ofSeconds(1);
        Duration initialDelay = Duration.ofSeconds(2);

        Scheduler scheduler = new JdkBasedScheduler(task, executor, period, initialDelay);
        scheduler.start();

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleAtFixedRate(captor.capture(), eq(2000L), eq(1000L), eq(TimeUnit.MILLISECONDS));

        Runnable actualTask = captor.getValue();
        // it's the internal class, cannot be referenced
        assertEquals("ExceptionHandler", actualTask.getClass().getSimpleName());
        verifyNoMoreInteractions(executor);
    }

    @Test
    public void close() throws Exception {
        Runnable task = () -> {
        };

        Scheduler scheduler = new JdkBasedScheduler(task, executor, Duration.ofDays(1), Duration.ofDays(1));
        scheduler.close();

        verify(executor).shutdown();
        verify(executor).awaitTermination(2000, TimeUnit.MILLISECONDS);
        verifyNoMoreInteractions(executor);
    }
}