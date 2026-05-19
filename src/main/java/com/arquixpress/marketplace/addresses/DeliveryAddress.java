package com.arquixpress.marketplace.addresses;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_address")
public class DeliveryAddress {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String addressLine;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt;

    protected DeliveryAddress() {
    }

    public DeliveryAddress(UUID id, UUID userId, String label, String recipient, String addressLine, String city,
            String phone) {
        this.id = id;
        this.userId = userId;
        this.label = label.trim();
        this.recipient = recipient.trim();
        this.addressLine = addressLine.trim();
        this.city = city.trim();
        this.phone = phone.trim();
        this.active = true;
        this.createdAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String label() { return label; }
    public String recipient() { return recipient; }
    public String addressLine() { return addressLine; }
    public String city() { return city; }
    public String phone() { return phone; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }

    public String snapshot() {
        return recipient + " - " + addressLine + ", " + city + " - " + phone;
    }
}
