package com.gravity.exchange.service;

import com.gravity.exchange.cache.ExchangeRateCache;
import com.gravity.exchange.client.ExchangeRateProvider;
import com.gravity.exchange.entity.Currency;
import com.gravity.exchange.entity.ExchangeRate;
import com.gravity.exchange.repository.ExchangeRateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateFetchSchedulerTest {

    @Mock
    private List<ExchangeRateProvider> providers;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private ExchangeRateCache exchangeRateCache;

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private ExchangeRateFetchScheduler scheduler;

    @Test
    @DisplayName("Should skip fetch when no active currencies")
    void refreshRates_noActiveCurrencies_skips() {
        when(currencyService.getActiveCurrencyCodes()).thenReturn(List.of());

        scheduler.refreshRates();

        verify(exchangeRateRepository, never()).saveAll(any());
        verify(exchangeRateCache, never()).updateRates(anyString(), anyMap());
    }
}
