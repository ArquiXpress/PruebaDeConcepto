package com.arquixpress.marketplace.promotions;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.catalog.ProductSummary;
import com.arquixpress.marketplace.identity.AppUser;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.notifications.NotificationService;
import com.arquixpress.marketplace.orders.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionService {
    private final MarketingCouponRepository coupons;
    private final SellerOfferRequestRepository offers;
    private final AppUserRepository users;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final NotificationService notifications;

    public PromotionService(MarketingCouponRepository coupons, SellerOfferRequestRepository offers,
            AppUserRepository users, ProductRepository products, OrderRepository orders, NotificationService notifications) {
        this.coupons = coupons;
        this.offers = offers;
        this.users = users;
        this.products = products;
        this.orders = orders;
        this.notifications = notifications;
    }

    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request, CurrentUser admin) {
        MarketingCoupon coupon = coupons.save(new MarketingCoupon(request.code(), request.title(), request.description(),
                request.discountPercent(), request.targetType(), request.targetValue(), admin.id()));
        recipientIds(request).forEach(userId -> notifications.notify(userId, "COUPON",
                "Cupon " + coupon.code(),
                coupon.description() + " Usa el codigo " + coupon.code() + " para recibir "
                        + coupon.discountPercent() + "% de descuento.",
                "/notificaciones"));
        return CouponResponse.from(coupon);
    }

    public List<CouponResponse> listCoupons() {
        return coupons.findTop30ByOrderByCreatedAtDesc().stream().map(CouponResponse::from).toList();
    }

    @Transactional
    public OfferRequestResponse createOffer(OfferCreateRequest request, CurrentUser admin) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException("La fecha final de la oferta debe ser posterior al inicio");
        }
        List<Product> selected = products.findAllById(request.productIds());
        if (selected.isEmpty() || selected.stream().anyMatch(product -> !product.sellerId().equals(request.sellerId()))) {
            throw new IllegalArgumentException("Selecciona productos que pertenezcan al vendedor indicado");
        }
        SellerOfferRequest offer = offers.save(new SellerOfferRequest(request.sellerId(), request.title(),
                request.message(), request.discountPercent(),
                selected.stream().map(Product::id).collect(Collectors.toCollection(LinkedHashSet::new)), admin.id(),
                request.startsAt(), request.endsAt()));
        notifications.notify(request.sellerId(), "OFFER_REQUEST", request.title(),
                request.message() + " Productos incluidos: "
                        + selected.stream().map(Product::title).collect(Collectors.joining(", "))
                        + ". Descuento propuesto: " + request.discountPercent() + "%. Vigencia: "
                        + request.startsAt() + " a " + request.endsAt() + ".",
                "/vendedor");
        return enrich(offer);
    }

    public List<OfferRequestResponse> listOffersForAdmin() {
        return offers.findTop50ByOrderByCreatedAtDesc().stream().map(this::enrich).toList();
    }

    public List<OfferRequestResponse> listOffersForSeller(UUID sellerId) {
        return offers.findBySellerIdOrderByCreatedAtDesc(sellerId).stream().map(this::enrich).toList();
    }

    public List<ProductSummary> activeOfferProducts() {
        Map<UUID, Integer> discountByProduct = new java.util.HashMap<>();
        Map<UUID, Instant> endsByProduct = new java.util.HashMap<>();
        for (SellerOfferRequest offer : offers.findActiveAccepted(Instant.now())) {
            for (UUID productId : offer.productIds()) {
                int current = discountByProduct.getOrDefault(productId, 0);
                if (offer.discountPercent() >= current) {
                    discountByProduct.put(productId, offer.discountPercent());
                    endsByProduct.put(productId, offer.endsAt());
                }
            }
        }
        if (discountByProduct.isEmpty()) {
            return List.of();
        }
        return products.findAllById(discountByProduct.keySet()).stream()
                .map(product -> ProductSummary.discounted(product, discountByProduct.get(product.id()), endsByProduct.get(product.id())))
                .toList();
    }

    @Transactional
    public OfferRequestResponse decideOffer(UUID offerId, UUID sellerId, boolean accepted) {
        SellerOfferRequest offer = offers.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud de oferta no encontrada"));
        if (!offer.sellerId().equals(sellerId)) {
            throw new IllegalArgumentException("No puedes responder ofertas de otro vendedor");
        }
        if (offer.status() != OfferRequestStatus.PENDING) {
            throw new IllegalArgumentException("Esta oferta ya fue respondida");
        }
        if (accepted) {
            offer.accept();
        } else {
            offer.reject();
        }
        return enrich(offer);
    }

    private List<UUID> recipientIds(CouponCreateRequest request) {
        if (request.targetType() == CouponTargetType.HIGH_VALUE_BUYERS) {
            BigDecimal minimum = parseMoney(request.targetValue(), new BigDecimal("1000000"));
            return orders.findBuyerIdsWithPaidTotalAtLeast(minimum);
        }
        if (request.targetType() == CouponTargetType.CATEGORY_BUYERS && request.targetValue() != null) {
            List<UUID> productIds = products.findByCategoryIgnoreCase(request.targetValue()).stream().map(Product::id).toList();
            return productIds.isEmpty() ? List.of() : orders.findBuyerIdsByPaidProductIds(productIds);
        }
        return users.findAll().stream()
                .filter(user -> user.roleSet().contains(Role.CLIENT))
                .map(AppUser::id)
                .toList();
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

    private OfferRequestResponse enrich(SellerOfferRequest offer) {
        AppUser seller = users.findById(offer.sellerId()).orElse(null);
        Map<UUID, Product> productById = products.findAllById(offer.productIds()).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        return OfferRequestResponse.from(offer, seller, productById);
    }
}
