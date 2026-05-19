package com.arquixpress.marketplace.promotions;

import com.arquixpress.marketplace.payments.PaymentGatewayResult;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RF32CampaignMetricsTest {

    @Test
    void recordsAndShowsBasicCampaignMetrics() {
        AdvertisingCampaignService service = new AdvertisingCampaignService(
                (orderId, amount, key) -> PaymentGatewayResult.pending("No usado"));
        AdvertisingCampaign campaign = new AdvertisingCampaign(UUID.randomUUID(), Instant.parse("2026-05-01T00:00:00Z"), 5);

        service.registrarImpresion(campaign);
        service.registrarImpresion(campaign);
        service.registrarClick(campaign);

        AdvertisingCampaignMetrics metrics = service.metricas(campaign);

        assertThat(metrics.campaignId()).isEqualTo(campaign.id());
        assertThat(metrics.impressions()).isEqualTo(2);
        assertThat(metrics.clicks()).isEqualTo(1);
    }
}
