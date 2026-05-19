package com.arquixpress.marketplace.promotions;

import com.arquixpress.marketplace.payments.PaymentGatewayResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RF31FinishExpiredCampaignTest {

    @Test
    void finalizesOnlyActiveCampaignsWhoseEndDateHasArrived() {
        AdvertisingCampaignService service = service();
        Instant start = Instant.parse("2026-05-01T00:00:00Z");
        Instant now = Instant.parse("2026-05-08T00:00:00Z");
        AdvertisingCampaign expired = new AdvertisingCampaign(UUID.randomUUID(), start, 7);
        AdvertisingCampaign stillRunning = new AdvertisingCampaign(UUID.randomUUID(), start, 10);
        AdvertisingCampaign pendingPayment = new AdvertisingCampaign(UUID.randomUUID(), start, 7);
        expired.activate();
        stillRunning.activate();

        int finalized = service.finalizarCampaniasVencidas(List.of(expired, stillRunning, pendingPayment), now);

        assertThat(finalized).isEqualTo(1);
        assertThat(expired.status()).isEqualTo(AdvertisingCampaign.Status.FINISHED);
        assertThat(stillRunning.status()).isEqualTo(AdvertisingCampaign.Status.ACTIVE);
        assertThat(pendingPayment.status()).isEqualTo(AdvertisingCampaign.Status.PENDING_PAYMENT);
    }

    @Test
    void keepsActiveCampaignOpenWhenExpirationDateHasNotArrived() {
        AdvertisingCampaignService service = service();
        Instant start = Instant.parse("2026-05-01T00:00:00Z");
        Instant beforeExpiration = Instant.parse("2026-05-07T23:59:59Z");
        AdvertisingCampaign campaign = new AdvertisingCampaign(UUID.randomUUID(), start, 7);
        campaign.activate();

        int finalized = service.finalizarCampaniasVencidas(List.of(campaign), beforeExpiration);

        assertThat(finalized).isZero();
        assertThat(campaign.status()).isEqualTo(AdvertisingCampaign.Status.ACTIVE);
    }

    private AdvertisingCampaignService service() {
        return new AdvertisingCampaignService((orderId, amount, key) -> PaymentGatewayResult.pending("No usado"));
    }
}
