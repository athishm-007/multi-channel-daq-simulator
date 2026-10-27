package com.athish.daq.core;

import com.athish.daq.model.Sample;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base class for an input channel. Each channel runs in its own thread and
 * produces samples at its own rate. Subclasses only decide what the value is.
 */
public abstract class Channel implements Runnable {
    public static final int MIN_RATE_HZ = 1;
    public static final int MAX_RATE_HZ = 100;

    private final String name;
    private final String unit;
    private final String color;
    private volatile int rateHz;
    private volatile boolean enabled = true;
    private volatile boolean running;
    private final AtomicLong produced = new AtomicLong();
    private SampleSink sink;
    private SessionClock clock;
    protected final Random random = new Random();

    protected Channel(String name, String unit, String color, int defaultRateHz) {
        this.name = name;
        this.unit = unit;
        this.color = color;
        this.rateHz = defaultRateHz;
    }

    /** The only method a concrete channel must supply: the reading at time t (seconds). */
    protected abstract double nextValue(double tSeconds);

    public static void validateRate(int hz) throws InvalidRateException {
        if (hz < MIN_RATE_HZ || hz > MAX_RATE_HZ) {
            throw new InvalidRateException(hz, MIN_RATE_HZ, MAX_RATE_HZ);
        }
    }

    public void setRateHz(int hz) throws InvalidRateException {
        validateRate(hz);
        this.rateHz = hz;
    }

    /** Connects the channel to a logger and clock before its thread starts. */
    public void attach(SampleSink sink, SessionClock clock) {
        this.sink = sink;
        this.clock = clock;
        this.produced.set(0);
        this.running = true;
    }

    public void requestStop() { running = false; }

    @Override
    public void run() {
        long next = System.nanoTime();
        int lastRate = rateHz;
        while (running) {
            int rate = rateHz;
            if (rate != lastRate) {              // rate changed live: restart the schedule
                lastRate = rate;
                next = System.nanoTime();
            }
            long period = 1_000_000_000L / rate;
            next += period;
            long wait = next - System.nanoTime();
            if (wait > 0) {
                try {
                    Thread.sleep(wait / 1_000_000L, (int) (wait % 1_000_000L));
                } catch (InterruptedException e) {
                    break;
                }
            } else if (wait < -5 * period) {      // fell far behind: do not burst to catch up
                next = System.nanoTime();
            }
            if (!running) break;
            if (!enabled) continue;
            double t = clock.elapsedMs();
            double v = Math.round(nextValue(t / 1000.0) * 1000.0) / 1000.0;
            try {
                sink.submit(new Sample(t, name, v, unit));
                produced.incrementAndGet();
            } catch (LoggerException e) {
                running = false;                  // logger closed: this channel is finished
            }
        }
    }

    public String getName()        { return name; }
    public String getUnit()        { return unit; }
    public String getColor()       { return color; }
    public int getRateHz()         { return rateHz; }
    public boolean isEnabled()     { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getProduced()      { return produced.get(); }
}
