package com.athish.daq.core;

/** User-defined checked exception for every logger failure (closed logger, disk error). */
public class LoggerException extends Exception {
    private static final long serialVersionUID = 1L;

    public LoggerException(String message) { super(message); }
    public LoggerException(String message, Throwable cause) { super(message, cause); }
}
