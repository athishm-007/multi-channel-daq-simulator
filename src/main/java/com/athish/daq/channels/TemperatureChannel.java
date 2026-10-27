package com.athish.daq.channels;

import com.athish.daq.core.Channel;

/** Slow thermocouple: gentle drift around 28 C with a little noise. */
public class TemperatureChannel extends Channel {
    public TemperatureChannel() { super("Temperature", "C", "#ff8a65", 2); }

    @Override
    protected double nextValue(double t) {
        return 28.0 + 3.0 * Math.sin(t / 20.0) + random.nextGaussian() * 0.1;
    }
}
