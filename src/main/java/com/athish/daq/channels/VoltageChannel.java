package com.athish.daq.channels;

import com.athish.daq.core.Channel;

/** ADC voltage input: 0.5 Hz sine riding on 3.3 V. */
public class VoltageChannel extends Channel {
    public VoltageChannel() { super("Voltage", "V", "#d4af37", 10); }

    @Override
    protected double nextValue(double t) {
        return 3.3 + 1.2 * Math.sin(2 * Math.PI * 0.5 * t) + random.nextGaussian() * 0.02;
    }
}
