package com.arquixpress.marketplace.orders;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "marketplace_order")
public class OrderEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID buyerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus shipmentStatus;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "shipping_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "shipping_city")
    private String shippingCity;

    @Column(name = "logistics_center_id")
    private UUID logisticsCenterId;

    @Column(name = "logistics_operator_id")
    private UUID logisticsOperatorId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    protected OrderEntity() {
    }

    public OrderEntity(UUID id, UUID buyerId) {
        this.id = id;
        this.buyerId = buyerId;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.shipmentStatus = ShipmentStatus.PREPARATION;
        this.total = BigDecimal.ZERO;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void addLine(UUID productId, int quantity, BigDecimal unitPrice) {
        lines.add(new OrderLine(UUID.randomUUID(), this, productId, quantity, unitPrice));
        total = total.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        updatedAt = Instant.now();
    }

    public void markPaid() {
        status = OrderStatus.PAID;
        updatedAt = Instant.now();
    }

    public void markPendingPayment() {
        status = OrderStatus.PENDING_PAYMENT;
        updatedAt = Instant.now();
    }

    public void markRejected() {
        status = OrderStatus.PAYMENT_REJECTED;
        updatedAt = Instant.now();
    }

    public void updateShipment(ShipmentStatus next) {
        if (status != OrderStatus.PAID) {
            throw new IllegalArgumentException("Solo pedidos pagados pueden cambiar estado logistico");
        }
        if (Math.abs(next.ordinal() - shipmentStatus.ordinal()) > 1) {
            throw new IllegalArgumentException("Solo se permite avanzar o retroceder un paso logistico");
        }
        shipmentStatus = next;
        updatedAt = Instant.now();
    }

    public void setShipping(String address, String city, BigDecimal cost) {
        this.shippingAddress = address;
        this.shippingCity = city;
        this.shippingCost = cost == null ? BigDecimal.ZERO : cost;
        this.total = lines.stream()
                .map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(this.shippingCost);
        updatedAt = Instant.now();
    }

    public void assignCenter(UUID centerId, UUID operatorId) {
        this.logisticsCenterId = centerId;
        this.logisticsOperatorId = operatorId;
        this.updatedAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID buyerId() { return buyerId; }
    public OrderStatus status() { return status; }
    public ShipmentStatus shipmentStatus() { return shipmentStatus; }
    public BigDecimal total() { return total; }
    public BigDecimal shippingCost() { return shippingCost; }
    public String shippingAddress() { return shippingAddress; }
    public String shippingCity() { return shippingCity; }
    public UUID logisticsCenterId() { return logisticsCenterId; }
    public UUID logisticsOperatorId() { return logisticsOperatorId; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public List<OrderLine> lines() { return List.copyOf(lines); }
}
