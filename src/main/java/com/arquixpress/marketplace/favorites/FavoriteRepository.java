package com.arquixpress.marketplace.favorites;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository {
    void save(UUID userId, UUID productId);
    void delete(UUID userId, UUID productId);
    List<UUID> findProductIdsByUserId(UUID userId);
}
