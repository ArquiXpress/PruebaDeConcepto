package com.arquixpress.marketplace.promotions;

import com.arquixpress.marketplace.payments.PaymentGatewayClient;
import com.arquixpress.marketplace.payments.PaymentGatewayResult;
import com.arquixpress.marketplace.payments.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AdvertisingCampaignService {
    private final PaymentGatewayClient paymentGateway;

    public AdvertisingCampaignService(PaymentGatewayClient paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public AdvertisingCampaign crearCampania(UUID sellerId, Instant inicio, int duracionDias) {
        return new AdvertisingCampaign(sellerId, inicio, duracionDias);
    }

    public PaymentStatus pagarYActivar(AdvertisingCampaign campaign, BigDecimal amount) {
        PaymentGatewayResult result = paymentGateway.charge(campaign.id(), amount, campaign.id().toString());
        if (result.status() == PaymentStatus.APPROVED) {
            campaign.activate();
        }
        return result.status();
    }

    public int finalizarCampaniasVencidas(List<AdvertisingCampaign> campaigns, Instant now) {
        int finished = 0;
        for (AdvertisingCampaign campaign : campaigns) {
            if (campaign.finishIfExpired(now)) {
                finished++;
            }
        }
        return finished;
    }

    public AdvertisingCampaignMetrics registrarImpresion(AdvertisingCampaign campaign) {
        campaign.registerImpression();
        return campaign.metrics();
    }

    public AdvertisingCampaignMetrics registrarClick(AdvertisingCampaign campaign) {
        campaign.registerClick();
        return campaign.metrics();
    }

    public AdvertisingCampaignMetrics metricas(AdvertisingCampaign campaign) {
        return campaign.metrics();
    }
}
