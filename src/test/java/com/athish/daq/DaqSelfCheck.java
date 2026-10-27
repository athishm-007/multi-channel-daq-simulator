package com.athish.daq;

import com.athish.daq.core.AcquisitionManager;
import com.athish.daq.core.Channel;
import com.athish.daq.core.InvalidRateException;
import com.athish.daq.model.Sample;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Headless check (no JavaFX needed): runs 4 threads for 4 s and verifies the log file. */
public class DaqSelfCheck {
    public static void main(String[] args) throws Exception {
        Path file = Files.createTempFile("daq", ".csv");
        Files.delete(file);
        AtomicLong seenByUi = new AtomicLong();
        AcquisitionManager m = new AcquisitionManager(file, batch -> seenByUi.addAndGet(batch.size()));

        boolean rejected = false;
        try { m.getChannels().get(0).setRateHz(500); } catch (InvalidRateException e) { rejected = true; }
        check(rejected, "rate 500 Hz must be rejected");

        m.start();
        Thread.sleep(4000);
        long written = m.stop();

        List<Sample> rows = SharedLoggerReader.all(file);
        check(rows.size() == written, "rows in file (" + rows.size() + ") == rows written (" + written + ")");
        check(seenByUi.get() == written, "UI listener saw every row: " + seenByUi.get());
        boolean ordered = true;
        for (int i = 1; i < rows.size(); i++) {
            if (rows.get(i).getElapsedMs() < rows.get(i - 1).getElapsedMs()) ordered = false;
            if (rows.get(i).getIndex() != i + 1) ordered = false;
        }
        check(ordered, "file is strictly time-ordered with sequential indexes");
        for (Channel c : m.getChannels()) {
            long n = rows.stream().filter(s -> s.getChannel().equals(c.getName())).count();
            long expected = c.getRateHz() * 4L;
            System.out.printf("  %-12s %3d Hz  rows=%d (expected about %d)%n", c.getName(), c.getRateHz(), n, expected);
            check(Math.abs(n - expected) <= expected * 0.2 + 3, c.getName() + " count close to rate x time");
            check(n == c.getProduced(), c.getName() + " produced == logged");
        }
        System.out.println("ALL CHECKS PASSED");
    }

    private static void check(boolean ok, String what) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + what);
        if (!ok) System.exit(1);
    }

    /** Tiny helper so the test reads the file the same way the UI restores it. */
    static class SharedLoggerReader {
        static List<Sample> all(Path f) { return com.athish.daq.core.SharedLogger.readTail(f, Integer.MAX_VALUE); }
    }
}
