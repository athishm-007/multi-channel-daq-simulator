package com.athish.daq.core;

/** User-defined checked exception thrown when a sampling rate is outside the allowed range. */
public class InvalidRateException extends Exception {
    private static final long serialVersionUID = 1L;
    private final int requested;

    public InvalidRateException(int requested, int min, int max) {
        super("Rate " + requested + " Hz is not allowed (use " + min + " to " + max + " Hz)");
        this.requested = requested;
    }

    public int getRequested() { return requested; }
}
