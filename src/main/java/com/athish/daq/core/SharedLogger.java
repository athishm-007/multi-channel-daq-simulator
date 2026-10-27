package com.athish.daq.core;

import com.athish.daq.model.Sample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The single shared logger. Every channel thread calls submit(); one drainer
 * thread calls flushReady(). All shared state (queue, file, counters) is only
 * touched inside synchronized methods, so there are no races.
 *
 * Time ordering: samples wait in a PriorityQueue sorted by timestamp and are
 * written only once they are older than a small reorder window. By then no
 * slower thread can still be holding an earlier sample, so the file comes out
 * strictly in time order even though channels run at different rates.
 */
public class SharedLogger implements SampleSink {
    private final PriorityQueue<Sample> pending = new PriorityQueue<>();
    private final BufferedWriter out;
    private final SessionClock clock;
    private final double reorderWindowMs;
    private long nextIndex = 1;
    private boolean open = true;

    public SharedLogger(Path file, SessionClock clock, double reorderWindowMs) throws LoggerException {
        this.clock = clock;
        this.reorderWindowMs = reorderWindowMs;
        try {
            boolean fresh = !Files.exists(file) || Files.size(file) == 0;
            out = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (fresh) {
                out.write("index,wall_time,elapsed_ms,channel,value,unit");
                out.newLine();
            }
            out.write("# session started " + clock.startText());
            out.newLine();
            out.flush();
        } catch (IOException e) {
            throw new LoggerException("Cannot open log file " + file, e);
        }
    }

    /** Called by many channel threads at once. */
    @Override
    public synchronized void submit(Sample sample) throws LoggerException {
        if (!open) {
            throw new LoggerException("Logger is closed");
        }
        pending.add(sample);
    }

    /**
     * Writes queued samples in time order and returns them (now stamped with
     * their file index). With all = false only samples older than the reorder
     * window are released; with all = true the queue is emptied.
     */
    public synchronized List<Sample> flushReady(boolean all) throws LoggerException {
        if (!open) {
            throw new LoggerException("Logger is closed");
        }
        double watermark = clock.elapsedMs() - reorderWindowMs;
        List<Sample> written = new ArrayList<>();
        try {
            while (!pending.isEmpty() && (all || pending.peek().getElapsedMs() <= watermark)) {
                Sample s = pending.poll();
                s = s.stamped(nextIndex++, clock.wallTime(s.getElapsedMs()));
                out.write(s.toCsv());
                out.newLine();
                written.add(s);
            }
            out.flush();
        } catch (IOException e) {
            throw new LoggerException("Write to log file failed", e);
        }
        return written;
    }

    public synchronized int getPendingCount() { return pending.size(); }

    public synchronized long getWritten() { return nextIndex - 1; }

    public synchronized void close() throws LoggerException {
        if (!open) return;
        flushReady(true);
        open = false;
        try {
            out.close();
        } catch (IOException e) {
            throw new LoggerException("Closing log file failed", e);
        }
    }

    /** Reads the last 'max' data rows of an existing log file (used to restore the table on startup). */
    public static List<Sample> readTail(Path file, int max) {
        Deque<Sample> tail = new ArrayDeque<>();
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("index")) continue;
                try {
                    tail.addLast(Sample.fromCsv(line));
                    if (tail.size() > max) tail.removeFirst();
                } catch (IllegalArgumentException skip) {
                    // ignore a damaged row, keep reading
                }
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }
        return new ArrayList<>(tail);
    }
}
