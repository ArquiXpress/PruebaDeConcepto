package com.arquixpress.marketplace.promotions;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PromotionPricingServiceTest {

    @Test
    void aplicarPrecioPromocional_duranteVigencia_usaPrecioConDescuento() {
        PromotionPricingService service = new PromotionPricingService();
        Instant inicio = Instant.parse("2026-05-18T00:00:00Z");
        Instant fin = Instant.parse("2026-05-20T00:00:00Z");
        Instant ahora = Instant.parse("2026-05-19T00:00:00Z");

        BigDecimal promo = service.calcularPrecioPromocional(new BigDecimal("100.00"), 20);
        BigDecimal vigente = service.precioVigente(new BigDecimal("100.00"), promo, ahora, inicio, fin);

        assertThat(vigente).isEqualByComparingTo("80.00");
    }

    @Test
    void aplicarPrecioPromocional_fueraVigencia_reviertePrecioBase() {
        PromotionPricingService service = new PromotionPricingService();
        Instant inicio = Instant.parse("2026-05-18T00:00:00Z");
        Instant fin = Instant.parse("2026-05-20T00:00:00Z");
        Instant ahora = Instant.parse("2026-05-21T00:00:00Z");

        BigDecimal promo = service.calcularPrecioPromocional(new BigDecimal("100.00"), 20);
        BigDecimal vigente = service.precioVigente(new BigDecimal("100.00"), promo, ahora, inicio, fin);

        assertThat(vigente).isEqualByComparingTo("100.00");
    }
}
