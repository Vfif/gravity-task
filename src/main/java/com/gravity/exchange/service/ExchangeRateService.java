package com.gravity.exchange.service;

import com.gravity.exchange.cache.ExchangeRateCache;
import com.gravity.exchange.dto.ConversionResponse;
import com.gravity.exchange.dto.TrendResponse;
import com.gravity.exchange.entity.ExchangeRate;
import com.gravity.exchange.exception.ExchangeRateNotFoundException;
import com.gravity.exchange.exception.InvalidPeriodException;
import com.gravity.exchange.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("^(\\d+)([HDMY])$", Pattern.CASE_INSENSITIVE);
    private static final int SCALE = 8;

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateCache exchangeRateCache;

    /**
     * Converts an amount from one currency to another using the best cached rate.
     *
     * @param amount the amount to convert
     * @param from   the source currency code
     * @param to     the target currency code
     * @return the conversion result
     */
    public ConversionResponse convert(BigDecimal amount, String from, String to) {
        String fromUpper = from.toUpperCase();
        String toUpper = to.toUpperCase();

        BigDecimal rate = exchangeRateCache.getBestRate(fromUpper, toUpper)
                .orElseThrow(() -> new ExchangeRateNotFoundException(fromUpper, toUpper));

        BigDecimal convertedAmount = amount.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);

        return ConversionResponse.builder()
                .from(fromUpper)
                .to(toUpper)
                .amount(amount)
                .convertedAmount(convertedAmount)
                .rate(rate)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Calculates the exchange rate trend over a specified period.
     *
     * @param from   source currency
     * @param to     target currency
     * @param period the period string (e.g., "12H", "10D", "3M", "1Y")
     * @return the trend information
     */
    public TrendResponse getTrend(String from, String to, String period) {
        String fromUpper = from.toUpperCase();
        String toUpper = to.toUpperCase();

        LocalDateTime since = parsePeriod(period);

        ExchangeRate oldRate = exchangeRateRepository
                .findOldestRateSince(fromUpper, toUpper, since)
                .orElseThrow(() -> new ExchangeRateNotFoundException(fromUpper, toUpper));

        ExchangeRate latestRate = exchangeRateRepository
                .findLatestRate(fromUpper, toUpper)
                .orElseThrow(() -> new ExchangeRateNotFoundException(fromUpper, toUpper));

        BigDecimal percentageChange = latestRate.getRate()
                .subtract(oldRate.getRate())
                .divide(oldRate.getRate(), SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        return TrendResponse.builder()
                .from(fromUpper)
                .to(toUpper)
                .period(period.toUpperCase())
                .rateAtStart(oldRate.getRate())
                .currentRate(latestRate.getRate())
                .percentageChange(percentageChange)
                .build();
    }

    /**
     * Parses a period string like "12H", "10D", "3M", "1Y" into a LocalDateTime
     * representing the start of that period from now.
     *
     * @param period the period string
     * @return the start datetime
     */
    private LocalDateTime parsePeriod(String period) {
        Matcher matcher = PERIOD_PATTERN.matcher(period.trim());
        if (!matcher.matches()) {
            throw new InvalidPeriodException(period);
        }

        int value = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2).toUpperCase();

        LocalDateTime now = LocalDateTime.now();
        return switch (unit) {
            case "H" -> {
                if (value < 12) {
                    throw new InvalidPeriodException(period + " (minimum period is 12H)");
                }
                yield now.minusHours(value);
            }
            case "D" -> now.minusDays(value);
            case "M" -> now.minusMonths(value);
            case "Y" -> now.minusYears(value);
            default -> throw new InvalidPeriodException(period);
        };
    }
}
