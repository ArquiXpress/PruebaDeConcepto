package com.arquixpress.marketplace.promotions;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SponsoredProductViewServiceTest {

    @Test
    void mostrarPatrocinados_enLanding_muestraEtiquetaPatrocinado() {
        SponsoredProductViewService service = new SponsoredProductViewService();
        UUID patrocinado = UUID.randomUUID();
        UUID normal = UUID.randomUUID();

        var result = service.marcarPatrocinados(List.of(
                new SponsoredProductViewService.ProductCard(patrocinado, "Taladro", null, false),
                new SponsoredProductViewService.ProductCard(normal, "Martillo", null, false)
        ), Set.of(patrocinado));

        assertThat(result.get(0).label()).isEqualTo("Patrocinado");
        assertThat(result.get(0).sponsored()).isTrue();
        assertThat(result.get(1).label()).isNull();
        assertThat(result.get(1).sponsored()).isFalse();
    }

    @Test
    void mostrarPatrocinados_enBusquedaYCategorias_mantieneEtiqueta() {
        SponsoredProductViewService service = new SponsoredProductViewService();
        UUID patrocinado = UUID.randomUUID();

        var busqueda = service.marcarPatrocinados(List.of(
                new SponsoredProductViewService.ProductCard(patrocinado, "Sierra", null, false)
        ), Set.of(patrocinado));

        var categorias = service.marcarPatrocinados(List.of(
                new SponsoredProductViewService.ProductCard(patrocinado, "Sierra", null, false)
        ), Set.of(patrocinado));

        assertThat(busqueda.get(0).label()).isEqualTo("Patrocinado");
        assertThat(categorias.get(0).label()).isEqualTo("Patrocinado");
    }
}
