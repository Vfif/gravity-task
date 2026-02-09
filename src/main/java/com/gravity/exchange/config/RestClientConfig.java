package com.gravity.exchange.config;

import com.gravity.exchange.client.ExchangeRateProvider;
import com.gravity.exchange.client.MockProviderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean("mockProvider1")
    public ExchangeRateProvider mockProvider1(
            @Value("${exchange.providers.mock-provider-1.url}") String baseUrl,
            RestClient.Builder restClientBuilder) {
        return new MockProviderClient("MOCK_PROVIDER_1", baseUrl, restClientBuilder);
    }

    @Bean("mockProvider2")
    public ExchangeRateProvider mockProvider2(
            @Value("${exchange.providers.mock-provider-2.url}") String baseUrl,
            RestClient.Builder restClientBuilder) {
        return new MockProviderClient("MOCK_PROVIDER_2", baseUrl, restClientBuilder);
    }

    @Bean
    public List<ExchangeRateProvider> exchangeRateProviders(List<ExchangeRateProvider> providers) {
        return providers;
    }
}
