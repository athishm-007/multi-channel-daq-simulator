package com.athish.daq.channels;

import com.athish.daq.core.Channel;

/** Pressure sensor modelled as a bounded random walk around 101.3 kPa. */
public class PressureChannel extends Channel {
    private double level = 101.3;

    public PressureChannel() { super("Pressure", "kPa", "#4fc3f7", 5); }

    @Override
    protected double nextValue(double t) {
        level += random.nextGaussian() * 0.05;
        level = Math.max(95.0, Math.min(108.0, level));
        return level;
    }
}
