package com.gravity.exchange.client;

import com.gravity.exchange.client.dto.MockProviderResponse;
import com.gravity.exchange.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class MockProviderClient implements ExchangeRateProvider {

    private final String providerName;
    private final RestClient restClient;

    public MockProviderClient(String providerName, String baseUrl, RestClient.Builder restClientBuilder) {
        this.providerName = providerName;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Map<String, BigDecimal> fetchRates(String baseCurrency, Set<String> targetCurrencies) {
        if (targetCurrencies.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            log.info("Fetching rates from {}: base={}", providerName, baseCurrency);
            MockProviderResponse response = restClient.get()
                    .uri("/api/rates?base={base}", baseCurrency)
                    .retrieve()
                    .body(MockProviderResponse.class);

            return Optional.ofNullable(response)
                    .map(MockProviderResponse::getRates)
                    .map(rates -> rates.entrySet().stream()
                            .filter(e -> targetCurrencies.contains(e.getKey()))
                            .filter(e -> !e.getKey().equals(baseCurrency))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .orElse(Collections.emptyMap());
        } catch (Exception ex) {
            log.error("Failed to fetch rates from {}: {}", providerName, ex.getMessage());
            throw new ExternalApiException(providerName, ex.getMessage(), ex);
        }
    }

    @Override
    public String getProviderName() {
        return providerName;
    }
}
