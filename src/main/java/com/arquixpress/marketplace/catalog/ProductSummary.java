package com.arquixpress.marketplace.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductSummary(
        UUID id,
        UUID sellerId,
        String title,
        String description,
        String category,
        String imageUrl,
        List<String> imageUrls,
        BigDecimal price,
        int stockAvailable) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ProductSummary from(Product product) {
        return new ProductSummary(product.id(), product.sellerId(), product.title(), product.description(),
                product.category(), product.imageUrl(), parseImages(product.imageUrls(), product.imageUrl()),
                product.price(), product.stockAvailable());
    }

    public static List<String> parseImages(String imageUrls, String fallbackImageUrl) {
        if (imageUrls == null || imageUrls.isBlank()) {
            return List.of(fallbackImageUrl);
        }
        try {
            List<String> parsed = MAPPER.readValue(imageUrls, new TypeReference<>() {});
            List<String> clean = parsed.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .toList();
            return clean.isEmpty() ? List.of(fallbackImageUrl) : clean;
        } catch (JsonProcessingException ex) {
            return List.of(fallbackImageUrl);
        }
    }
}
