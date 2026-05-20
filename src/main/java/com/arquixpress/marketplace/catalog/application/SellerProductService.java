package com.arquixpress.marketplace.catalog.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.catalog.ProductStatus;
import com.arquixpress.marketplace.catalog.api.ModerationRequest;
import com.arquixpress.marketplace.catalog.api.SellerProductRequest;
import com.arquixpress.marketplace.catalog.api.SellerProductResponse;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.notifications.NotificationService;

@Service
public class SellerProductService {

    private final ProductRepository products;
    private final NotificationService notifications;
    private final AppUserRepository users;

    public SellerProductService(ProductRepository products, NotificationService notifications, AppUserRepository users) {
        this.products = products;
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<SellerProductResponse> listMine(UUID sellerId) {
        return products.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(SellerProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SellerProductResponse> listForOperations() {
        return products.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(SellerProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SellerProductResponse detail(CurrentUser user, UUID productId) {
        if (user.hasAny(Role.ADMIN, Role.SUPERADMIN)) {
            return SellerProductResponse.from(products.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado")));
        }
        return SellerProductResponse.from(findMine(user.id(), productId));
    }

    @Transactional
    public SellerProductResponse create(UUID sellerId, SellerProductRequest request) {
        Product product = new Product(
                sellerId,
                request.title(),
                request.description(),
                request.category(),
                request.normalizedImageUrls().isEmpty() ? "" : request.normalizedImageUrls().get(0),
                request.price(),
                request.stockAvailable()
        );
        product.updateDetails(request.title(), request.description(), request.category(), request.normalizedImageUrls(),
                request.price(), request.stockAvailable(), request.normalizedStatus());

        if (request.normalizedStatus().name().equals("INACTIVE")) {
            product.deactivate();
        }

        return SellerProductResponse.from(products.save(product));
    }

    @Transactional
    public SellerProductResponse update(UUID sellerId, UUID productId, SellerProductRequest request) {
        Product product = findMine(sellerId, productId);
        product.updateDetails(
                request.title(),
                request.description(),
                request.category(),
                request.normalizedImageUrls(),
                request.price(),
                request.stockAvailable(),
                request.normalizedStatus()
        );
        return SellerProductResponse.from(product);
    }

    @Transactional
    public SellerProductResponse updateStock(UUID sellerId, UUID productId, int stockAvailable) {
        Product product = findMine(sellerId, productId);
        product.updateStock(stockAvailable);
        return SellerProductResponse.from(product);
    }

    @Transactional
    public SellerProductResponse activate(UUID sellerId, UUID productId) {
        Product product = findMine(sellerId, productId);
        product.activate();
        return SellerProductResponse.from(product);
    }

    @Transactional
    public SellerProductResponse deactivate(UUID sellerId, UUID productId) {
        Product product = findMine(sellerId, productId);
        product.deactivate();
        return SellerProductResponse.from(product);
    }

    @Transactional
    public SellerProductResponse removeByModerator(UUID moderatorId, UUID productId, ModerationRequest request) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        String reason = request == null ? null : request.reason();
        product.removeByModerator(moderatorId, reason);
        notifications.notify(product.sellerId(), "PRODUCT_REMOVED", "Publicacion eliminada",
                "Eliminamos tu publicacion \"" + product.title() + "\" por: " + product.moderationReason()
                        + ". Puedes revisar los detalles y apelar la decision.",
                "/vendedor/publicaciones/" + product.id());
        return SellerProductResponse.from(product);
    }

    @Transactional
    public SellerProductResponse appeal(UUID sellerId, UUID productId, ModerationRequest request) {
        Product product = findMine(sellerId, productId);
        if (product.status() != ProductStatus.INACTIVE || product.moderationReason() == null) {
            throw new IllegalArgumentException("Esta publicacion no tiene una eliminacion apelable");
        }
        product.requestAppeal(request == null ? null : request.reason());
        notifyAdmins("PRODUCT_APPEAL", "Apelacion de publicacion",
                "El vendedor apelo la eliminacion de \"" + product.title() + "\". Revisa la bandeja de operaciones.",
                "/operaciones");
        return SellerProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<SellerProductResponse> listPendingAppeals() {
        return products.findPendingAppeals().stream()
                .map(SellerProductResponse::from)
                .toList();
    }

    @Transactional
    public SellerProductResponse restoreAppeal(UUID productId, ModerationRequest request) {
        Product product = findPendingAppeal(productId);
        product.restoreAfterAppeal(request == null ? null : request.reason());
        notifications.notify(product.sellerId(), "PRODUCT_APPEAL_APPROVED", "Apelacion aprobada",
                product.stockAvailable() == 0
                        ? "Aprobamos tu apelacion de \"" + product.title() + "\", pero sigue oculta porque no tiene stock."
                        : "Aprobamos tu apelacion y restauramos la publicacion \"" + product.title() + "\".",
                "/vendedor/publicaciones/" + product.id());
        return SellerProductResponse.from(product);
    }

    @Transactional
    public SellerProductResponse rejectAppeal(UUID productId, ModerationRequest request) {
        Product product = findPendingAppeal(productId);
        product.rejectAppeal(request == null ? null : request.reason());
        notifications.notify(product.sellerId(), "PRODUCT_APPEAL_REJECTED", "Apelacion rechazada",
                "Revisamos tu apelacion de \"" + product.title() + "\" y mantuvimos la eliminacion. Motivo: "
                        + product.appealResolutionNote(),
                "/vendedor/publicaciones/" + product.id());
        return SellerProductResponse.from(product);
    }

    private Product findMine(UUID sellerId, UUID productId) {
        return products.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para este vendedor"));
    }

    private Product findPendingAppeal(UUID productId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        if (product.moderationReason() == null || product.appealRequestedAt() == null || product.appealResolvedAt() != null) {
            throw new IllegalArgumentException("Esta publicacion no tiene una apelacion pendiente");
        }
        return product;
    }

    private void notifyAdmins(String type, String title, String body, String actionUrl) {
        users.findAll().stream()
                .filter(user -> {
                    var roles = user.roleSet();
                    return roles.contains(Role.ADMIN) || roles.contains(Role.SUPERADMIN);
                })
                .forEach(user -> notifications.notify(user.id(), type, title, body, actionUrl));
    }
}
