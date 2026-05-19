package com.arquixpress.marketplace.addresses;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, UUID> {
    Optional<DeliveryAddress> findByIdAndUserIdAndActiveTrue(UUID id, UUID userId);
}
