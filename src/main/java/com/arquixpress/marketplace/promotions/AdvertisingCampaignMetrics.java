package com.arquixpress.marketplace.promotions;

import java.util.UUID;

public record AdvertisingCampaignMetrics(UUID campaignId, long impressions, long clicks) {
}
