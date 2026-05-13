package com.arquixpress.marketplace.catalog.questions;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.notifications.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductQuestionService {
    private final ProductQuestionRepository questions;
    private final ProductRepository products;
    private final AppUserRepository users;
    private final NotificationService notifications;

    public ProductQuestionService(ProductQuestionRepository questions, ProductRepository products,
            AppUserRepository users, NotificationService notifications) {
        this.questions = questions;
        this.products = products;
        this.users = users;
        this.notifications = notifications;
    }

    public List<ProductQuestionResponse> listForProduct(UUID productId) {
        return questions.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(ProductQuestionResponse::from)
                .toList();
    }

    public List<ProductQuestionResponse> listForSeller(UUID sellerId) {
        return questions.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(ProductQuestionResponse::from)
                .toList();
    }

    @Transactional
    public ProductQuestionResponse ask(UUID productId, CurrentUser buyer, ProductQuestionRequest request) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        if (!StringUtils.hasText(request.question())) {
            throw new IllegalArgumentException("La pregunta es obligatoria");
        }
        ProductQuestion question = questions.save(new ProductQuestion(product.id(), buyer.id(), product.sellerId(),
                request.question()));
        String buyerName = users.findById(buyer.id()).map(user -> user.displayName()).orElse("Un comprador");
        notifications.notify(product.sellerId(), "PRODUCT_QUESTION", "Nueva pregunta sobre " + product.title(),
                buyerName + " pregunto: " + request.question().trim(), "/vendedor");
        return ProductQuestionResponse.from(question);
    }

    @Transactional
    public ProductQuestionResponse answer(UUID questionId, CurrentUser seller, ProductAnswerRequest request) {
        ProductQuestion question = questions.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada"));
        if (!seller.hasAny(Role.ADMIN, Role.SUPERADMIN) && !question.sellerId().equals(seller.id())) {
            throw new IllegalArgumentException("Solo el vendedor puede responder esta pregunta");
        }
        question.answer(seller.id(), request.answer());
        Product product = products.findById(question.productId()).orElse(null);
        String productTitle = product == null ? "tu producto" : product.title();
        notifications.notify(question.buyerId(), "PRODUCT_ANSWER", "Respondieron tu pregunta",
                "El vendedor respondio tu pregunta sobre " + productTitle + ".", "/producto/" + question.productId());
        return ProductQuestionResponse.from(question);
    }
}
