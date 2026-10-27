package com.athish.daq.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** One shared time base so every channel stamps samples against the same zero. */
public final class SessionClock {
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final long startNanos = System.nanoTime();
    private final Instant startInstant = Instant.now();

    /** Milliseconds since the session began (monotonic, never goes backwards). */
    public double elapsedMs() {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    public String wallTime(double elapsedMs) {
        return FMT.format(startInstant.plusNanos((long) (elapsedMs * 1_000_000)));
    }

    public String startText() {
        return FMT.format(startInstant);
    }
}
