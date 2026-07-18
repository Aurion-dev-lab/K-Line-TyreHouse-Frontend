package com.gui.kline.utils;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Utility for running background tasks off the JavaFX Application Thread
 * to prevent UI lag. Uses a shared thread pool.
 */
public final class BackgroundTask {

    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "background-task");
        t.setDaemon(true);
        return t;
    });

    private BackgroundTask() {}

    /**
     * Run a background task. The task runs off the FX thread.
     * onSuccess is called back on the FX thread with the result.
     * onFailure is called on the FX thread with the exception.
     */
    public static <T> void run(
            java.util.concurrent.Callable<T> backgroundWork,
            Consumer<T> onSuccess,
            Consumer<Throwable> onFailure) {

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return backgroundWork.call();
            }
        };

        task.setOnSucceeded(e -> {
            if (onSuccess != null) {
                Platform.runLater(() -> onSuccess.accept(task.getValue()));
            }
        });

        task.setOnFailed(e -> {
            if (onFailure != null) {
                Platform.runLater(() -> onFailure.accept(task.getException()));
            } else {
                Throwable ex = task.getException();
                System.err.println("Background task failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        POOL.submit(task);
    }

    /**
     * Run a background task with only success callback (failures logged).
     */
    public static <T> void run(
            java.util.concurrent.Callable<T> backgroundWork,
            Consumer<T> onSuccess) {
        run(backgroundWork, onSuccess, null);
    }

    /**
     * Run a void background task.
     */
    public static void runVoid(
            Runnable backgroundWork,
            Runnable onSuccess,
            Consumer<Throwable> onFailure) {
        run(() -> {
            backgroundWork.run();
            return true;
        }, ignored -> {
            if (onSuccess != null) onSuccess.run();
        }, onFailure);
    }

    /**
     * Run a void background task with only success callback.
     */
    public static void runVoid(Runnable backgroundWork, Runnable onSuccess) {
        runVoid(backgroundWork, onSuccess, null);
    }

    /**
     * Shutdown the thread pool (call on app exit).
     */
    public static void shutdown() {
        POOL.shutdown();
    }
}