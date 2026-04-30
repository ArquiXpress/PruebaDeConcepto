package com.arquixpress.marketplace.logistics;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogisticsOperatorRepository extends JpaRepository<LogisticsOperator, UUID> {
    Optional<LogisticsOperator> findByAppUserId(UUID appUserId);
}
