package com.athish.daq.channels;

import com.athish.daq.core.Channel;

/** Accelerometer axis: a 3 Hz tone buried in noise. Fastest channel. */
public class VibrationChannel extends Channel {
    public VibrationChannel() { super("Vibration", "g", "#aed581", 20); }

    @Override
    protected double nextValue(double t) {
        return 0.5 * Math.sin(2 * Math.PI * 3.0 * t) + random.nextGaussian() * 0.15;
    }
}
