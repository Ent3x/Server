package org.cloudburstmc.server.scheduler;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.plugin.PluginManager;
import org.cloudburstmc.server.utils.PluginException;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Nukkit Project Team
 */
@Log4j2
@Singleton
public class ServerScheduler {

    private final PluginManager pluginManager;
    private final ForkJoinPool asyncPool;

    private final Queue<TaskHandler> pending;
    private final Map<Integer, ArrayDeque<TaskHandler>> queueMap;
    private final Map<Integer, TaskHandler> taskMap;
    private final AtomicInteger currentTaskId;

    private volatile int currentTick = -1;

    @Inject
    public ServerScheduler(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        this.pending = new ConcurrentLinkedQueue<>();
        this.currentTaskId = new AtomicInteger();
        this.queueMap = new ConcurrentHashMap<>();
        this.taskMap = new ConcurrentHashMap<>();
        this.asyncPool = ForkJoinPool.commonPool();
    }

    public TaskHandler scheduleTask(Task task) {
        return addTask(task, 0, 0, false);
    }

    /**
     * @deprecated Use {@link #scheduleTask(Object, Runnable)}
     */
    @Deprecated
    public TaskHandler scheduleTask(Runnable task) {
        return addTask(null, task, 0, 0, false);
    }

    public TaskHandler scheduleTask(Object plugin, Runnable task) {
        return addTask(plugin, task, 0, 0, false);
    }

    /**
     * @deprecated Use {@link #scheduleTask(Object, Runnable, boolean)}
     */
    @Deprecated
    public TaskHandler scheduleTask(Runnable task, boolean asynchronous) {
        return addTask(null, task, 0, 0, asynchronous);
    }

    public TaskHandler scheduleTask(Object plugin, Runnable task, boolean asynchronous) {
        return addTask(plugin, task, 0, 0, asynchronous);
    }

    /**
     * @deprecated Use {@link #scheduleAsyncTask(Object, AsyncTask)}
     */
    @Deprecated
    public TaskHandler scheduleAsyncTask(AsyncTask task) {
        return addTask(null, task, 0, 0, true);
    }

    public TaskHandler scheduleAsyncTask(Object plugin, AsyncTask task) {
        return addTask(plugin, task, 0, 0, true);
    }

    @Deprecated
    public void scheduleAsyncTaskToWorker(AsyncTask task, int worker) {
        scheduleAsyncTask(task);
    }

    public int getAsyncTaskPoolSize() {
        return asyncPool.getPoolSize();
    }

    public ForkJoinPool getAsyncPool() {
        return asyncPool;
    }

    public TaskHandler scheduleDelayedTask(Task task, int delay) {
        return this.addTask(task, delay, 0, false);
    }

    public TaskHandler scheduleDelayedTask(Task task, int delay, boolean asynchronous) {
        return this.addTask(task, delay, 0, asynchronous);
    }

    /**
     * @deprecated Use {@link #scheduleDelayedTask(Object, Runnable, int)}
     */
    @Deprecated
    public TaskHandler scheduleDelayedTask(Runnable task, int delay) {
        return addTask(null, task, delay, 0, false);
    }

    public TaskHandler scheduleDelayedTask(Object plugin, Runnable task, int delay) {
        return addTask(plugin, task, delay, 0, false);
    }

    /**
     * @deprecated Use {@link #scheduleDelayedTask(Object, Runnable, int, boolean)}
     */
    @Deprecated
    public TaskHandler scheduleDelayedTask(Runnable task, int delay, boolean asynchronous) {
        return addTask(null, task, delay, 0, asynchronous);
    }

    public TaskHandler scheduleDelayedTask(Object plugin, Runnable task, int delay, boolean asynchronous) {
        return addTask(plugin, task, delay, 0, asynchronous);
    }

    /**
     * @deprecated Use {@link #scheduleRepeatingTask(Object, Runnable, int)}
     */
    @Deprecated
    public TaskHandler scheduleRepeatingTask(Runnable task, int period) {
        return addTask(null, task, 0, period, false);
    }

    public TaskHandler scheduleRepeatingTask(Object plugin, Runnable task, int period) {
        return addTask(plugin, task, 0, period, false);
    }

    /**
     * @deprecated Use {@link #scheduleRepeatingTask(Object, Runnable, int, boolean)}
     */
    @Deprecated
    public TaskHandler scheduleRepeatingTask(Runnable task, int period, boolean asynchronous) {
        return addTask(null, task, 0, period, asynchronous);
    }

    public TaskHandler scheduleRepeatingTask(Object plugin, Runnable task, int period, boolean asynchronous) {
        return addTask(plugin, task, 0, period, asynchronous);
    }

    public TaskHandler scheduleRepeatingTask(Task task, int period) {
        return addTask(task, 0, period, false);
    }

    public TaskHandler scheduleRepeatingTask(Task task, int period, boolean asynchronous) {
        return addTask(task, 0, period, asynchronous);
    }

    public TaskHandler scheduleDelayedRepeatingTask(Task task, int delay, int period) {
        return addTask(task, delay, period, false);
    }

    public TaskHandler scheduleDelayedRepeatingTask(Task task, int delay, int period, boolean asynchronous) {
        return addTask(task, delay, period, asynchronous);
    }

    /**
     * @deprecated Use {@link #scheduleDelayedRepeatingTask(Object, Runnable, int, int)}
     */
    @Deprecated
    public TaskHandler scheduleDelayedRepeatingTask(Runnable task, int delay, int period) {
        return addTask(null, task, delay, period, false);
    }

    public TaskHandler scheduleDelayedRepeatingTask(Object plugin, Runnable task, int delay, int period) {
        return addTask(plugin, task, delay, period, false);
    }

