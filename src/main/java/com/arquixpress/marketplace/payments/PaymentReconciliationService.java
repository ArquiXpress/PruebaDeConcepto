package com.arquixpress.marketplace.payments;

import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.notifications.NotificationOutbox;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;
import com.arquixpress.marketplace.notifications.NotificationService;
import com.arquixpress.marketplace.orders.CheckoutProblem;
import com.arquixpress.marketplace.orders.OrderEntity;
import com.arquixpress.marketplace.orders.OrderLine;
import com.arquixpress.marketplace.orders.OrderRepository;
import com.arquixpress.marketplace.orders.OrderStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PaymentReconciliationService {
    private final PaymentTransactionRepository payments;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final PaymentGatewayClient paymentGateway;
    private final NotificationOutboxRepository outbox;
    private final NotificationService notifications;
    private final TransactionTemplate tx;

    public PaymentReconciliationService(PaymentTransactionRepository payments, OrderRepository orders,
            ProductRepository products, PaymentGatewayClient paymentGateway, NotificationOutboxRepository outbox,
            NotificationService notifications, TransactionTemplate tx) {
        this.payments = payments;
        this.orders = orders;
        this.products = products;
        this.paymentGateway = paymentGateway;
        this.outbox = outbox;
        this.notifications = notifications;
        this.tx = tx;
    }

    public PaymentReconciliationResponse consultAndSync(UUID orderId) {
        return tx.execute(status -> {
            OrderEntity order = orders.findWithLines(orderId)
                    .orElseThrow(() -> new CheckoutProblem("ORDER_NOT_FOUND", "Pedido no encontrado", HttpStatus.NOT_FOUND));
            PaymentTransaction payment = payments.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                    .orElseThrow(() -> new CheckoutProblem("PAYMENT_NOT_FOUND", "No hay pagos registrados para el pedido",
                            HttpStatus.NOT_FOUND));
            OrderStatus previousOrderStatus = order.status();
            PaymentGatewayResult result = paymentGateway.checkStatus(order.id(), payment.gatewayReference());
            payment.apply(result);
            if (result.status() == PaymentStatus.APPROVED) {
                order.markPaid();
                notifications.notify(order.buyerId(), "PAYMENT_APPROVED", "Pago aprobado",
                        "La pasarela confirmo el pago de tu pedido.", "/mis-compras");
                outbox.save(new NotificationOutbox("ORDER", order.id(), "PAYMENT_APPROVED",
                        "{\"orderId\":\"" + order.id() + "\"}"));
            } else if (result.status() == PaymentStatus.REJECTED) {
                order.markRejected();
                if (previousOrderStatus == OrderStatus.PAID || previousOrderStatus == OrderStatus.PENDING_PAYMENT) {
                    for (OrderLine line : order.lines()) {
                        products.releaseStock(line.productId(), line.quantity());
                    }
                }
                notifications.notify(order.buyerId(), "PAYMENT_REJECTED", "Pago rechazado",
                        "La pasarela rechazo el pago de tu pedido.", "/mis-compras");
                outbox.save(new NotificationOutbox("ORDER", order.id(), "PAYMENT_REJECTED",
                        "{\"orderId\":\"" + order.id() + "\"}"));
            }
            return new PaymentReconciliationResponse(order.id(), payment.transactionId(), payment.status(), order.status(),
                    result.message());
        });
    }
}
