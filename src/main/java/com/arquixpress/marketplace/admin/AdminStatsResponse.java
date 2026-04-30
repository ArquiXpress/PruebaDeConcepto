package com.arquixpress.marketplace.admin;

public record AdminStatsResponse(
    long totalUsers,
    long totalProducts,
    long totalOrders,
    long totalRevenue
) {}
