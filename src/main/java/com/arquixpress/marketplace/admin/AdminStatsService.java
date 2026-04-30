package com.arquixpress.marketplace.admin;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class AdminStatsService {
    private final EntityManager em;

    public AdminStatsService(EntityManager em) {
        this.em = em;
    }

    public AdminStatsResponse getStats() {
        long totalUsers = (Long) em.createQuery("SELECT COUNT(u) FROM AppUser u").getSingleResult();
        long totalProducts = (Long) em.createQuery("SELECT COUNT(p) FROM Product p").getSingleResult();
        long totalOrders = (Long) em.createQuery("SELECT COUNT(o) FROM OrderEntity o").getSingleResult();

        Object totalRevenueObj = em.createQuery(
                "SELECT SUM(o.total) FROM OrderEntity o WHERE o.status = 'COMPLETED'")
            .getSingleResult();
        long totalRevenue = totalRevenueObj != null ? ((Number) totalRevenueObj).longValue() : 0;

        return new AdminStatsResponse(totalUsers, totalProducts, totalOrders, totalRevenue);
    }
}
