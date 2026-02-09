package com.gravity.exchange.exception;

public class InvalidPeriodException extends RuntimeException {

    public InvalidPeriodException(String period) {
        super("Invalid period format: " + period + ". Expected formats: 12H, 10D, 3M, 1Y");
    }
}
