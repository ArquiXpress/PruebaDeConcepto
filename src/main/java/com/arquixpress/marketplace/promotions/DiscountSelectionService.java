package com.arquixpress.marketplace.promotions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DiscountSelectionService {
    private final int minDiscountPct;
    private final int maxDiscountPct;

    public DiscountSelectionService() {
        this(1, 80);
    }

    public DiscountSelectionService(int minDiscountPct, int maxDiscountPct) {
        this.minDiscountPct = minDiscountPct;
        this.maxDiscountPct = maxDiscountPct;
    }

    public Map<UUID, BigDecimal> seleccionarProductosConDescuento(
            Map<UUID, BigDecimal> preciosBase,
            Set<UUID> productosSeleccionados,
            int descuentoPct) {
        if (descuentoPct < minDiscountPct || descuentoPct > maxDiscountPct) {
            throw new IllegalArgumentException("El descuento esta fuera de las reglas permitidas");
        }
        if (productosSeleccionados == null || productosSeleccionados.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar al menos un producto");
        }

        Map<UUID, BigDecimal> preciosFinales = new HashMap<>(preciosBase);
        for (UUID productId : productosSeleccionados) {
            BigDecimal precioBase = preciosBase.get(productId);
            if (precioBase != null) {
                BigDecimal factor = BigDecimal.valueOf(100 - descuentoPct)
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                preciosFinales.put(productId, precioBase.multiply(factor).setScale(2, RoundingMode.HALF_UP));
            }
        }
        return preciosFinales;
    }
}
