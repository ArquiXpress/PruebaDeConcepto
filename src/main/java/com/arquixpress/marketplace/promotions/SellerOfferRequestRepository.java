package com.arquixpress.marketplace.promotions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SellerOfferRequestRepository extends JpaRepository<SellerOfferRequest, UUID> {
    List<SellerOfferRequest> findTop50ByOrderByCreatedAtDesc();
    List<SellerOfferRequest> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);

    @Query("""
            select distinct o from SellerOfferRequest o join fetch o.productIds
            where o.status = com.arquixpress.marketplace.promotions.OfferRequestStatus.ACCEPTED
              and o.startsAt <= :now
              and o.endsAt >= :now
            order by o.createdAt desc
            """)
    List<SellerOfferRequest> findActiveAccepted(@Param("now") Instant now);
}