    /**
     * @deprecated Use {@link #scheduleDelayedRepeatingTask(Object, Runnable, int, int, boolean)}
     */
    @Deprecated
    public TaskHandler scheduleDelayedRepeatingTask(Runnable task, int delay, int period, boolean asynchronous) {
        return addTask(null, task, delay, period, asynchronous);
    }

    public TaskHandler scheduleDelayedRepeatingTask(Object plugin, Runnable task, int delay, int period, boolean asynchronous) {
        return addTask(plugin, task, delay, period, asynchronous);
    }

    public void cancelTask(int taskId) {
        if (taskMap.containsKey(taskId)) {
            try {
                taskMap.remove(taskId).cancel();
            } catch (RuntimeException ex) {
                log.error("Exception while invoking onCancel", ex);
            }
        }
    }

    public void cancelTask(Object plugin) {
        PluginContainer container = ensurePlugin(plugin);
        for (Map.Entry<Integer, TaskHandler> entry : taskMap.entrySet()) {
            TaskHandler taskHandler = entry.getValue();
            // TODO: Remove the "taskHandler.getPlugin() == null" check
            // It is only there for backwards compatibility!
            if (container == taskHandler.getPlugin()) {
                try {
                    taskHandler.cancel(); /* It will remove from task map automatic in next main heartbeat. */
                } catch (RuntimeException ex) {
                    log.error("Exception while invoking onCancel", ex);
                }
            }
        }
    }

    public void cancelAllTasks() {
        for (Map.Entry<Integer, TaskHandler> entry : this.taskMap.entrySet()) {
            try {
                entry.getValue().cancel();
            } catch (RuntimeException ex) {
                log.error("Exception while invoking onCancel", ex);
            }
        }
        this.taskMap.clear();
        this.queueMap.clear();
        this.currentTaskId.set(0);
    }

    public boolean isQueued(int taskId) {
        return this.taskMap.containsKey(taskId);
    }

    private TaskHandler addTask(Task task, int delay, int period, boolean asynchronous) {
        return addTask(task instanceof PluginTask ? ((PluginTask<?>) task).getOwner() : null, task, delay, period, asynchronous);
    }

    private TaskHandler addTask(Object plugin, Runnable task, int delay, int period, boolean asynchronous) {
        PluginContainer container = plugin != null ? ensurePlugin(plugin) : null;
        if (delay < 0 || period < 0) {
            throw new PluginException("Attempted to register a task with negative delay or period.");
        }

        TaskHandler taskHandler = new TaskHandler(container, task, nextTaskId(), asynchronous);
        taskHandler.setDelay(delay);
        taskHandler.setPeriod(period);
        taskHandler.setNextRunTick(taskHandler.isDelayed() ? currentTick + taskHandler.getDelay() : currentTick);

        if (task instanceof Task) {
            ((Task) task).setHandler(taskHandler);
        }

        pending.offer(taskHandler);
        taskMap.put(taskHandler.getTaskId(), taskHandler);

        return taskHandler;
    }

    public void mainThreadHeartbeat(int currentTick) {
        // Accepts pending.
        TaskHandler task;
        while ((task = pending.poll()) != null) {
            int tick = Math.max(currentTick, task.getNextRunTick()); // Do not schedule in the past
            this.queueMap.computeIfAbsent(tick, integer -> new ArrayDeque<>()).add(task);
        }
        if (currentTick - this.currentTick > queueMap.size()) { // A large number of ticks have passed since the last execution
            for (Map.Entry<Integer, ArrayDeque<TaskHandler>> entry : queueMap.entrySet()) {
                int tick = entry.getKey();
                if (tick <= currentTick) {
                    runTasks(tick);
                }
            }
        } else { // Normal server tick
            for (int i = this.currentTick + 1; i <= currentTick; i++) {
                runTasks(currentTick);
            }
        }
        this.currentTick = currentTick;
        AsyncTask.collectTask();
    }

    private void runTasks(int currentTick) {
        ArrayDeque<TaskHandler> queue = queueMap.remove(currentTick);
        if (queue != null) {
            for (TaskHandler taskHandler : queue) {
                if (taskHandler.isCancelled()) {
                    taskMap.remove(taskHandler.getTaskId());
                    continue;
                } else if (taskHandler.isAsynchronous()) {
                    asyncPool.execute(taskHandler.getTask());
                } else {
                    taskHandler.timing.startTiming();
                    try {
                        taskHandler.run(currentTick);
                    } catch (Throwable e) {
                        log.error("Could not execute taskHandler " + taskHandler.getTaskId(), e);
                    }
                    taskHandler.timing.stopTiming();
                }
                if (taskHandler.isRepeating()) {
                    taskHandler.setNextRunTick(currentTick + taskHandler.getPeriod());
                    pending.offer(taskHandler);
                } else {
                    try {
                        TaskHandler removed = taskMap.remove(taskHandler.getTaskId());
                        if (removed != null) removed.cancel();
                    } catch (RuntimeException ex) {
                        log.error("Exception while invoking onCancel", ex);
                    }
                }
            }
        }
    }

    public int getQueueSize() {
        int size = pending.size();
        for (ArrayDeque<TaskHandler> queue : queueMap.values()) {
            size += queue.size();
        }
        return size;
    }

    private int nextTaskId() {
        return currentTaskId.incrementAndGet();
    }

    @Nonnull
    private PluginContainer ensurePlugin(Object plugin) {
        checkNotNull(plugin, "plugin");
        return this.pluginManager.fromInstance(plugin).orElseThrow(() ->
                new IllegalArgumentException("Object is not a registered plugin"));
    }

    public static class ExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            log.fatal("Exception in scheduled task on thread: " + t.getName(), e);
        }
    }
}
