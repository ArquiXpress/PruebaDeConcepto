package com.arquixpress.marketplace.logistics;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogisticsCenterRepository extends JpaRepository<LogisticsCenter, UUID> {
    Optional<LogisticsCenter> findByCity(String city);
}
