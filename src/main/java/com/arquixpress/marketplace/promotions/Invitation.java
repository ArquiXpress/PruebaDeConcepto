package com.arquixpress.marketplace.promotions;

import java.util.UUID;

public class Invitation {
    public enum Status { PENDING, ACCEPTED, DECLINED }

    private UUID id;
    private UUID promotionId;
    private UUID sellerId;
    private Status status;

    public Invitation(UUID promotionId, UUID sellerId) {
        this.id = UUID.randomUUID();
        this.promotionId = promotionId;
        this.sellerId = sellerId;
        this.status = Status.PENDING;
    }

    public UUID id() { return id; }
    public UUID promotionId() { return promotionId; }
    public UUID sellerId() { return sellerId; }
    public Status status() { return status; }

    public void accept() { this.status = Status.ACCEPTED; }
    public void decline() { this.status = Status.DECLINED; }
}
