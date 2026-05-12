package com.arquixpress.marketplace.orders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    @Query("select o from OrderEntity o left join fetch o.lines where o.id = :id")
    Optional<OrderEntity> findWithLines(@Param("id") UUID id);

    @Query("select distinct o from OrderEntity o left join fetch o.lines where o.buyerId = :buyerId order by o.createdAt desc")
    List<OrderEntity> findByBuyerWithLines(@Param("buyerId") UUID buyerId);

    @Query("select distinct o from OrderEntity o left join fetch o.lines where o.logisticsCenterId = :centerId and o.status = com.arquixpress.marketplace.orders.OrderStatus.PAID order by o.updatedAt desc")
    List<OrderEntity> findPaidByCenter(@Param("centerId") UUID centerId);

    @Query("select distinct o from OrderEntity o left join fetch o.lines where o.status = com.arquixpress.marketplace.orders.OrderStatus.PAID order by o.updatedAt desc")
    List<OrderEntity> findAllPaid();
}
