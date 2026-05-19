package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.addresses.DeliveryAddress;
import com.arquixpress.marketplace.addresses.DeliveryAddressRepository;
import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.catalog.ProductStatus;
import com.arquixpress.marketplace.logistics.LogisticsCenter;
import com.arquixpress.marketplace.logistics.LogisticsCenterRepository;
import com.arquixpress.marketplace.notifications.NotificationOutbox;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;
import com.arquixpress.marketplace.notifications.NotificationService;
import com.arquixpress.marketplace.payments.PaymentGatewayClient;
import com.arquixpress.marketplace.payments.PaymentGatewayResult;
import com.arquixpress.marketplace.payments.PaymentStatus;
import com.arquixpress.marketplace.payments.PaymentTransaction;
import com.arquixpress.marketplace.payments.PaymentTransactionRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CheckoutService {
    private static final String DEFAULT_PAYMENT_METHOD = "Pago simulado";

    private final ProductRepository products;
    private final OrderRepository orders;
    private final PaymentTransactionRepository payments;
    private final PaymentGatewayClient paymentGateway;
    private final NotificationOutboxRepository outbox;
    private final NotificationService notifications;
    private final LogisticsCenterRepository centers;
    private final TransactionTemplate tx;
    private final DeliveryAddressRepository deliveryAddresses;

    public CheckoutService(ProductRepository products, OrderRepository orders, PaymentTransactionRepository payments,
            PaymentGatewayClient paymentGateway, NotificationOutboxRepository outbox,
            NotificationService notifications, LogisticsCenterRepository centers, TransactionTemplate tx) {
        this(products, orders, payments, paymentGateway, outbox, notifications, centers, tx, null);
    }

    @Autowired
    public CheckoutService(ProductRepository products, OrderRepository orders, PaymentTransactionRepository payments,
            PaymentGatewayClient paymentGateway, NotificationOutboxRepository outbox,
            NotificationService notifications, LogisticsCenterRepository centers, TransactionTemplate tx,
            DeliveryAddressRepository deliveryAddresses) {
        this.products = products;
        this.orders = orders;
        this.payments = payments;
        this.paymentGateway = paymentGateway;
        this.outbox = outbox;
        this.notifications = notifications;
        this.centers = centers;
        this.tx = tx;
        this.deliveryAddresses = deliveryAddresses;
    }

    public CheckoutResponse checkout(UUID buyerId, CheckoutRequest request, String idempotencyKey) {
        String key = requireKey(idempotencyKey);
        var existing = payments.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            OrderEntity order = findOrder(existing.get().orderId());
            return buildCheckoutResponse(order, existing.get(), "Resultado idempotente; no se crea otro cobro ni otra orden");
        }

        PendingCheckout pending = tx.execute(status -> createPendingOrder(buyerId, request, key, resolvePaymentMethod(request.paymentMethod())));
        PaymentGatewayResult result = paymentGateway.charge(pending.orderId(), pending.total(), key);
        return tx.execute(status -> applyPaymentResult(pending.orderId(), key, result));
    }

    public CheckoutResponse retryPayment(UUID orderId, String idempotencyKey) {
        String key = requireKey(idempotencyKey);
        var existing = payments.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            OrderEntity order = findOrder(existing.get().orderId());
            return buildCheckoutResponse(order, existing.get(), "Resultado idempotente de reintento");
        }

        OrderEntity order = findOrder(orderId);
        if (order.status() != OrderStatus.PENDING_PAYMENT && order.status() != OrderStatus.PAYMENT_REJECTED) {
            throw new CheckoutProblem("ORDER_NOT_RETRYABLE", "El pedido no esta pendiente de pago", HttpStatus.CONFLICT);
        }

        tx.executeWithoutResult(status -> prepareRetry(orderId, key));
        PaymentGatewayResult result = paymentGateway.charge(order.id(), order.total(), key);
        return tx.execute(status -> applyPaymentResult(order.id(), key, result));
    }

    public OrderResponse getOrder(UUID orderId) {
        return OrderResponse.from(findOrder(orderId));
    }

    public java.util.List<OrderResponse> getOrdersForBuyer(UUID buyerId) {
        return orders.findByBuyerWithLines(buyerId).stream().map(OrderResponse::from).toList();
    }

    public OrderResponse updateShipment(UUID orderId, ShipmentStatus next) {
        return tx.execute(status -> {
            OrderEntity order = findOrder(orderId);
            order.updateShipment(next);
            return OrderResponse.from(order);
        });
    }

    public OrderResponse cancelOrder(UUID buyerId, UUID orderId) {
        return tx.execute(status -> {
            OrderEntity order = findOrder(orderId);
            if (!order.buyerId().equals(buyerId)) {
                throw new CheckoutProblem("ORDER_NOT_FOUND", "Pedido no encontrado", HttpStatus.NOT_FOUND);
            }
            OrderStatus previousStatus = order.status();
            order.cancelBeforeDispatch();
            if (previousStatus == OrderStatus.PAID || previousStatus == OrderStatus.PENDING_PAYMENT) {
                for (OrderLine line : order.lines()) {
                    products.releaseStock(line.productId(), line.quantity());
                }
            }
            outbox.save(new NotificationOutbox("ORDER", order.id(), "ORDER_CANCELLED",
                    "{\"orderId\":\"" + order.id() + "\"}"));
            notifications.notify(order.buyerId(), "ORDER_CANCELLED", "Pedido cancelado",
                    "Tu pedido fue cancelado antes del despacho.", "/mis-compras");
            return OrderResponse.from(order);
        });
    }

    public List<OrderResponse> listShipmentsByCenter(UUID centerId) {
        List<OrderEntity> list = centerId == null ? orders.findAllPaid() : orders.findPaidByCenter(centerId);
        return list.stream().map(OrderResponse::from).toList();
    }

    private PendingCheckout createPendingOrder(UUID buyerId, CheckoutRequest request, String idempotencyKey, String paymentMethod) {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), buyerId);
        UUID centerId = pickCenterId();
        if (centerId != null) {
            order.assignCenter(centerId, null);
        }
        assignDeliveryAddress(order, buyerId, request.deliveryAddressId());
        for (CheckoutItemRequest item : request.items()) {
            Product product = products.findByIdAndStatus(item.productId(), ProductStatus.ACTIVE)
                    .orElseThrow(() -> new CheckoutProblem("PRODUCT_NOT_FOUND", "Producto no encontrado", HttpStatus.NOT_FOUND));
            int reserved = products.reserveStock(item.productId(), item.quantity());
            if (reserved != 1) {
                throw new CheckoutProblem("INSUFFICIENT_STOCK", "Stock insuficiente para el producto " + item.productId(), HttpStatus.CONFLICT);
            }
            order.addLine(product.id(), item.quantity(), product.price());
        }
        orders.save(order);
        savePendingPayment(order.id(), idempotencyKey, order.total(), paymentMethod);
        return new PendingCheckout(order.id(), order.total());
    }

    private void assignDeliveryAddress(OrderEntity order, UUID buyerId, UUID deliveryAddressId) {
        if (deliveryAddressId == null) {
            return;
        }
        if (deliveryAddresses == null) {
            throw new CheckoutProblem("DELIVERY_ADDRESS_NOT_AVAILABLE", "No hay libreta de direcciones configurada",
                    HttpStatus.CONFLICT);
        }
        DeliveryAddress address = deliveryAddresses.findByIdAndUserIdAndActiveTrue(deliveryAddressId, buyerId)
                .orElseThrow(() -> new CheckoutProblem("DELIVERY_ADDRESS_NOT_FOUND",
                        "La direccion de entrega no pertenece al comprador o no esta activa", HttpStatus.NOT_FOUND));
        order.assignDeliveryAddress(address.id(), address.snapshot());
    }

    private void prepareRetry(UUID orderId, String idempotencyKey) {
        OrderEntity order = findOrder(orderId);
        if (order.status() == OrderStatus.PAYMENT_REJECTED) {
            for (OrderLine line : order.lines()) {
                int reserved = products.reserveStock(line.productId(), line.quantity());
                if (reserved != 1) {
                    throw new CheckoutProblem("INSUFFICIENT_STOCK", "Stock insuficiente para reintentar el pedido", HttpStatus.CONFLICT);
                }
            }
            order.markPendingPayment();
        }
        savePendingPayment(order.id(), idempotencyKey, order.total(), latestPaymentFor(order.id()).paymentMethod());
    }

    private CheckoutResponse applyPaymentResult(UUID orderId, String idempotencyKey, PaymentGatewayResult result) {
        PaymentTransaction payment = payments.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new CheckoutProblem("PAYMENT_NOT_FOUND", "Transaccion no encontrada", HttpStatus.INTERNAL_SERVER_ERROR));
        OrderEntity order = findOrder(orderId);
        payment.apply(result);
        if (result.status() == PaymentStatus.APPROVED) {
            order.markPaid();
            outbox.save(new NotificationOutbox("ORDER", order.id(), "ORDER_PAID", "{\"orderId\":\"" + order.id() + "\"}"));
            notifications.notify(order.buyerId(), "ORDER_PAID", "Compra confirmada",
                    "Tu compra fue aprobada y ya entro al proceso logistico.", "/mis-compras");
            notifySellersForSale(order);
        } else if (result.status() == PaymentStatus.REJECTED) {
            order.markRejected();
            for (OrderLine line : order.lines()) {
                products.releaseStock(line.productId(), line.quantity());
            }
            outbox.save(new NotificationOutbox("ORDER", order.id(), "PAYMENT_REJECTED", "{\"orderId\":\"" + order.id() + "\"}"));
            notifications.notify(order.buyerId(), "PAYMENT_REJECTED", "Pago rechazado",
                    "No pudimos aprobar el pago de tu compra. Puedes intentar de nuevo con otro metodo.", "/mis-compras");
        }
        return buildCheckoutResponse(order, payment, result.message());
    }

    private void notifySellersForSale(OrderEntity order) {
        var productById = products.findAllById(order.lines().stream().map(OrderLine::productId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        order.lines().stream()
                .map(line -> productById.get(line.productId()))
                .filter(product -> product != null)
                .collect(Collectors.groupingBy(Product::sellerId, Collectors.counting()))
                .forEach((sellerId, itemCount) -> {
                    notifications.notify(sellerId, "SELLER_SALE", "Vendiste un producto",
                            "Recibiste una compra con " + itemCount + " producto(s). Revisa tu portal de vendedor.",
                            "/vendedor");
                    outbox.save(new NotificationOutbox("ORDER", order.id(), "SELLER_SALE",
                            "{\"orderId\":\"" + order.id() + "\",\"sellerId\":\"" + sellerId + "\"}"));
                });
    }

    private OrderEntity findOrder(UUID orderId) {
        return orders.findWithLines(orderId)
                .orElseThrow(() -> new CheckoutProblem("ORDER_NOT_FOUND", "Pedido no encontrado", HttpStatus.NOT_FOUND));
    }

    private UUID pickCenterId() {
        List<LogisticsCenter> all = centers.findAll();
        if (all.isEmpty()) {
            return null;
        }
        return all.get(ThreadLocalRandom.current().nextInt(all.size())).id();
    }

    private PaymentTransaction latestPaymentFor(UUID orderId) {
        return payments.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new CheckoutProblem("PAYMENT_NOT_FOUND", "No hay pagos registrados para el pedido", HttpStatus.NOT_FOUND));
    }

    private CheckoutResponse buildCheckoutResponse(OrderEntity order, PaymentTransaction payment, String message) {
        var productById = products.findAllById(order.lines().stream().map(OrderLine::productId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        var items = order.lines().stream()
                .map(line -> CheckoutProductResponse.from(line, productById.get(line.productId())))
                .toList();
        return CheckoutResponse.from(order, payment, items, message);
    }

    private void savePendingPayment(UUID orderId, String idempotencyKey, java.math.BigDecimal total, String paymentMethod) {
        try {
            payments.save(new PaymentTransaction(UUID.randomUUID(), orderId, idempotencyKey, total, paymentMethod));
        } catch (DataIntegrityViolationException ex) {
            throw new CheckoutProblem("DUPLICATE_PAYMENT", "La llave de idempotencia ya fue usada", HttpStatus.CONFLICT);
        }
    }

    private String requireKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new CheckoutProblem("IDEMPOTENCY_KEY_REQUIRED", "Enviar header Idempotency-Key", HttpStatus.BAD_REQUEST);
        }
        return idempotencyKey.trim();
    }

    private String resolvePaymentMethod(String rawPaymentMethod) {
        if (rawPaymentMethod == null || rawPaymentMethod.isBlank()) {
            return DEFAULT_PAYMENT_METHOD;
        }
        String paymentMethod = rawPaymentMethod.trim();
        if (paymentMethod.length() > 80) {
            throw new CheckoutProblem("PAYMENT_METHOD_TOO_LONG", "El metodo de pago no puede superar 80 caracteres", HttpStatus.BAD_REQUEST);
        }
        return paymentMethod;
    }
}
