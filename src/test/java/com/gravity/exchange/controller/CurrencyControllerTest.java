package com.gravity.exchange.controller;

import com.gravity.exchange.config.SecurityConfig;
import com.gravity.exchange.dto.ConversionResponse;
import com.gravity.exchange.dto.CurrencyDto;
import com.gravity.exchange.dto.TrendResponse;
import com.gravity.exchange.exception.CurrencyAlreadyExistsException;
import com.gravity.exchange.exception.CurrencyNotFoundException;
import com.gravity.exchange.exception.ExchangeRateNotFoundException;
import com.gravity.exchange.exception.GlobalExceptionHandler;
import com.gravity.exchange.exception.InvalidPeriodException;
import com.gravity.exchange.service.CurrencyService;
import com.gravity.exchange.service.ExchangeRateFetchScheduler;
import com.gravity.exchange.service.ExchangeRateService;
import com.gravity.exchange.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurrencyController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyService currencyService;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @MockBean
    private ExchangeRateFetchScheduler fetchScheduler;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    // ===== GET /api/v1/currencies =====

    @Nested
    @DisplayName("GET /api/v1/currencies")
    class GetCurrencies {

        @Test
        @DisplayName("Should return currencies list for anonymous user")
        @WithAnonymousUser
        void getAllCurrencies_anonymous_returns200() throws Exception {
            List<CurrencyDto> currencies = List.of(
                    CurrencyDto.builder().code("USD").name("US Dollar").build(),
                    CurrencyDto.builder().code("EUR").name("Euro").build()
            );
            when(currencyService.getAllCurrencies()).thenReturn(currencies);

            mockMvc.perform(get("/api/v1/currencies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].code", is("USD")))
                    .andExpect(jsonPath("$[1].code", is("EUR")));
        }

        @Test
        @DisplayName("Should return empty list when no currencies")
        @WithAnonymousUser
        void getAllCurrencies_emptyList_returns200() throws Exception {
            when(currencyService.getAllCurrencies()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/currencies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ===== POST /api/v1/currencies =====

    @Nested
    @DisplayName("POST /api/v1/currencies")
    class AddCurrency {

        @Test
        @DisplayName("Should add currency as ADMIN")
        @WithMockUser(roles = "ADMIN")
        void addCurrency_admin_returns201() throws Exception {
            CurrencyDto created = CurrencyDto.builder().code("USD").name("US Dollar").build();
            when(currencyService.addCurrency("USD")).thenReturn(created);

            mockMvc.perform(post("/api/v1/currencies").param("currency", "USD"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code", is("USD")))
                    .andExpect(jsonPath("$.name", is("US Dollar")));
        }

        @Test
        @DisplayName("Should return 403 for regular USER")
        @WithMockUser(roles = "USER")
        void addCurrency_user_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/currencies").param("currency", "USD"))
                    .andExpect(status().isForbidden());

            verify(currencyService, never()).addCurrency(anyString());
        }

        @Test
        @DisplayName("Should return 401 for anonymous user")
        @WithAnonymousUser
        void addCurrency_anonymous_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/currencies").param("currency", "USD"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 409 when currency already exists")
        @WithMockUser(roles = "ADMIN")
        void addCurrency_alreadyExists_returns409() throws Exception {
            when(currencyService.addCurrency("USD"))
                    .thenThrow(new CurrencyAlreadyExistsException("USD"));

            mockMvc.perform(post("/api/v1/currencies").param("currency", "USD"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Currency already exists: USD"));
        }

        @Test
        @DisplayName("Should return 400 when currency param is blank")
        @WithMockUser(roles = "ADMIN")
        void addCurrency_blankParam_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/currencies").param("currency", ""))
                    .andExpect(status().isBadRequest());
        }
    }

    // ===== GET /api/v1/currencies/exchange-rates =====

    @Nested
    @DisplayName("GET /api/v1/currencies/exchange-rates")
    class GetExchangeRates {

        @Test
        @DisplayName("Should return conversion result for anonymous user")
        @WithAnonymousUser
        void getExchangeRate_anonymous_returns200() throws Exception {
            ConversionResponse response = ConversionResponse.builder()
                    .from("USD").to("EUR")
                    .amount(new BigDecimal("15"))
                    .convertedAmount(new BigDecimal("13.80"))
                    .rate(new BigDecimal("0.92"))
                    .timestamp(LocalDateTime.now())
                    .build();
            when(exchangeRateService.convert(any(BigDecimal.class), anyString(), anyString()))
                    .thenReturn(response);

            mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                            .param("amount", "15")
                            .param("from", "USD")
                            .param("to", "EUR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.from", is("USD")))
                    .andExpect(jsonPath("$.to", is("EUR")));
        }

        @Test
        @DisplayName("Should return 404 when exchange rate not found")
        @WithAnonymousUser
        void getExchangeRate_notFound_returns404() throws Exception {
            when(exchangeRateService.convert(any(BigDecimal.class), anyString(), anyString()))
                    .thenThrow(new ExchangeRateNotFoundException("USD", "XYZ"));

            mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                            .param("amount", "15")
                            .param("from", "USD")
                            .param("to", "XYZ"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when amount is missing")
        @WithAnonymousUser
        void getExchangeRate_missingAmount_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                            .param("from", "USD")
                            .param("to", "EUR"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when from is missing")
        @WithAnonymousUser
        void getExchangeRate_missingFrom_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                            .param("amount", "15")
                            .param("to", "EUR"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ===== POST /api/v1/currencies/refresh =====

    @Nested
    @DisplayName("POST /api/v1/currencies/refresh")
    class RefreshRates {

        @Test
        @DisplayName("Should refresh rates as ADMIN")
        @WithMockUser(roles = "ADMIN")
        void refreshRates_admin_returns200() throws Exception {
            mockMvc.perform(post("/api/v1/currencies/refresh"))
                    .andExpect(status().isOk());

            verify(fetchScheduler).refreshRates();
        }

        @Test
        @DisplayName("Should return 403 for regular USER")
        @WithMockUser(roles = "USER")
        void refreshRates_user_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/currencies/refresh"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for PREMIUM_USER")
        @WithMockUser(roles = "PREMIUM_USER")
        void refreshRates_premiumUser_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/currencies/refresh"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 for anonymous user")
        @WithAnonymousUser
        void refreshRates_anonymous_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/currencies/refresh"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== GET /api/v1/currencies/trends =====

    @Nested
    @DisplayName("GET /api/v1/currencies/trends")
    class GetTrends {

        @Test
        @DisplayName("Should return trends for ADMIN")
        @WithMockUser(roles = "ADMIN")
        void getTrends_admin_returns200() throws Exception {
            TrendResponse response = TrendResponse.builder()
                    .from("USD").to("EUR").period("12H")
                    .rateAtStart(new BigDecimal("0.91"))
                    .currentRate(new BigDecimal("0.92"))
                    .percentageChange(new BigDecimal("1.10"))
                    .build();
            when(exchangeRateService.getTrend("USD", "EUR", "12H")).thenReturn(response);

            mockMvc.perform(get("/api/v1/currencies/trends")
                            .param("from", "USD")
                            .param("to", "EUR")
                            .param("period", "12H"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.percentageChange", is(1.10)));
        }

        @Test
        @DisplayName("Should return trends for PREMIUM_USER")
        @WithMockUser(roles = "PREMIUM_USER")
        void getTrends_premiumUser_returns200() throws Exception {
            TrendResponse response = TrendResponse.builder()
                    .from("USD").to("EUR").period("12H")
                    .rateAtStart(new BigDecimal("0.91"))
                    .currentRate(new BigDecimal("0.92"))
                    .percentageChange(new BigDecimal("1.10"))
                    .build();
            when(exchangeRateService.getTrend("USD", "EUR", "12H")).thenReturn(response);

            mockMvc.perform(get("/api/v1/currencies/trends")
                            .param("from", "USD")
                            .param("to", "EUR")
                            .param("period", "12H"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 for regular USER")
        @WithMockUser(roles = "USER")
        void getTrends_user_returns403() throws Exception {
            mockMvc.perform(get("/api/v1/currencies/trends")
                            .param("from", "USD")
                            .param("to", "EUR")
                            .param("period", "12H"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 for anonymous user")
        @WithAnonymousUser
        void getTrends_anonymous_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/currencies/trends")
                            .param("from", "USD")
                            .param("to", "EUR")
                            .param("period", "12H"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 400 for invalid period")
        @WithMockUser(roles = "ADMIN")
        void getTrends_invalidPeriod_returns400() throws Exception {
            when(exchangeRateService.getTrend("USD", "EUR", "INVALID"))
                    .thenThrow(new InvalidPeriodException("INVALID"));

            mockMvc.perform(get("/api/v1/currencies/trends")
                            .param("from", "USD")
                            .param("to", "EUR")
                            .param("period", "INVALID"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when period param is missing")
        @WithMockUser(roles = "ADMIN")
        void getTrends_missingPeriod_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/currencies/trends")
                            .param("from", "USD")
                            .param("to", "EUR"))
                    .andExpect(status().isBadRequest());
        }
    }
}
