package com.gravity.exchange.exception;

public class ExchangeRateNotFoundException extends RuntimeException {

    public ExchangeRateNotFoundException(String from, String to) {
        super("Exchange rate not found for " + from + " -> " + to);
    }
}
