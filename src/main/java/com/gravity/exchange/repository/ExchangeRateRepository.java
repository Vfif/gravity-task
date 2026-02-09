package com.gravity.exchange.repository;

import com.gravity.exchange.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    @Query("SELECT e FROM ExchangeRate e WHERE e.baseCurrency = :base AND e.targetCurrency = :target "
            + "ORDER BY e.timestamp DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRate(@Param("base") String baseCurrency,
                                         @Param("target") String targetCurrency);

    @Query("SELECT e FROM ExchangeRate e WHERE e.baseCurrency = :base AND e.targetCurrency = :target "
            + "AND e.timestamp >= :since ORDER BY e.timestamp ASC")
    List<ExchangeRate> findRatesSince(@Param("base") String baseCurrency,
                                     @Param("target") String targetCurrency,
                                     @Param("since") LocalDateTime since);

    @Query("SELECT e FROM ExchangeRate e WHERE e.baseCurrency = :base AND e.targetCurrency = :target "
            + "AND e.timestamp >= :since ORDER BY e.timestamp ASC LIMIT 1")
    Optional<ExchangeRate> findOldestRateSince(@Param("base") String baseCurrency,
                                              @Param("target") String targetCurrency,
                                              @Param("since") LocalDateTime since);
}
