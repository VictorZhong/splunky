package com.wpb.spky.splunk;

public class SplunkConnectivityException extends RuntimeException {

    private final int statusCode;

    public SplunkConnectivityException(String message, Throwable cause) {
        this(message, 0, cause);
    }

    public SplunkConnectivityException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
