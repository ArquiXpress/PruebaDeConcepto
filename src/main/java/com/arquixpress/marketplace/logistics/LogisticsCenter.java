package com.arquixpress.marketplace.logistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "logistics_center")
public class LogisticsCenter {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String city;

    @Column(nullable = false)
    private String displayName;

    protected LogisticsCenter() {
    }

    public LogisticsCenter(UUID id, String city, String displayName) {
        this.id = id;
        this.city = city;
        this.displayName = displayName;
    }

    public UUID id() { return id; }
    public String city() { return city; }
    public String displayName() { return displayName; }
}
