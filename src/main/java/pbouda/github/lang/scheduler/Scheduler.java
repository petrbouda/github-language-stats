package pbouda.github.lang.scheduler;

public interface Scheduler extends AutoCloseable{

    /**
     * This method automatically starts the scheduler. If the scheduler is a recurrent scheduler then this is the method
     * enqueues and starts periodical execution. First execution does not have to be scheduled immediately, it depends on
     * an implementation of {@link Scheduler}.
     */
    void start();

}
