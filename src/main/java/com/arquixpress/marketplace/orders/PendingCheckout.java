package com.arquixpress.marketplace.orders;

import java.math.BigDecimal;
import java.util.UUID;

record PendingCheckout(UUID orderId, BigDecimal total) {
}
