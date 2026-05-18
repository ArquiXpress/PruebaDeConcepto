package com.arquixpress.marketplace.promotions;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SponsoredProductViewService {

    public record ProductCard(UUID productId, String title, String label, boolean sponsored) {
    }

    public List<ProductCard> marcarPatrocinados(List<ProductCard> products, Set<UUID> sponsoredProductIds) {
        return products.stream()
                .map(product -> {
                    if (sponsoredProductIds.contains(product.productId())) {
                        return new ProductCard(product.productId(), product.title(), "Patrocinado", true);
                    }
                    return new ProductCard(product.productId(), product.title(), product.label(), false);
                })
                .toList();
    }
}
