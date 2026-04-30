package com.arquixpress.marketplace.logistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "logistics_operator")
public class LogisticsOperator {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID appUserId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "center_id", nullable = false)
    private LogisticsCenter center;

    protected LogisticsOperator() {
    }

    public LogisticsOperator(UUID id, UUID appUserId, LogisticsCenter center) {
        this.id = id;
        this.appUserId = appUserId;
        this.center = center;
    }

    public UUID id() { return id; }
    public UUID appUserId() { return appUserId; }
    public LogisticsCenter center() { return center; }
}
