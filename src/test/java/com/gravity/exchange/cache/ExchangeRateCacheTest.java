package com.gravity.exchange.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateCacheTest {

    private ExchangeRateCache cache;

    @BeforeEach
    void setUp() {
        cache = new ExchangeRateCache();
    }

    @Test
    @DisplayName("Should return empty Optional when rate not cached")
    void getBestRate_whenNotCached_returnsEmpty() {
        Optional<BigDecimal> rate = cache.getBestRate("USD", "EUR");
        assertThat(rate).isEmpty();
    }

    @Test
    @DisplayName("Should return rate when cached")
    void getBestRate_whenCached_returnsRate() {
        cache.putRate("USD", "EUR", new BigDecimal("0.92"));

        Optional<BigDecimal> rate = cache.getBestRate("USD", "EUR");

        assertThat(rate).isPresent();
        assertThat(rate.get()).isEqualByComparingTo(new BigDecimal("0.92"));
    }

    @Test
    @DisplayName("Should update existing rate")
    void putRate_updatesExistingRate() {
        cache.putRate("USD", "EUR", new BigDecimal("0.90"));
        cache.putRate("USD", "EUR", new BigDecimal("0.95"));

        Optional<BigDecimal> rate = cache.getBestRate("USD", "EUR");

        assertThat(rate).isPresent();
        assertThat(rate.get()).isEqualByComparingTo(new BigDecimal("0.95"));
    }

    @Test
    @DisplayName("Should update multiple rates at once")
    void updateRates_updatesMultipleRates() {
        Map<String, BigDecimal> rates = Map.of(
                "EUR", new BigDecimal("0.92"),
                "GBP", new BigDecimal("0.79"),
                "JPY", new BigDecimal("149.55")
        );

        cache.updateRates("USD", rates);

        assertThat(cache.getBestRate("USD", "EUR")).hasValue(new BigDecimal("0.92"));
        assertThat(cache.getBestRate("USD", "GBP")).hasValue(new BigDecimal("0.79"));
        assertThat(cache.getBestRate("USD", "JPY")).hasValue(new BigDecimal("149.55"));
    }

    @Test
    @DisplayName("Should return all cached rates")
    void getAllRates_returnsAllCachedRates() {
        cache.putRate("USD", "EUR", new BigDecimal("0.92"));
        cache.putRate("USD", "GBP", new BigDecimal("0.79"));

        Map<String, BigDecimal> allRates = cache.getAllRates();

        assertThat(allRates).hasSize(2);
        assertThat(allRates).containsKey("USD_EUR");
        assertThat(allRates).containsKey("USD_GBP");
    }

    @Test
    @DisplayName("Should handle case-insensitive currency codes")
    void getBestRate_caseInsensitive() {
        cache.putRate("usd", "eur", new BigDecimal("0.92"));

        Optional<BigDecimal> rate = cache.getBestRate("USD", "EUR");

        assertThat(rate).isPresent();
        assertThat(rate.get()).isEqualByComparingTo(new BigDecimal("0.92"));
    }

    @Test
    @DisplayName("Should clear all cached rates")
    void clear_removesAllRates() {
        cache.putRate("USD", "EUR", new BigDecimal("0.92"));
        cache.putRate("USD", "GBP", new BigDecimal("0.79"));

        cache.clear();

        assertThat(cache.getAllRates()).isEmpty();
        assertThat(cache.getBestRate("USD", "EUR")).isEmpty();
    }
}
