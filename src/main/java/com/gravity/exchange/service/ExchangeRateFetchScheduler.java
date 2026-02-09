package com.gravity.exchange.service;

import com.gravity.exchange.cache.ExchangeRateCache;
import com.gravity.exchange.client.ExchangeRateProvider;
import com.gravity.exchange.entity.ExchangeRate;
import com.gravity.exchange.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateFetchScheduler {

    private final List<ExchangeRateProvider> providers;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateCache exchangeRateCache;
    private final CurrencyService currencyService;

    /**
     * Fetches exchange rates on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Fetching exchange rates on startup...");
        fetchAndUpdateAllRates();
    }

    /**
     * Scheduled task: fetches exchange rates every hour.
     */
    @Scheduled(fixedRateString = "${exchange.scheduler.fixed-rate}")
    public void scheduledFetch() {
        log.info("Scheduled exchange rate fetch triggered");
        fetchAndUpdateAllRates();
    }

    /**
     * Triggers a manual refresh of exchange rates.
     */
    public void refreshRates() {
        log.info("Manual exchange rate refresh triggered");
        fetchAndUpdateAllRates();
    }

    /**
     * Fetches rates from all providers for all active currencies,
     * persists them to the database, and updates the in-memory cache
     * with the best rates.
     */
    private void fetchAndUpdateAllRates() {
        List<String> activeCurrencies = currencyService.getActiveCurrencyCodes();
        if (activeCurrencies.isEmpty()) {
            log.info("No active currencies configured, skipping rate fetch");
            return;
        }

        Set<String> currencySet = new HashSet<>(activeCurrencies);

        // For each currency as base, fetch rates against all other currencies
        // We'll use each currency as a base and fetch rates to all others
        Map<String, Map<String, BigDecimal>> bestRates = new HashMap<>();

        for (String baseCurrency : activeCurrencies) {
            Set<String> targets = currencySet.stream()
                    .filter(c -> !c.equals(baseCurrency))
                    .collect(Collectors.toSet());

            if (targets.isEmpty()) {
                continue;
            }

            Map<String, BigDecimal> bestForBase = new HashMap<>();

            for (ExchangeRateProvider provider : providers) {
                try {
                    Map<String, BigDecimal> rates = provider.fetchRates(baseCurrency, targets);

                    // Persist all fetched rates
                    LocalDateTime now = LocalDateTime.now();
                    List<ExchangeRate> entities = rates.entrySet().stream()
                            .map(entry -> ExchangeRate.builder()
                                    .baseCurrency(baseCurrency)
                                    .targetCurrency(entry.getKey())
                                    .rate(entry.getValue())
                                    .source(provider.getProviderName())
                                    .timestamp(now)
                                    .build())
                            .collect(Collectors.toList());
                    exchangeRateRepository.saveAll(entities);

                    // Track the best (highest) rate for each target currency
                    rates.forEach((target, rate) ->
                            bestForBase.merge(target, rate, BigDecimal::max));

                    log.info("Fetched {} rates from {} for base {}",
                            rates.size(), provider.getProviderName(), baseCurrency);
                } catch (Exception ex) {
                    log.error("Failed to fetch rates from {} for base {}: {}",
                            provider.getProviderName(), baseCurrency, ex.getMessage());
                    // Continue with remaining providers
                }
            }

            bestRates.put(baseCurrency, bestForBase);
        }

        // Update cache with best rates
        bestRates.forEach((base, ratesMap) ->
                exchangeRateCache.updateRates(base, ratesMap));

        long totalCached = bestRates.values().stream()
                .mapToLong(m -> m.size())
                .sum();
        log.info("Exchange rate update complete. Cached {} best rates", totalCached);
    }
}
