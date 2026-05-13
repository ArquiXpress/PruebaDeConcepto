package com.arquixpress.marketplace.catalog.questions;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, UUID> {
    List<ProductQuestion> findByProductIdOrderByCreatedAtDesc(UUID productId);

    List<ProductQuestion> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);
}
