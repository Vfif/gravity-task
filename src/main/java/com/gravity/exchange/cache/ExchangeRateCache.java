package com.gravity.exchange.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for the best (most recent) exchange rates.
 * Key format: "BASE_TARGET" (e.g., "USD_EUR").
 */
@Slf4j
@Component
public class ExchangeRateCache {

    private final ConcurrentHashMap<String, BigDecimal> rateCache = new ConcurrentHashMap<>();

    /**
     * Gets the best rate for a given currency pair.
     *
     * @param from source currency code
     * @param to   target currency code
     * @return the cached rate, or empty if not found
     */
    public Optional<BigDecimal> getBestRate(String from, String to) {
        return Optional.ofNullable(rateCache.get(buildKey(from, to)));
    }

    /**
     * Updates a single rate in the cache.
     *
     * @param from source currency code
     * @param to   target currency code
     * @param rate the exchange rate
     */
    public void putRate(String from, String to, BigDecimal rate) {
        String key = buildKey(from, to);
        rateCache.put(key, rate);
        log.debug("Cache updated: {} = {}", key, rate);
    }

    /**
     * Updates multiple rates in the cache.
     *
     * @param baseCurrency the base currency code
     * @param rates        map of target currency to rate
     */
    public void updateRates(String baseCurrency, Map<String, BigDecimal> rates) {
        rates.forEach((target, rate) -> putRate(baseCurrency, target, rate));
    }

    /**
     * Returns an unmodifiable view of all cached rates.
     *
     * @return all cached rates
     */
    public Map<String, BigDecimal> getAllRates() {
        return Collections.unmodifiableMap(rateCache);
    }

    /**
     * Clears all cached rates.
     */
    public void clear() {
        rateCache.clear();
        log.info("Exchange rate cache cleared");
    }

    private String buildKey(String from, String to) {
        return from.toUpperCase() + "_" + to.toUpperCase();
    }
}
