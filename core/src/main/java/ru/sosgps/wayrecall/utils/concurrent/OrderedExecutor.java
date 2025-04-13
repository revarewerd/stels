package ru.sosgps.wayrecall.utils.concurrent;

import org.slf4j.Logger;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An executor that make sure tasks submitted with the same key
 * will be executed in the same order as task submission
 * (order of calling the {@link #submit(Object, Runnable)} method).
 * <p/>
 * Tasks submitted will be run in the given {@link Executor}.
 * There is no restriction on how many threads in the given {@link Executor}
 * needs to have (it can be single thread executor as well as a cached thread pool).
 * <p/>
 * If there are more than one thread in the given {@link Executor}, tasks
 * submitted with different keys may be executed in parallel, but never
 * for tasks submitted with the same key.
 * <p/>
 * * @param <K> type of keys.
 */
public class OrderedExecutor<K> {

    private static Logger log = org.slf4j.LoggerFactory.getLogger(OrderedExecutor.class);

    private final Executor executor;
    private final Map<K, Reference<Task>> tasks;

    /**
     * Constructs a {@code OrderedExecutor}.
     *
     * @param executor tasks will be run in this executor.
     */
    public OrderedExecutor(Executor executor) {
        this.executor = executor;
        this.tasks = new HashMap<>();
    }

    /**
     * Adds a new task to run for the given key.
     *
     * @param key      the key for applying tasks ordering.
     * @param runnable the task to run.
     */
    public synchronized void submit(K key, Runnable runnable) {
        //log.debug("summitting {} {}", key, runnable);
        Reference<Task> reference = tasks.get(key);
        Task task = reference != null ? reference.get() : null;
        if (task == null) {
            task = new Task(key);
            tasks.put(key, new SoftReference<Task>(task));
        }
        task.add(runnable);
    }

//    public synchronized int sumPending() {
//        return tasks.values().stream()
//                .mapToInt(
//                        r -> Optional.ofNullable(r.get())
//                                .map(task -> task.queue.size())
//                                .orElse(0))
//                .sum();
//    }

    /**
     * Private inner class for running tasks for each key.
     * Each key submitted will have one instance of this class.
     */
    private class Task implements Runnable {

        private final Lock lock;
        private final Queue<Runnable> queue;
        volatile boolean scheduled = false;

        K key;

        Task(K key) {
            this.key = key;
            this.lock = new ReentrantLock();
            this.queue = new LinkedList<Runnable>();
        }

        public void add(Runnable runnable) {
            boolean runTask;
            lock.lock();
            try {
                // Run only if no job is running.
                runTask = queue.isEmpty();
                queue.offer(runnable);
                if (queue.size() > 200)
                    log.debug("{} key={} task added queue.size={} scheduled = {}", this, key, queue.size(), scheduled);
            } finally {
                lock.unlock();
            }
            if (runTask) {
                executor.execute(this);
                scheduled = true;
            }
        }

        @Override
        public void run() {
            scheduled = false;
            // Pick a task to run.
            Runnable runnable;
            lock.lock();
            try {
                runnable = queue.peek();
                //log.debug("processing {} {}", key, runnable);
            } finally {
                lock.unlock();
            }
            try {
                if (runnable != null)
                    runnable.run();
                //log.debug("finished {} {}", key, runnable);
            } catch (Throwable ex) {
                log.error("error processig task key=" + key, ex);
                ex.printStackTrace();
                if(!(ex instanceof Exception))
                    throw ex;
            }
            finally {
                // Check to see if there are queued task, if yes, submit for execution.
                lock.lock();
                try {
                    queue.poll();
                    if (queue.size() > 200)
                        log.debug("{} key={} task processed sheduling next queue.size={}", this, key, queue.size());
                    if (!queue.isEmpty()) {
                        executor.execute(this);
                        scheduled = true;
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    public Executor getExecutor() {
        return executor;
    }
}
