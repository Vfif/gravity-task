package com.gravity.exchange;

import com.gravity.exchange.cache.ExchangeRateCache;
import com.gravity.exchange.entity.Currency;
import com.gravity.exchange.entity.ExchangeRate;
import com.gravity.exchange.repository.CurrencyRepository;
import com.gravity.exchange.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class CurrencyExchangeIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("exchange_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Disable scheduled tasks in tests
        registry.add("exchange.scheduler.fixed-rate", () -> "999999999");
        // Use dummy URLs for providers (won't be called in these tests)
        registry.add("exchange.providers.frankfurter.url", () -> "http://localhost:19999");
        registry.add("exchange.providers.exchangerate-api.url", () -> "http://localhost:19998");
        registry.add("exchange.providers.mock-provider-1.url", () -> "http://localhost:19997");
        registry.add("exchange.providers.mock-provider-2.url", () -> "http://localhost:19996");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private ExchangeRateCache exchangeRateCache;

    @BeforeEach
    void setUp() {
        exchangeRateRepository.deleteAll();
        currencyRepository.deleteAll();
        exchangeRateCache.clear();
    }

    @Nested
    @DisplayName("Currency CRUD Integration Tests")
    class CurrencyCrudIT {

        @Test
        @DisplayName("Should return empty list initially")
        @WithAnonymousUser
        void getAllCurrencies_emptyInitially() throws Exception {
            mockMvc.perform(get("/api/v1/currencies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should add currency and retrieve it")
        @WithMockUser(roles = "ADMIN")
        void addAndGetCurrency() throws Exception {
            // Add currency
            mockMvc.perform(post("/api/v1/currencies").param("currency", "USD"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code", is("USD")));

            // Verify it's in the list
            mockMvc.perform(get("/api/v1/currencies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].code", is("USD")));
        }

        @Test
        @DisplayName("Should return 409 when adding duplicate currency")
        @WithMockUser(roles = "ADMIN")
        void addDuplicateCurrency_returns409() throws Exception {
            // Add first time
            mockMvc.perform(post("/api/v1/currencies").param("currency", "EUR"))
                    .andExpect(status().isCreated());

            // Add same currency again
            mockMvc.perform(post("/api/v1/currencies").param("currency", "EUR"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Currency already exists: EUR"));
        }

        @Test
        @DisplayName("Should persist currency to database")
        @WithMockUser(roles = "ADMIN")
        void addCurrency_persistsToDb() throws Exception {
            mockMvc.perform(post("/api/v1/currencies").param("currency", "GBP"))
                    .andExpect(status().isCreated());

            assertThat(currencyRepository.findByCode("GBP")).isPresent();
            assertThat(currencyRepository.findByCode("GBP").get().getName()).isEqualTo("British Pound");
        }
    }

    @Nested
    @DisplayName("Exchange Rate Conversion Integration Tests")
    class ConversionIT {

        @Test
        @DisplayName("Should convert amount when rate is cached")
        @WithAnonymousUser
        void convert_withCachedRate_returns200() throws Exception {
            exchangeRateCache.putRate("USD", "EUR", new BigDecimal("0.92000000"));

            mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                            .param("amount", "100")
                            .param("from", "USD")
                            .param("to", "EUR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.from", is("USD")))
                    .andExpect(jsonPath("$.to", is("EUR")))
                    .andExpect(jsonPath("$.rate").value(0.92000000));
        }

        @Test
        @DisplayName("Should return 404 when rate is not cached")
        @WithAnonymousUser
        void convert_withoutCachedRate_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                            .param("amount", "100")
                            .param("from", "USD")
                            .param("to", "XYZ"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Security Integration Tests")
    class SecurityIT {

        @Test
        @DisplayName("Anonymous user can access GET currencies")
        @WithAnonymousUser
        void anonymousCanGetCurrencies() throws Exception {
            mockMvc.perform(get("/api/v1/currencies"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Anonymous user can access GET exchange-rates")
        @WithAnonymousUser
        void anonymousCanGetExchangeRates() throws Exception {
            exchangeRateCache.putRate("USD", "EUR", new BigDecimal("0.92"));

            mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                            .param("amount", "10")
                            .param("from", "USD")
                            .param("to", "EUR"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Anonymous user cannot POST currencies")
        @WithAnonymousUser
        void anonymousCannotAddCurrency() throws Exception {
            mockMvc.perform(post("/api/v1/currencies").param("currency", "USD"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("USER cannot POST currencies")
        @WithMockUser(roles = "USER")
        void userCannotAddCurrency() throws Exception {
            mockMvc.perform(post("/api/v1/currencies").param("currency", "USD"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("USER cannot access trends")
        @WithMockUser(roles = "USER")
        void userCannotAccessTrends() throws Exception {
            mockMvc.perform(get("/api/v1/currencies/trends")
                            .param("from", "USD")
                            .param("to", "EUR")
                            .param("period", "12H"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PREMIUM_USER can access trends")
        @WithMockUser(roles = "PREMIUM_USER")
        void premiumUserCanAccessTrends() throws Exception {
            // Will get 404 because no data, but the security check passes
            exchangeRateRepository.save(ExchangeRate.builder()
                    .baseCurrency("USD").targetCurrency("EUR")
                    .rate(new BigDecimal("0.91")).source("TEST")
                    .timestamp(LocalDateTime.now().minusHours(13))
                    .build());
            exchangeRateRepository.save(ExchangeRate.builder()
                    .baseCurrency("USD").targetCurrency("EUR")
                    .rate(new BigDecimal("0.92")).source("TEST")
                    .timestamp(LocalDateTime.now())
                    .build());

            mockMvc.perform(get("/api/v1/currencies/trends")
                            .param("from", "USD")
                            .param("to", "EUR")
                            .param("period", "12H"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN can access trends")
        @WithMockUser(roles = "ADMIN")
        void adminCanAccessTrends() throws Exception {
            exchangeRateRepository.save(ExchangeRate.builder()
                    .baseCurrency("USD").targetCurrency("EUR")
                    .rate(new BigDecimal("0.91")).source("TEST")
                    .timestamp(LocalDateTime.now().minusHours(13))
                    .build());
            exchangeRateRepository.save(ExchangeRate.builder()
                    .baseCurrency("USD").targetCurrency("EUR")
                    .rate(new BigDecimal("0.92")).source("TEST")
                    .timestamp(LocalDateTime.now())
                    .build());

            mockMvc.perform(get("/api/v1/currencies/trends")
                            .param("from", "USD")
                            .param("to", "EUR")
                            .param("period", "12H"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN can refresh rates")
        @WithMockUser(roles = "ADMIN")
        void adminCanRefreshRates() throws Exception {
            mockMvc.perform(post("/api/v1/currencies/refresh"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USER cannot refresh rates")
        @WithMockUser(roles = "USER")
        void userCannotRefreshRates() throws Exception {
            mockMvc.perform(post("/api/v1/currencies/refresh"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Exception Handling Integration Tests")
    class ExceptionHandlingIT {

        @Test
        @DisplayName("Should return error JSON for currency not found")
        @WithAnonymousUser
        void currencyNotFound_returnsErrorJson() throws Exception {
            mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                            .param("amount", "100")
                            .param("from", "USD")
                            .param("to", "NONEXISTENT"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.path").exists());
        }

        @Test
        @DisplayName("Should return error JSON for invalid period")
        @WithMockUser(roles = "ADMIN")
        void invalidPeriod_returnsErrorJson() throws Exception {
            mockMvc.perform(get("/api/v1/currencies/trends")
                            .param("from", "USD")
                            .param("to", "EUR")
                            .param("period", "INVALID"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Bad Request")));
        }

        @Test
        @DisplayName("Should return error JSON for missing required param")
        @WithAnonymousUser
        void missingParam_returnsErrorJson() throws Exception {
            mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                            .param("from", "USD")
                            .param("to", "EUR"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));
        }
    }
}
