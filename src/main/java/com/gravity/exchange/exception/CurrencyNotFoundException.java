package com.gravity.exchange.exception;

public class CurrencyNotFoundException extends RuntimeException {

    public CurrencyNotFoundException(String currencyCode) {
        super("Currency not found: " + currencyCode);
    }
}
