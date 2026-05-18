package com.arquixpress.marketplace.promotions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public class PromotionPricingService {

    public BigDecimal calcularPrecioPromocional(BigDecimal precioBase, int descuentoPct) {
        BigDecimal factor = BigDecimal.valueOf(100 - descuentoPct)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return precioBase.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal precioVigente(
            BigDecimal precioBase,
            BigDecimal precioPromocional,
            Instant ahora,
            Instant inicio,
            Instant fin) {
        boolean vigente = !ahora.isBefore(inicio) && !ahora.isAfter(fin);
        return vigente ? precioPromocional : precioBase;
    }
}
