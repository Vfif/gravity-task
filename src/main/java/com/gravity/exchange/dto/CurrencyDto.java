package com.gravity.exchange.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Currency information")
public class CurrencyDto {

    @Schema(description = "Currency code (ISO 4217)", example = "USD")
    private String code;

    @Schema(description = "Currency name", example = "US Dollar")
    private String name;
}
