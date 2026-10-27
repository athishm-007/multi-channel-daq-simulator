package com.athish.daq.core;

import com.athish.daq.model.Sample;

/** Anything that can receive samples from a channel thread. */
public interface SampleSink {
    void submit(Sample sample) throws LoggerException;
}
