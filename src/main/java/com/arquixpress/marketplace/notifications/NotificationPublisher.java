package com.arquixpress.marketplace.notifications;

import com.arquixpress.marketplace.identity.AppUser;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.orders.OrderEntity;
import com.arquixpress.marketplace.orders.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);

    private final NotificationOutboxRepository outbox;
    private final EmailService email;
    private final AppUserRepository users;
    private final OrderRepository orders;
    private final ObjectMapper mapper;
    private final boolean enabled;

    NotificationPublisher(
            NotificationOutboxRepository outbox,
            EmailService email,
            AppUserRepository users,
            OrderRepository orders,
            @Value("${app.notifications.enabled}") boolean enabled) {
        this.outbox  = outbox;
        this.email   = email;
        this.users   = users;
        this.orders  = orders;
        this.mapper  = new ObjectMapper();
        this.enabled = enabled;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.notifications.retry-delay}")
    void publishPending() {
        for (NotificationOutbox event : outbox.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)) {
            if (!enabled) {
                event.sent();
                continue;
            }
            try {
                dispatch(event);
                event.sent();
            } catch (Exception ex) {
                log.warn("Fallo al procesar notificación {} tipo {}: {}",
                        event.id(), event.eventType(), ex.getMessage());
                event.failed();
            }
        }
    }

    // ── Enrutador de eventos ─────────────────────────────────────────────────

    private void dispatch(NotificationOutbox event) throws Exception {
        JsonNode payload = mapper.readTree(event.payload());
        switch (event.eventType()) {

            case "USER_REGISTERED" -> {
                String userId = payload.get("userId").asText();
                findUser(userId).ifPresent(u ->
                        email.sendWelcome(u.email(), u.displayName()));
            }

            case "PASSWORD_CHANGED" -> {
                String userId = payload.get("userId").asText();
                findUser(userId).ifPresent(u ->
                        email.sendPasswordChanged(u.email(), u.displayName()));
            }

            case "SELLER_APPROVED" -> {
                String userId = payload.get("userId").asText();
                findUser(userId).ifPresent(u ->
                        email.sendSellerApproved(u.email(), u.displayName()));
            }

            case "SELLER_REJECTED" -> {
                String userId = payload.get("userId").asText();
                findUser(userId).ifPresent(u ->
                        email.sendSellerRejected(u.email(), u.displayName()));
            }

            case "ORDER_PAID" -> {
                String orderId = payload.get("orderId").asText();
                findOrderWithBuyer(orderId).ifPresent(pair ->
                        email.sendOrderPaid(pair.userEmail(), pair.displayName(), orderId));
            }

            case "PAYMENT_REJECTED" -> {
                String orderId = payload.get("orderId").asText();
                findOrderWithBuyer(orderId).ifPresent(pair ->
                        email.sendPaymentRejected(pair.userEmail(), pair.displayName(), orderId));
            }

            case "SELLER_SALE" -> {
                String orderId = payload.get("orderId").asText();
                String sellerId = payload.get("sellerId").asText();
                findUser(sellerId).ifPresent(u ->
                        email.sendSellerSale(u.email(), u.displayName(), orderId));
            }

            case "SHIPMENT_UPDATED" -> {
                String orderId  = payload.get("orderId").asText();
                String status   = payload.get("shipmentStatus").asText();
                findOrderWithBuyer(orderId).ifPresent(pair ->
                        email.sendShipmentUpdated(pair.userEmail(), pair.displayName(),
                                orderId, status));
            }

            case "SHIPMENT_DELIVERED" -> {
                String orderId = payload.get("orderId").asText();
                findOrderWithBuyer(orderId).ifPresent(pair ->
                        email.sendShipmentDelivered(pair.userEmail(), pair.displayName(), orderId));
            }

            case "ORDER_CANCELLED" -> {
                String orderId = payload.get("orderId").asText();
                findOrderWithBuyer(orderId).ifPresent(pair ->
                        email.sendOrderCancelled(pair.userEmail(), pair.displayName(), orderId));
            }

            default -> log.warn("Tipo de notificación desconocido: {}", event.eventType());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Optional<AppUser> findUser(String userId) {
        return users.findById(UUID.fromString(userId));
    }

    private Optional<BuyerInfo> findOrderWithBuyer(String orderId) {
        return orders.findWithLines(UUID.fromString(orderId)).flatMap(order ->
                users.findById(order.buyerId()).map(u ->
                        new BuyerInfo(u.email(), u.displayName())));
    }

    private record BuyerInfo(String userEmail, String displayName) {}
}
