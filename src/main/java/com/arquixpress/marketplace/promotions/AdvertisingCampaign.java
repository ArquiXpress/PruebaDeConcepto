package com.arquixpress.marketplace.promotions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class AdvertisingCampaign {
    public enum Status { PENDING_PAYMENT, ACTIVE, FINISHED }

    private final UUID id;
    private final UUID sellerId;
    private final Instant startsAt;
    private final Instant endsAt;
    private Status status;
    private long impressions;
    private long clicks;

    public AdvertisingCampaign(UUID sellerId, Instant startsAt, int durationDays) {
        if (durationDays <= 0) {
            throw new IllegalArgumentException("La duracion debe ser mayor a 0 dias");
        }
        this.id = UUID.randomUUID();
        this.sellerId = sellerId;
        this.startsAt = startsAt;
        this.endsAt = startsAt.plus(durationDays, ChronoUnit.DAYS);
        this.status = Status.PENDING_PAYMENT;
    }

    public UUID id() { return id; }
    public UUID sellerId() { return sellerId; }
    public Instant startsAt() { return startsAt; }
    public Instant endsAt() { return endsAt; }
    public Status status() { return status; }
    public long impressions() { return impressions; }
    public long clicks() { return clicks; }

    public void activate() {
        this.status = Status.ACTIVE;
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(endsAt);
    }

    public boolean finishIfExpired(Instant now) {
        if (status == Status.ACTIVE && isExpiredAt(now)) {
            status = Status.FINISHED;
            return true;
        }
        return false;
    }

    public void registerImpression() {
        impressions++;
    }

    public void registerClick() {
        clicks++;
    }

    public AdvertisingCampaignMetrics metrics() {
        return new AdvertisingCampaignMetrics(id, impressions, clicks);
    }
}
