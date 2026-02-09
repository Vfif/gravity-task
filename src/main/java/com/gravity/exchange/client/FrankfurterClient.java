package com.gravity.exchange.client;

import com.gravity.exchange.client.dto.FrankfurterResponse;
import com.gravity.exchange.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FrankfurterClient implements ExchangeRateProvider {

    private static final String PROVIDER_NAME = "FRANKFURTER";
    private final RestClient restClient;

    public FrankfurterClient(@Value("${exchange.providers.frankfurter.url}") String baseUrl,
                             RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Map<String, BigDecimal> fetchRates(String baseCurrency, Set<String> targetCurrencies) {
        if (targetCurrencies.isEmpty()) {
            return Collections.emptyMap();
        }

        String symbols = targetCurrencies.stream()
                .filter(c -> !c.equals(baseCurrency))
                .collect(Collectors.joining(","));

        if (symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            log.info("Fetching rates from Frankfurter: base={}, symbols={}", baseCurrency, symbols);
            FrankfurterResponse response = restClient.get()
                    .uri("/v1/latest?base={base}&symbols={symbols}", baseCurrency, symbols)
                    .retrieve()
                    .body(FrankfurterResponse.class);

            return Optional.ofNullable(response)
                    .map(FrankfurterResponse::getRates)
                    .orElse(Collections.emptyMap());
        } catch (Exception ex) {
            log.error("Failed to fetch rates from Frankfurter: {}", ex.getMessage());
            throw new ExternalApiException(PROVIDER_NAME, ex.getMessage(), ex);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
