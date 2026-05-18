package com.arquixpress.marketplace.promotions;

import com.arquixpress.marketplace.payments.PaymentGatewayClient;
import com.arquixpress.marketplace.payments.PaymentGatewayResult;
import com.arquixpress.marketplace.payments.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
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
}
