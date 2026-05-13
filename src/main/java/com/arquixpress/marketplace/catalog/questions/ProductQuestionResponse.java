package com.arquixpress.marketplace.catalog.questions;

import java.time.Instant;
import java.util.UUID;

public record ProductQuestionResponse(
        UUID id,
        UUID productId,
        UUID buyerId,
        UUID sellerId,
        String question,
        String answer,
        UUID answeredBy,
        Instant answeredAt,
        Instant createdAt
) {
    public static ProductQuestionResponse from(ProductQuestion question) {
        return new ProductQuestionResponse(
                question.id(),
                question.productId(),
                question.buyerId(),
                question.sellerId(),
                question.question(),
                question.answer(),
                question.answeredBy(),
                question.answeredAt(),
                question.createdAt());
    }
}
