package com.gravity.exchange.exception;

public class CurrencyAlreadyExistsException extends RuntimeException {

    public CurrencyAlreadyExistsException(String currencyCode) {
        super("Currency already exists: " + currencyCode);
    }
}
