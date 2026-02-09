package com.gravity.exchange.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Exchange rate trend information")
public class TrendResponse {

    @Schema(description = "Source currency code", example = "USD")
    private String from;

    @Schema(description = "Target currency code", example = "EUR")
    private String to;

    @Schema(description = "Period used for trend calculation", example = "12H")
    private String period;

    @Schema(description = "Exchange rate at the start of the period", example = "0.9100")
    private BigDecimal rateAtStart;

    @Schema(description = "Current exchange rate", example = "0.9235")
    private BigDecimal currentRate;

    @Schema(description = "Percentage change over the period", example = "1.48")
    private BigDecimal percentageChange;
}
