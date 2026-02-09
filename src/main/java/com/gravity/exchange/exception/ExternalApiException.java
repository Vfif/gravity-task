package com.gravity.exchange.exception;

public class ExternalApiException extends RuntimeException {

    public ExternalApiException(String provider, String message) {
        super("External API error from " + provider + ": " + message);
    }

    public ExternalApiException(String provider, String message, Throwable cause) {
        super("External API error from " + provider + ": " + message, cause);
    }
}
