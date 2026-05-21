package com.arquixpress.marketplace.favorites;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.catalog.ProductSummary;
import com.arquixpress.marketplace.catalog.ProductStatus;

public class FavoriteService {

    private final FavoriteRepository favorites;
    private final ProductRepository products;

    public FavoriteService(FavoriteRepository favorites, ProductRepository products) {
        this.favorites = favorites;
        this.products = products;
    }

    public void addFavorite(UUID userId, UUID productId) {
        favorites.save(userId, productId);
    }

    public void removeFavorite(UUID userId, UUID productId) {
        favorites.delete(userId, productId);
    }

    public List<ProductSummary> listFavorites(UUID userId) {
        return favorites.findProductIdsByUserId(userId).stream()
                .map(id -> products.findByIdAndStatus(id, ProductStatus.ACTIVE))
                .filter(java.util.Optional::isPresent)
                .map(opt -> ProductSummary.from(opt.get()))
                .collect(Collectors.toList());
    }
}
