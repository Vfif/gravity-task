package com.gravity.exchange.client;

import com.gravity.exchange.client.dto.ErApiResponse;
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
public class ExchangeRateApiClient implements ExchangeRateProvider {

    private static final String PROVIDER_NAME = "EXCHANGERATE_API";
    private final RestClient restClient;

    public ExchangeRateApiClient(@Value("${exchange.providers.exchangerate-api.url}") String baseUrl,
                                  RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Map<String, BigDecimal> fetchRates(String baseCurrency, Set<String> targetCurrencies) {
        if (targetCurrencies.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            log.info("Fetching rates from ExchangeRate-API: base={}", baseCurrency);
            ErApiResponse response = restClient.get()
                    .uri("/v6/latest/{base}", baseCurrency)
                    .retrieve()
                    .body(ErApiResponse.class);

            return Optional.ofNullable(response)
                    .map(ErApiResponse::getRates)
                    .map(rates -> rates.entrySet().stream()
                            .filter(e -> targetCurrencies.contains(e.getKey()))
                            .filter(e -> !e.getKey().equals(baseCurrency))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .orElse(Collections.emptyMap());
        } catch (Exception ex) {
            log.error("Failed to fetch rates from ExchangeRate-API: {}", ex.getMessage());
            throw new ExternalApiException(PROVIDER_NAME, ex.getMessage(), ex);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
