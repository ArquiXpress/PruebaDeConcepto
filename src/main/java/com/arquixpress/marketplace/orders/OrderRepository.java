package com.arquixpress.marketplace.orders;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    @Query("select o from OrderEntity o left join fetch o.lines where o.id = :id")
    Optional<OrderEntity> findWithLines(@Param("id") UUID id);
}
