package com.gravity.exchange.service;

import com.gravity.exchange.cache.ExchangeRateCache;
import com.gravity.exchange.dto.ConversionResponse;
import com.gravity.exchange.dto.TrendResponse;
import com.gravity.exchange.entity.ExchangeRate;
import com.gravity.exchange.exception.ExchangeRateNotFoundException;
import com.gravity.exchange.exception.InvalidPeriodException;
import com.gravity.exchange.repository.ExchangeRateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private ExchangeRateCache exchangeRateCache;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("Should convert amount successfully")
    void convert_success() {
        when(exchangeRateCache.getBestRate("USD", "EUR"))
                .thenReturn(Optional.of(new BigDecimal("0.92000000")));

        ConversionResponse result = exchangeRateService.convert(
                new BigDecimal("100"), "USD", "EUR");

        assertThat(result.getFrom()).isEqualTo("USD");
        assertThat(result.getTo()).isEqualTo("EUR");
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(result.getConvertedAmount()).isEqualByComparingTo(new BigDecimal("92.00000000"));
        assertThat(result.getRate()).isEqualByComparingTo(new BigDecimal("0.92000000"));
    }

    @Test
    @DisplayName("Should throw exception when exchange rate not found for conversion")
    void convert_throwsWhenRateNotFound() {
        when(exchangeRateCache.getBestRate("USD", "XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exchangeRateService.convert(
                new BigDecimal("100"), "USD", "XYZ"))
                .isInstanceOf(ExchangeRateNotFoundException.class);
    }

    @Test
    @DisplayName("Should handle lowercase currency codes in conversion")
    void convert_handlesLowercase() {
        when(exchangeRateCache.getBestRate("USD", "EUR"))
                .thenReturn(Optional.of(new BigDecimal("0.92")));

        ConversionResponse result = exchangeRateService.convert(
                new BigDecimal("50"), "usd", "eur");

        assertThat(result.getFrom()).isEqualTo("USD");
        assertThat(result.getTo()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("Should calculate trend over 12H period")
    void getTrend_12H_success() {
        ExchangeRate oldRate = ExchangeRate.builder()
                .baseCurrency("USD").targetCurrency("EUR")
                .rate(new BigDecimal("0.90000000"))
                .timestamp(LocalDateTime.now().minusHours(11))
                .build();
        ExchangeRate latestRate = ExchangeRate.builder()
                .baseCurrency("USD").targetCurrency("EUR")
                .rate(new BigDecimal("0.92000000"))
                .timestamp(LocalDateTime.now())
                .build();

        when(exchangeRateRepository.findOldestRateSince(eq("USD"), eq("EUR"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(oldRate));
        when(exchangeRateRepository.findLatestRate("USD", "EUR"))
                .thenReturn(Optional.of(latestRate));

        TrendResponse result = exchangeRateService.getTrend("USD", "EUR", "12H");

        assertThat(result.getFrom()).isEqualTo("USD");
        assertThat(result.getTo()).isEqualTo("EUR");
        assertThat(result.getPeriod()).isEqualTo("12H");
        assertThat(result.getRateAtStart()).isEqualByComparingTo(new BigDecimal("0.90000000"));
        assertThat(result.getCurrentRate()).isEqualByComparingTo(new BigDecimal("0.92000000"));
        assertThat(result.getPercentageChange()).isPositive();
    }

    @Test
    @DisplayName("Should throw exception for invalid period format")
    void getTrend_invalidPeriod_throws() {
        assertThatThrownBy(() -> exchangeRateService.getTrend("USD", "EUR", "INVALID"))
                .isInstanceOf(InvalidPeriodException.class);
    }

    @Test
    @DisplayName("Should throw exception for period less than 12H")
    void getTrend_periodLessThan12H_throws() {
        assertThatThrownBy(() -> exchangeRateService.getTrend("USD", "EUR", "6H"))
                .isInstanceOf(InvalidPeriodException.class)
                .hasMessageContaining("minimum period is 12H");
    }

    @Test
    @DisplayName("Should throw exception when no rates found for trend")
    void getTrend_noRates_throws() {
        when(exchangeRateRepository.findOldestRateSince(eq("USD"), eq("EUR"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> exchangeRateService.getTrend("USD", "EUR", "12H"))
                .isInstanceOf(ExchangeRateNotFoundException.class);
    }

    @Test
    @DisplayName("Should handle day period")
    void getTrend_dayPeriod_success() {
        ExchangeRate oldRate = ExchangeRate.builder()
                .baseCurrency("USD").targetCurrency("EUR")
                .rate(new BigDecimal("0.90000000"))
                .timestamp(LocalDateTime.now().minusDays(5))
                .build();
        ExchangeRate latestRate = ExchangeRate.builder()
                .baseCurrency("USD").targetCurrency("EUR")
                .rate(new BigDecimal("0.93000000"))
                .timestamp(LocalDateTime.now())
                .build();

        when(exchangeRateRepository.findOldestRateSince(eq("USD"), eq("EUR"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(oldRate));
        when(exchangeRateRepository.findLatestRate("USD", "EUR"))
                .thenReturn(Optional.of(latestRate));

        TrendResponse result = exchangeRateService.getTrend("USD", "EUR", "10D");

        assertThat(result.getPeriod()).isEqualTo("10D");
        assertThat(result.getPercentageChange()).isPositive();
    }

    @Test
    @DisplayName("Should handle month period")
    void getTrend_monthPeriod_success() {
        ExchangeRate oldRate = ExchangeRate.builder()
                .baseCurrency("USD").targetCurrency("EUR")
                .rate(new BigDecimal("0.88000000"))
                .timestamp(LocalDateTime.now().minusMonths(2))
                .build();
        ExchangeRate latestRate = ExchangeRate.builder()
                .baseCurrency("USD").targetCurrency("EUR")
                .rate(new BigDecimal("0.92000000"))
                .timestamp(LocalDateTime.now())
                .build();

        when(exchangeRateRepository.findOldestRateSince(eq("USD"), eq("EUR"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(oldRate));
        when(exchangeRateRepository.findLatestRate("USD", "EUR"))
                .thenReturn(Optional.of(latestRate));

        TrendResponse result = exchangeRateService.getTrend("USD", "EUR", "3M");

        assertThat(result.getPeriod()).isEqualTo("3M");
    }

    @Test
    @DisplayName("Should handle year period")
    void getTrend_yearPeriod_success() {
        ExchangeRate oldRate = ExchangeRate.builder()
                .baseCurrency("USD").targetCurrency("EUR")
                .rate(new BigDecimal("0.85000000"))
                .timestamp(LocalDateTime.now().minusYears(1))
                .build();
        ExchangeRate latestRate = ExchangeRate.builder()
                .baseCurrency("USD").targetCurrency("EUR")
                .rate(new BigDecimal("0.92000000"))
                .timestamp(LocalDateTime.now())
                .build();

        when(exchangeRateRepository.findOldestRateSince(eq("USD"), eq("EUR"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(oldRate));
        when(exchangeRateRepository.findLatestRate("USD", "EUR"))
                .thenReturn(Optional.of(latestRate));

        TrendResponse result = exchangeRateService.getTrend("USD", "EUR", "1Y");

        assertThat(result.getPeriod()).isEqualTo("1Y");
    }
}
