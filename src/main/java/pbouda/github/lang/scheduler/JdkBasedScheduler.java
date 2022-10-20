package pbouda.github.lang.scheduler;

import pbouda.github.lang.github.HttpInvocation;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

/**
 * Implementation of {@link Scheduler} based on JDK Scheduler {@link ScheduledExecutorService}.
 * <p>
 * <b>We need to keep in mind:</b>
 * <ul>
 * <li>We need to define fixed rate (default 1-day) ticking.</li>
 * <li>Are we able to keep with the pace of the ticking?</li>
 * <ul/>
 */
public class JdkBasedScheduler implements Scheduler {

    private static final System.Logger LOG = System.getLogger(HttpInvocation.class.getName());

    private static final Duration DEFAULT_PERIOD = Duration.ofDays(1);
    private static final Duration DEFAULT_INITIAL_DELAY = Duration.ZERO;
    private static final Duration DEFAULT_TERMINATION_TIMEOUT = Duration.ofSeconds(2);


    private final Runnable task;
    private final ScheduledExecutorService executor;
    private final Duration period;
    private final Duration initialDelay;

    /**
     * Creates a new instance of {@link Scheduler} based on default values
     * {@link  #DEFAULT_PERIOD}, {@link #DEFAULT_INITIAL_DELAY}.
     *
     * @param task the task periodically executed on the scheduler.
     */
    public JdkBasedScheduler(Runnable task) {
        this(task, Executors.newSingleThreadScheduledExecutor(), DEFAULT_PERIOD, DEFAULT_INITIAL_DELAY);
    }

    /**
     * Creates a customized instance of {@link Scheduler}
     *
     * @param task         the task periodically executed on the scheduler.
     * @param executor     the customized service executor to schedule tasks on.
     * @param period       the period of task's executions defined using {@link java.time.Duration}
     *                     with {@link TimeUnit#MILLISECONDS} granularity.
     * @param initialDelay by default the period starts immediately, but we can shift the
     *                     * start to ensure that the period will run at the certain time (e.g. 00:00 every day)
     */
    public JdkBasedScheduler(
            Runnable task,
            ScheduledExecutorService executor,
            Duration period,
            Duration initialDelay) {

        this.task = task;
        this.executor = executor;
        this.period = period;
        this.initialDelay = initialDelay;
    }

    @Override
    public void start() {
        // Automatically register the Shutdown hook to gracefully
        // close the scheduler when the JVM is shutting down.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        executor.scheduleAtFixedRate(
                new ExceptionHandler(task),
                initialDelay.toMillis(),
                period.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws Exception {
        // Set the Executor into Shutdown state.
        // We don't have any pending tasks in the queue,
        // all threads are running, however, it's still
        // better to gracefully close the executor.
        executor.shutdown();

        // Wait for all tasks to finish the graceful shutdown
        // before we ignore their processing and let the JVM
        // kill them.
        boolean result = executor.awaitTermination(
                DEFAULT_TERMINATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        if (result) {
            LOG.log(INFO, "The Scheduler was terminated gracefully");
        } else {
            LOG.log(ERROR, "Could not finish the scheduler off gracefully");
        }
    }

    /**
     * Decorates the Runnable task, catches and print the exception, otherwise,
     * the scheduled job would be cancelled.
     */
    private record ExceptionHandler(Runnable delegate) implements Runnable {

        @Override
        public void run() {
            try {
                delegate.run();
            } catch (Exception ex) {
                LOG.log(ERROR, "An exception propagated up to the scheduler", ex);
            }
        }
    }
}
