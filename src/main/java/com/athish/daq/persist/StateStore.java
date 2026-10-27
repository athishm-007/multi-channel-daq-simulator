package com.athish.daq.persist;

import com.athish.daq.core.Channel;
import com.athish.daq.core.InvalidRateException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Properties;

/** Saves channel settings and lifetime counters so they survive a restart. */
public class StateStore {
    private final Path file;
    private final Properties props = new Properties();

    public StateStore(Path file) {
        this.file = file;
    }

    public void load(Collection<Channel> channels) {
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Could not read saved state: " + e.getMessage());
            }
        }
        for (Channel c : channels) {
            String rate = props.getProperty(c.getName() + ".rate");
            if (rate != null) {
                try {
                    c.setRateHz(Integer.parseInt(rate.trim()));
                } catch (NumberFormatException | InvalidRateException e) {
                    System.err.println("Ignoring bad saved rate for " + c.getName());
                }
            }
            String en = props.getProperty(c.getName() + ".enabled");
            if (en != null) {
                c.setEnabled(Boolean.parseBoolean(en));
            }
        }
    }

    public void save(Collection<Channel> channels) throws IOException {
        for (Channel c : channels) {
            props.setProperty(c.getName() + ".rate", String.valueOf(c.getRateHz()));
            props.setProperty(c.getName() + ".enabled", String.valueOf(c.isEnabled()));
        }
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "DAQ simulator saved state");
        }
    }

    public long getLong(String key, long fallback) {
        try {
            return Long.parseLong(props.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public void putLong(String key, long value) {
        props.setProperty(key, String.valueOf(value));
    }
}
