package com.gravity.exchange.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Exchange rate conversion result")
public class ConversionResponse {

    @Schema(description = "Source currency code", example = "USD")
    private String from;

    @Schema(description = "Target currency code", example = "EUR")
    private String to;

    @Schema(description = "Original amount", example = "100.00")
    private BigDecimal amount;

    @Schema(description = "Converted amount", example = "92.35")
    private BigDecimal convertedAmount;

    @Schema(description = "Exchange rate used", example = "0.9235")
    private BigDecimal rate;

    @Schema(description = "Timestamp of the rate used")
    private LocalDateTime timestamp;
}
