package com.athish.daq.model;

import java.util.Locale;

/** One immutable reading taken from a channel. Natural order = time order. */
public final class Sample implements Comparable<Sample> {
    private final long index;          // position in the log file (0 until the logger stamps it)
    private final String wallTime;     // clock time, filled in by the logger
    private final double elapsedMs;    // milliseconds since the session started
    private final String channel;
    private final double value;
    private final String unit;

    public Sample(double elapsedMs, String channel, double value, String unit) {
        this(0, "", elapsedMs, channel, value, unit);
    }

    private Sample(long index, String wallTime, double elapsedMs, String channel, double value, String unit) {
        this.index = index;
        this.wallTime = wallTime;
        this.elapsedMs = elapsedMs;
        this.channel = channel;
        this.value = value;
        this.unit = unit;
    }

    /** Returns a copy carrying the file index and wall-clock text assigned at write time. */
    public Sample stamped(long newIndex, String newWallTime) {
        return new Sample(newIndex, newWallTime, elapsedMs, channel, value, unit);
    }

    public long getIndex()        { return index; }
    public String getWallTime()   { return wallTime; }
    public double getElapsedMs()  { return elapsedMs; }
    public String getChannel()    { return channel; }
    public double getValue()      { return value; }
    public String getUnit()       { return unit; }

    @Override
    public int compareTo(Sample other) {
        int byTime = Double.compare(elapsedMs, other.elapsedMs);
        return byTime != 0 ? byTime : channel.compareTo(other.channel);
    }

    public String toCsv() {
        return String.format(Locale.ROOT, "%d,%s,%.3f,%s,%.3f,%s",
                index, wallTime, elapsedMs, channel, value, unit);
    }

    public static Sample fromCsv(String line) {
        String[] p = line.split(",", 6);
        if (p.length < 6) {
            throw new IllegalArgumentException("Bad CSV row: " + line);
        }
        try {
            return new Sample(Long.parseLong(p[0].trim()), p[1], Double.parseDouble(p[2]),
                    p[3], Double.parseDouble(p[4]), p[5].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad number in row: " + line, e);
        }
    }
}
