package com.sabev.neatologger;

import android.support.annotation.NonNull;

import com.sabev.events.Event;
import com.sabev.events.IEventSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LoggerServiceAsyncTasksExecutor {


    private ExecutorService taskExecutorService;
    private CompletionService<?> taskCompletionService;
    private Map<Future<?>, String> future2key;
    private Map<String, Future<?>> key2future;
    private Event<String> taskFinishedEvent = new Event<>();

    private ExecutorService trackingTaskExecutorService;


    public LoggerServiceAsyncTasksExecutor(ExecutorService taskExecutorService) {
        future2key = new HashMap<>();
        key2future = new HashMap<>();
        this.taskExecutorService = taskExecutorService;
        this.taskCompletionService = new ExecutorCompletionService<Void>(taskExecutorService);
        trackingTaskExecutorService = Executors.newFixedThreadPool(1);
        trackingTaskExecutorService.submit(new TrackingTask());
    }

    public IEventSource<String> getTaskFinishedEvent() {
        return taskFinishedEvent;
    }

    private class TrackingTask implements Runnable {
        @Override
        public void run() {
            while(true) {
                String action = null;
                try {
                    Future<?> finishedTask = taskCompletionService.take();
                    synchronized (LoggerServiceAsyncTasksExecutor.this) {
                        action = future2key.remove(finishedTask);
                        key2future.remove(action);
                    }
                    synchronized (action) {
                        action.notifyAll();
                    }
                } catch (InterruptedException ie) {
                    if (key2future.size() == 0) {
                        break;
                    }
                }
                if (action != null) {
                    taskFinishedEvent.signal(action);
                }
            }
        }
    }

    public synchronized Future<?> get(String actionKey) {
        return key2future.get(actionKey);
    }

    public synchronized int getTaskCount() {
        return key2future.size();
    }

    public void waitForTask(Future<?> task) throws InterruptedException {
        final String action = future2key.get(task);
        if (action == null) {
            return;
        }
        synchronized (action) {
            action.wait();
            action.notifyAll();
        }
    }

    public synchronized Future<?> submit(String action, Runnable task) {
        final Future<?> future = taskCompletionService.submit(task, null);
        future2key.put(future, action);
        key2future.put(action, future);
        return future;
    }

    public void shutdown() {
        taskExecutorService.shutdown();
        trackingTaskExecutorService.shutdown();
    }

    @NonNull
    public List<Runnable> shutdownNow() {
        final List<Runnable> leftFromTaskExecutor = taskExecutorService.shutdownNow();
        final List<Runnable> trackingTaskExecutor = trackingTaskExecutorService.shutdownNow();
        if(BuildConfig.DEBUG && trackingTaskExecutor.size() !=0 && trackingTaskExecutor.size() != 1)
            throw new AssertionError();
        final List<Runnable> totalLeft = new ArrayList<>(leftFromTaskExecutor.size() + trackingTaskExecutor.size());
        totalLeft.addAll(leftFromTaskExecutor);
        totalLeft.addAll(trackingTaskExecutor);
        return totalLeft;
    }
}
