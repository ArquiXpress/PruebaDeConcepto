package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.catalog.ProductStatus;
import com.arquixpress.marketplace.identity.AppUser;
import com.arquixpress.marketplace.identity.AppUserRepository;
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
import com.arquixpress.marketplace.promotions.CouponRedemption;
import com.arquixpress.marketplace.promotions.CouponRedemptionRepository;
import com.arquixpress.marketplace.promotions.CouponTargetType;
import com.arquixpress.marketplace.promotions.MarketingCoupon;
import com.arquixpress.marketplace.promotions.MarketingCouponRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.text.Normalizer;
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
    private final AppUserRepository users;
    private final MarketingCouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final TransactionTemplate tx;

    public CheckoutService(ProductRepository products, OrderRepository orders, PaymentTransactionRepository payments,
            PaymentGatewayClient paymentGateway, NotificationOutboxRepository outbox,
            NotificationService notifications, LogisticsCenterRepository centers, AppUserRepository users,
            MarketingCouponRepository coupons, CouponRedemptionRepository redemptions, TransactionTemplate tx) {
        this.products = products;
        this.orders = orders;
        this.payments = payments;
        this.paymentGateway = paymentGateway;
        this.outbox = outbox;
        this.notifications = notifications;
        this.centers = centers;
        this.users = users;
        this.coupons = coupons;
        this.redemptions = redemptions;
        this.tx = tx;
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
        OrderEntity order = findOrder(orderId);
        return enrichedOrderResponse(order);
    }

    public java.util.List<OrderResponse> getOrdersForBuyer(UUID buyerId) {
        return orders.findByBuyerWithLines(buyerId).stream()
                .map(this::enrichedOrderResponse)
                .toList();
    }

    public OrderResponse updateShipment(UUID orderId, ShipmentStatus next) {
        return tx.execute(status -> {
            OrderEntity order = findOrder(orderId);
            order.updateShipment(next);
            return enrichedOrderResponse(order);
        });
    }

    public List<OrderResponse> listShipmentsByCenter(UUID centerId) {
        List<OrderEntity> list = centerId == null ? orders.findAllPaid() : orders.findPaidByCenter(centerId);
        return list.stream().map(this::enrichedOrderResponse).toList();
    }

    private PendingCheckout createPendingOrder(UUID buyerId, CheckoutRequest request, String idempotencyKey, String paymentMethod) {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), buyerId);
        List<Product> orderProducts = new java.util.ArrayList<>();
        for (CheckoutItemRequest item : request.items()) {
            Product product = products.findById(item.productId())
                    .orElseThrow(() -> new CheckoutProblem("PRODUCT_NOT_FOUND", "Producto no encontrado", HttpStatus.NOT_FOUND));
            if (product.status() != ProductStatus.ACTIVE || product.stockAvailable() < item.quantity()) {
                throw new CheckoutProblem("INSUFFICIENT_STOCK", "Stock insuficiente para el producto " + item.productId(), HttpStatus.CONFLICT);
            }
            orderProducts.add(product);
            int reserved = products.reserveStock(item.productId(), item.quantity());
            if (reserved != 1) {
                throw new CheckoutProblem("INSUFFICIENT_STOCK", "Stock insuficiente para el producto " + item.productId(), HttpStatus.CONFLICT);
            }
            order.addLine(product.id(), item.quantity(), product.price());
        }
        AppUser buyer = users.findById(buyerId).orElse(null);
        String shippingAddress = firstText(request.shippingAddress(), buyer == null ? null : buyer.address());
        String shippingCity = firstText(request.shippingCity(), buyer == null ? null : buyer.city());
        if (shippingAddress == null || shippingCity == null) {
            throw new CheckoutProblem("SHIPPING_ADDRESS_REQUIRED", "Selecciona o ingresa una direccion y ciudad de envio", HttpStatus.BAD_REQUEST);
        }
        shippingCity = canonicalCity(shippingCity);
        UUID centerId = pickCenterId(shippingCity);
        if (centerId != null) {
            order.assignCenter(centerId, null);
        }
        order.setShipping(shippingAddress, shippingCity, calculateShippingCost(orderProducts, shippingCity));
        AppliedCoupon appliedCoupon = applyCouponIfPresent(order, orderProducts, request.couponCode());
        orders.save(order);
        savePendingPayment(order.id(), idempotencyKey, order.total(), paymentMethod);
        return new PendingCheckout(order.id(), order.total());
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
            registerCouponRedemption(order);
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

    private UUID pickCenterId(String shippingCity) {
        List<LogisticsCenter> all = centers.findAll();
        if (all.isEmpty()) {
            return null;
        }
        String normalizedShippingCity = normalizeCity(shippingCity);
        for (LogisticsCenter center : all) {
            if (normalizeCity(center.city()).equals(normalizedShippingCity)) {
                return center.id();
            }
        }
        return all.get(ThreadLocalRandom.current().nextInt(all.size())).id();
    }

    private AppliedCoupon applyCouponIfPresent(OrderEntity order, List<Product> orderProducts, String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return null;
        }
        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        MarketingCoupon coupon = coupons.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new CheckoutProblem("COUPON_NOT_FOUND", "Cupon no encontrado", HttpStatus.BAD_REQUEST));
        if (redemptions.existsByCouponIdAndBuyerId(coupon.id(), order.buyerId())) {
            throw new CheckoutProblem("COUPON_ALREADY_USED", "Este cupon ya fue usado por tu cuenta", HttpStatus.CONFLICT);
        }
        BigDecimal eligibleSubtotal = eligibleSubtotal(coupon, order, orderProducts);
        if (eligibleSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CheckoutProblem("COUPON_NOT_APPLICABLE", "El cupon no aplica a los productos de este checkout", HttpStatus.BAD_REQUEST);
        }
        BigDecimal discount = eligibleSubtotal
                .multiply(BigDecimal.valueOf(coupon.discountPercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        order.applyCoupon(coupon.code(), discount);
        return new AppliedCoupon(coupon.id(), discount);
    }

    private BigDecimal eligibleSubtotal(MarketingCoupon coupon, OrderEntity order, List<Product> orderProducts) {
        Map<UUID, Product> productById = orderProducts.stream().collect(Collectors.toMap(Product::id, Function.identity()));
        if (coupon.targetType() == CouponTargetType.HIGH_VALUE_BUYERS) {
            BigDecimal minimum = parseMoney(coupon.targetValue(), new BigDecimal("1000000"));
            BigDecimal subtotal = order.lines().stream()
                    .map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return subtotal.compareTo(minimum) >= 0 ? subtotal : BigDecimal.ZERO;
        }
        return order.lines().stream()
                .filter(line -> appliesToProduct(coupon, productById.get(line.productId())))
                .map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean appliesToProduct(MarketingCoupon coupon, Product product) {
        if (product == null) {
            return false;
        }
        if (coupon.targetType() == CouponTargetType.ALL_CLIENTS) {
            return true;
        }
        if (coupon.targetType() == CouponTargetType.CATEGORY_BUYERS) {
            return coupon.targetValue() != null && product.category().equalsIgnoreCase(coupon.targetValue());
        }
        return true;
    }

    private void registerCouponRedemption(OrderEntity order) {
        if (order.couponCode() == null || order.couponCode().isBlank() || order.discountTotal().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        MarketingCoupon coupon = coupons.findByCodeIgnoreCase(order.couponCode()).orElse(null);
        if (coupon == null) {
            return;
        }
        redemptions.save(new CouponRedemption(coupon.id(), order.buyerId(), order.id(), order.discountTotal()));
    }

    private BigDecimal parseMoney(String value, BigDecimal fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
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

    private java.util.Map<UUID, Product> productMapFor(OrderEntity order) {
        return products.findAllById(order.lines().stream().map(OrderLine::productId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
    }

    private OrderResponse enrichedOrderResponse(OrderEntity order) {
        var productById = productMapFor(order);
        var sellerIds = productById.values().stream().map(Product::sellerId).distinct().toList();
        var sellerById = users.findAllById(sellerIds).stream()
                .collect(Collectors.toMap(AppUser::id, Function.identity()));
        AppUser buyer = users.findById(order.buyerId()).orElse(null);
        return OrderResponse.from(order, productById, sellerById, buyer);
    }

    private java.math.BigDecimal calculateShippingCost(List<Product> orderProducts, String shippingCity) {
        if (orderProducts.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        var sellerIds = orderProducts.stream().map(Product::sellerId).distinct().toList();
        java.math.BigDecimal totalShipping = java.math.BigDecimal.ZERO;
        String normalizedShippingCity = normalizeCity(shippingCity);
        for (AppUser seller : users.findAllById(sellerIds)) {
            boolean sameCity = seller.city() != null && !normalizedShippingCity.isBlank()
                    && normalizeCity(seller.city()).equals(normalizedShippingCity);
            totalShipping = totalShipping.add(sameCity ? java.math.BigDecimal.valueOf(8000) : java.math.BigDecimal.valueOf(18000));
        }
        return totalShipping;
    }

    private String canonicalCity(String value) {
        String normalized = normalizeCity(value);
        return switch (normalized) {
            case "bogota" -> "Bogota";
            case "medellin" -> "Medellin";
            case "cali" -> "Cali";
            case "barranquilla" -> "Barranquilla";
            case "cartagena" -> "Cartagena";
            case "bucaramanga" -> "Bucaramanga";
            case "pereira" -> "Pereira";
            case "manizales" -> "Manizales";
            default -> value.trim();
        };
    }

    private String normalizeCity(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private String firstText(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return fallback == null || fallback.isBlank() ? null : fallback.trim();
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

    private record AppliedCoupon(UUID couponId, BigDecimal discount) {}
}
