package com.arquixpress.marketplace.promotions;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountSelectionServiceTest {

    @Test
    void seleccionarProductosYDefinirDescuento_dentroReglas_aplicaDescuento() {
        DiscountSelectionService service = new DiscountSelectionService(5, 60);
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        Map<UUID, BigDecimal> result = service.seleccionarProductosConDescuento(
                Map.of(p1, new BigDecimal("100.00"), p2, new BigDecimal("50.00")),
                Set.of(p1),
                10);

        assertThat(result.get(p1)).isEqualByComparingTo("90.00");
        assertThat(result.get(p2)).isEqualByComparingTo("50.00");
    }

    @Test
    void seleccionarProductosYDefinirDescuento_fueraReglas_lanzaError() {
        DiscountSelectionService service = new DiscountSelectionService(5, 60);
        UUID p1 = UUID.randomUUID();

        assertThatThrownBy(() -> service.seleccionarProductosConDescuento(
                Map.of(p1, new BigDecimal("100.00")),
                Set.of(p1),
                80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fuera de las reglas");
    }
}
