package com.arquixpress.marketplace.promotions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class AdvertisingCampaign {
    public enum Status { PENDING_PAYMENT, ACTIVE }

    private final UUID id;
    private final UUID sellerId;
    private final Instant startsAt;
    private final Instant endsAt;
    private Status status;

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

    public void activate() {
        this.status = Status.ACTIVE;
    }
}
