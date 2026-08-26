package com.watashi.infrastructure.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public final class ThreadManager {

    private ThreadManager() { } // private constructor

    public static ExecutorService virtualPerTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    public static Thread startVirtual(String name, Runnable task) {
        return Thread.ofVirtual()
                .name(name)
                .start(task);
    }

    public static ThreadFactory virtualFactory(String prefix) {
        return Thread.ofVirtual()
                .name(prefix, 0)
                .factory();
    }
}