package com.athish.daq.core;

import com.athish.daq.model.Sample;

import java.util.List;
import java.util.function.Consumer;

/** Third kind of thread: periodically moves ready samples from the queue to the file and the UI. */
public class LogDrainer implements Runnable {
    public static final long INTERVAL_MS = 50;

    private final SharedLogger logger;
    private final Consumer<List<Sample>> listener;
    private volatile boolean running = true;

    public LogDrainer(SharedLogger logger, Consumer<List<Sample>> listener) {
        this.logger = logger;
        this.listener = listener;
    }

    public void requestStop() { running = false; }

    @Override
    public void run() {
        try {
            while (running) {
                try {
                    Thread.sleep(INTERVAL_MS);
                } catch (InterruptedException e) {
                    break;
                }
                publish(logger.flushReady(false));
            }
            publish(logger.flushReady(true));      // final flush so nothing is lost on Stop
        } catch (LoggerException e) {
            System.err.println("Log drainer stopped: " + e.getMessage());
        }
    }

    private void publish(List<Sample> batch) {
        if (!batch.isEmpty()) {
            listener.accept(batch);
        }
    }
}
