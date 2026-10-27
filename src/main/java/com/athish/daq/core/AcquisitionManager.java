package com.athish.daq.core;

import com.athish.daq.channels.PressureChannel;
import com.athish.daq.channels.TemperatureChannel;
import com.athish.daq.channels.VibrationChannel;
import com.athish.daq.channels.VoltageChannel;
import com.athish.daq.model.Sample;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Owns the channels, the shared logger and every thread; starts and stops a session. */
public class AcquisitionManager {
    public static final double REORDER_WINDOW_MS = 100;

    private final Path logFile;
    private final Consumer<List<Sample>> listener;
    private final Map<String, Channel> channels = new LinkedHashMap<>();
    private final List<Thread> channelThreads = new ArrayList<>();
    private SharedLogger logger;
    private LogDrainer drainer;
    private Thread drainThread;
    private boolean running;

    public AcquisitionManager(Path logFile, Consumer<List<Sample>> listener) {
        this.logFile = logFile;
        this.listener = listener;
        // Polymorphism: the manager only ever sees the Channel base type.
        List<Channel> all = List.of(new TemperatureChannel(), new PressureChannel(),
                new VoltageChannel(), new VibrationChannel());
        for (Channel c : all) {
            channels.put(c.getName(), c);
        }
    }

    public synchronized void start() throws LoggerException {
        if (running) return;
        SessionClock clock = new SessionClock();
        logger = new SharedLogger(logFile, clock, REORDER_WINDOW_MS);
        drainer = new LogDrainer(logger, listener);
        drainThread = new Thread(drainer, "log-drainer");
        drainThread.setDaemon(true);
        channelThreads.clear();
        for (Channel c : channels.values()) {
            c.attach(logger, clock);
            Thread t = new Thread(c, "channel-" + c.getName());
            t.setDaemon(true);
            channelThreads.add(t);
        }
        drainThread.start();
        for (Thread t : channelThreads) t.start();
        running = true;
    }

    /** Stops all threads, flushes the file and returns how many rows this session wrote. */
    public synchronized long stop() {
        if (!running) return 0;
        for (Channel c : channels.values()) c.requestStop();
        for (Thread t : channelThreads) {
            t.interrupt();
            join(t);
        }
        drainer.requestStop();
        drainThread.interrupt();
        join(drainThread);
        long written = logger.getWritten();
        try {
            logger.close();
        } catch (LoggerException e) {
            System.err.println(e.getMessage());
        }
        running = false;
        return written;
    }

    private static void join(Thread t) {
        try {
            t.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized boolean isRunning()   { return running; }
    public Path getLogFile()                   { return logFile; }
    public List<Channel> getChannels()         { return new ArrayList<>(channels.values()); }
    public Channel getChannel(String name)     { return channels.get(name); }

    public synchronized long getWritten()      { return logger == null ? 0 : logger.getWritten(); }
    public synchronized int getPending()       { return (logger == null || !running) ? 0 : logger.getPendingCount(); }
}
