package com.arquixpress.marketplace.promotions;

import com.arquixpress.marketplace.payments.PaymentGatewayClient;
import com.arquixpress.marketplace.payments.PaymentGatewayResult;
import com.arquixpress.marketplace.payments.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AdvertisingCampaignServiceTest {

    @Mock
    PaymentGatewayClient paymentGateway;

    AdvertisingCampaignService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AdvertisingCampaignService(paymentGateway);
    }

    @Test
    void crearCampaniaPublicitaria_porDuracion_calculaFechaFin() {
        Instant inicio = Instant.parse("2026-05-18T10:00:00Z");
        AdvertisingCampaign campaign = service.crearCampania(UUID.randomUUID(), inicio, 7);

        assertThat(campaign.startsAt()).isEqualTo(inicio);
        assertThat(campaign.endsAt()).isEqualTo(inicio.plus(7, ChronoUnit.DAYS));
    }

    @Test
    void crearCampaniaPublicitaria_duracionInvalida_lanzaError() {
        assertThatThrownBy(() -> service.crearCampania(UUID.randomUUID(), Instant.now(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duracion");
    }

    @Test
    void pagarCampania_pagoAprobado_activaCampania() {
        AdvertisingCampaign campaign = service.crearCampania(UUID.randomUUID(), Instant.now(), 3);
        when(paymentGateway.charge(any(), any(), anyString())).thenReturn(PaymentGatewayResult.approved("ok-ref"));

        PaymentStatus status = service.pagarYActivar(campaign, new BigDecimal("50000.00"));

        assertThat(status).isEqualTo(PaymentStatus.APPROVED);
        assertThat(campaign.status()).isEqualTo(AdvertisingCampaign.Status.ACTIVE);
    }

    @Test
    void pagarCampania_pagoRechazado_noActivaCampania() {
        AdvertisingCampaign campaign = service.crearCampania(UUID.randomUUID(), Instant.now(), 3);
        when(paymentGateway.charge(any(), any(), anyString())).thenReturn(PaymentGatewayResult.rejected("fail-ref"));

        PaymentStatus status = service.pagarYActivar(campaign, new BigDecimal("50000.00"));

        assertThat(status).isEqualTo(PaymentStatus.REJECTED);
        assertThat(campaign.status()).isEqualTo(AdvertisingCampaign.Status.PENDING_PAYMENT);
    }
}
