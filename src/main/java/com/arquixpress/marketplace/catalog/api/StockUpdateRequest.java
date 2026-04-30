package com.arquixpress.marketplace.catalog.api;

import jakarta.validation.constraints.Min;

public record StockUpdateRequest(@Min(0) int stockAvailable) {
}