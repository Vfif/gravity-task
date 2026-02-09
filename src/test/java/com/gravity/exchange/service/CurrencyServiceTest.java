package com.gravity.exchange.service;

import com.gravity.exchange.dto.CurrencyDto;
import com.gravity.exchange.entity.Currency;
import com.gravity.exchange.exception.CurrencyAlreadyExistsException;
import com.gravity.exchange.exception.CurrencyNotFoundException;
import com.gravity.exchange.repository.CurrencyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private CurrencyService currencyService;

    @Test
    @DisplayName("Should return all active currencies")
    void getAllCurrencies_returnsActiveCurrencies() {
        List<Currency> currencies = List.of(
                Currency.builder().code("USD").name("US Dollar").active(true).build(),
                Currency.builder().code("EUR").name("Euro").active(true).build()
        );
        when(currencyRepository.findAllByActiveTrue()).thenReturn(currencies);

        List<CurrencyDto> result = currencyService.getAllCurrencies();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CurrencyDto::getCode).containsExactly("USD", "EUR");
    }

    @Test
    @DisplayName("Should return empty list when no currencies exist")
    void getAllCurrencies_returnsEmptyList() {
        when(currencyRepository.findAllByActiveTrue()).thenReturn(List.of());

        List<CurrencyDto> result = currencyService.getAllCurrencies();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should add a new currency successfully")
    void addCurrency_success() {
        when(currencyRepository.existsByCode("USD")).thenReturn(false);
        when(currencyRepository.save(any(Currency.class))).thenAnswer(invocation -> {
            Currency c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        CurrencyDto result = currencyService.addCurrency("USD");

        assertThat(result.getCode()).isEqualTo("USD");
        assertThat(result.getName()).isEqualTo("US Dollar");
        verify(currencyRepository).save(any(Currency.class));
    }

    @Test
    @DisplayName("Should throw exception when currency already exists")
    void addCurrency_throwsWhenAlreadyExists() {
        when(currencyRepository.existsByCode("USD")).thenReturn(true);

        assertThatThrownBy(() -> currencyService.addCurrency("USD"))
                .isInstanceOf(CurrencyAlreadyExistsException.class)
                .hasMessageContaining("USD");

        verify(currencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle lowercase currency code input")
    void addCurrency_handlesLowercase() {
        when(currencyRepository.existsByCode("EUR")).thenReturn(false);
        when(currencyRepository.save(any(Currency.class))).thenAnswer(invocation -> {
            Currency c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        CurrencyDto result = currencyService.addCurrency("eur");

        assertThat(result.getCode()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("Should get currency by code")
    void getCurrencyByCode_returnsCurrency() {
        Currency currency = Currency.builder().code("USD").name("US Dollar").build();
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(currency));

        Currency result = currencyService.getCurrencyByCode("USD");

        assertThat(result.getCode()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should throw exception when currency not found by code")
    void getCurrencyByCode_throwsWhenNotFound() {
        when(currencyRepository.findByCode("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currencyService.getCurrencyByCode("XYZ"))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessageContaining("XYZ");
    }

    @Test
    @DisplayName("Should return list of active currency codes")
    void getActiveCurrencyCodes_returnsCodes() {
        List<Currency> currencies = List.of(
                Currency.builder().code("USD").name("US Dollar").active(true).build(),
                Currency.builder().code("EUR").name("Euro").active(true).build()
        );
        when(currencyRepository.findAllByActiveTrue()).thenReturn(currencies);

        List<String> result = currencyService.getActiveCurrencyCodes();

        assertThat(result).containsExactly("USD", "EUR");
    }
}
