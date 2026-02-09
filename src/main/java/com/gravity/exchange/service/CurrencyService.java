package com.gravity.exchange.service;

import com.gravity.exchange.dto.CurrencyDto;
import com.gravity.exchange.entity.Currency;
import com.gravity.exchange.exception.CurrencyAlreadyExistsException;
import com.gravity.exchange.exception.CurrencyNotFoundException;
import com.gravity.exchange.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {

    /**
     * Mapping of common ISO 4217 currency codes to their names.
     */
    private static final Map<String, String> CURRENCY_NAMES = Map.ofEntries(
            Map.entry("USD", "US Dollar"),
            Map.entry("EUR", "Euro"),
            Map.entry("GBP", "British Pound"),
            Map.entry("JPY", "Japanese Yen"),
            Map.entry("CHF", "Swiss Franc"),
            Map.entry("CAD", "Canadian Dollar"),
            Map.entry("AUD", "Australian Dollar"),
            Map.entry("NZD", "New Zealand Dollar"),
            Map.entry("CNY", "Chinese Yuan"),
            Map.entry("SEK", "Swedish Krona"),
            Map.entry("NOK", "Norwegian Krone"),
            Map.entry("DKK", "Danish Krone"),
            Map.entry("PLN", "Polish Zloty"),
            Map.entry("CZK", "Czech Koruna"),
            Map.entry("HUF", "Hungarian Forint"),
            Map.entry("TRY", "Turkish Lira"),
            Map.entry("BRL", "Brazilian Real"),
            Map.entry("MXN", "Mexican Peso"),
            Map.entry("INR", "Indian Rupee"),
            Map.entry("KRW", "South Korean Won"),
            Map.entry("SGD", "Singapore Dollar"),
            Map.entry("HKD", "Hong Kong Dollar"),
            Map.entry("ZAR", "South African Rand"),
            Map.entry("RUB", "Russian Ruble"),
            Map.entry("THB", "Thai Baht"),
            Map.entry("IDR", "Indonesian Rupiah"),
            Map.entry("MYR", "Malaysian Ringgit"),
            Map.entry("PHP", "Philippine Peso"),
            Map.entry("ILS", "Israeli Shekel"),
            Map.entry("BGN", "Bulgarian Lev"),
            Map.entry("RON", "Romanian Leu"),
            Map.entry("ISK", "Icelandic Krona"),
            Map.entry("HRK", "Croatian Kuna")
    );

    private final CurrencyRepository currencyRepository;

    /**
     * Returns all active currencies.
     *
     * @return list of currency DTOs
     */
    public List<CurrencyDto> getAllCurrencies() {
        return currencyRepository.findAllByActiveTrue().stream()
                .map(c -> CurrencyDto.builder()
                        .code(c.getCode())
                        .name(c.getName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Adds a new currency to the system.
     *
     * @param code the ISO 4217 currency code
     * @return the created currency DTO
     */
    @Transactional
    public CurrencyDto addCurrency(String code) {
        String upperCode = code.toUpperCase().trim();

        if (currencyRepository.existsByCode(upperCode)) {
            throw new CurrencyAlreadyExistsException(upperCode);
        }

        String name = CURRENCY_NAMES.getOrDefault(upperCode, upperCode);

        Currency currency = Currency.builder()
                .code(upperCode)
                .name(name)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Currency saved = currencyRepository.save(currency);
        log.info("Added new currency: {}", saved.getCode());

        return CurrencyDto.builder()
                .code(saved.getCode())
                .name(saved.getName())
                .build();
    }

    /**
     * Retrieves currency by code or throws exception.
     *
     * @param code the currency code
     * @return the currency entity
     */
    public Currency getCurrencyByCode(String code) {
        return currencyRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new CurrencyNotFoundException(code));
    }

    /**
     * Returns all active currency codes.
     *
     * @return set of active currency codes
     */
    public List<String> getActiveCurrencyCodes() {
        return currencyRepository.findAllByActiveTrue().stream()
                .map(Currency::getCode)
                .collect(Collectors.toList());
    }
}
