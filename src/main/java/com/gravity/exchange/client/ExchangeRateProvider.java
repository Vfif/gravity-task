package com.gravity.exchange.client;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Interface for external exchange rate providers.
 */
public interface ExchangeRateProvider {

    /**
     * Fetches exchange rates for the given base currency against the specified target currencies.
     *
     * @param baseCurrency     the base currency code (e.g., "USD")
     * @param targetCurrencies the set of target currency codes
     * @return a map of target currency code to exchange rate
     */
    Map<String, BigDecimal> fetchRates(String baseCurrency, Set<String> targetCurrencies);

    /**
     * Returns the name of this provider for identification and logging.
     *
     * @return the provider name
     */
    String getProviderName();
}
