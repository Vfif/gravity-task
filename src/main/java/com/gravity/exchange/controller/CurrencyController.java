package com.gravity.exchange.controller;

import com.gravity.exchange.dto.ConversionResponse;
import com.gravity.exchange.dto.CurrencyDto;
import com.gravity.exchange.dto.ErrorResponse;
import com.gravity.exchange.dto.TrendResponse;
import com.gravity.exchange.service.CurrencyService;
import com.gravity.exchange.service.ExchangeRateFetchScheduler;
import com.gravity.exchange.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
@Validated
@Tag(name = "Currency Exchange", description = "Currency exchange rate operations")
public class CurrencyController {

    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;
    private final ExchangeRateFetchScheduler fetchScheduler;

    @Operation(summary = "Get all currencies",
            description = "Returns a list of all active currencies used in the project. Available to everyone.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of currencies"),
    })
    @GetMapping
    public ResponseEntity<List<CurrencyDto>> getAllCurrencies() {
        return ResponseEntity.ok(currencyService.getAllCurrencies());
    }

    @Operation(summary = "Add a new currency",
            description = "Adds a new currency for getting exchange rates. Available to ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Currency added successfully"),
            @ApiResponse(responseCode = "409", description = "Currency already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid currency code",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping
    public ResponseEntity<CurrencyDto> addCurrency(
            @Parameter(description = "Currency code (ISO 4217)", example = "USD")
            @RequestParam @NotBlank(message = "Currency code must not be blank") String currency) {
        CurrencyDto created = currencyService.addCurrency(currency);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get exchange rates",
            description = "Converts an amount from one currency to another. Available to everyone.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversion result"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/exchange-rates")
    public ResponseEntity<ConversionResponse> getExchangeRate(
            @Parameter(description = "Amount to convert", example = "15")
            @RequestParam @NotNull(message = "Amount must not be null")
            @Positive(message = "Amount must be positive") BigDecimal amount,
            @Parameter(description = "Source currency code", example = "USD")
            @RequestParam @NotBlank(message = "Source currency must not be blank") String from,
            @Parameter(description = "Target currency code", example = "EUR")
            @RequestParam @NotBlank(message = "Target currency must not be blank") String to) {
        return ResponseEntity.ok(exchangeRateService.convert(amount, from, to));
    }

    @Operation(summary = "Refresh exchange rates",
            description = "Triggers a manual refresh of exchange rates from all providers. Available to ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rates refreshed successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshRates() {
        fetchScheduler.refreshRates();
        return ResponseEntity.ok("Exchange rates refreshed successfully");
    }

    @Operation(summary = "Get exchange rate trends",
            description = "Returns how much the exchange rate has changed in percentage over a specified period. "
                    + "Available only to ADMIN and PREMIUM_USER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trend information"),
            @ApiResponse(responseCode = "404", description = "Exchange rate data not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid period format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/trends")
    public ResponseEntity<TrendResponse> getTrends(
            @Parameter(description = "Source currency code", example = "USD")
            @RequestParam @NotBlank(message = "Source currency must not be blank") String from,
            @Parameter(description = "Target currency code", example = "EUR")
            @RequestParam @NotBlank(message = "Target currency must not be blank") String to,
            @Parameter(description = "Time period (e.g., 12H, 10D, 3M, 1Y)", example = "12H")
            @RequestParam @NotBlank(message = "Period must not be blank") String period) {
        return ResponseEntity.ok(exchangeRateService.getTrend(from, to, period));
    }
}
