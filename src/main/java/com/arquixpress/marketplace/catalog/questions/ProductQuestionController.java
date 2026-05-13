package com.arquixpress.marketplace.catalog.questions;

import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProductQuestionController {
    private final ProductQuestionService questionService;
    private final RoleGuard roles;

    public ProductQuestionController(ProductQuestionService questionService, RoleGuard roles) {
        this.questionService = questionService;
        this.roles = roles;
    }

    @GetMapping("/products/{productId}/questions")
    public List<ProductQuestionResponse> listForProduct(@PathVariable UUID productId) {
        return questionService.listForProduct(productId);
    }

    @PostMapping("/products/{productId}/questions")
    public ProductQuestionResponse ask(@PathVariable UUID productId,
            @Valid @RequestBody ProductQuestionRequest request, HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.CLIENT, Role.SELLER, Role.ADMIN, Role.SUPERADMIN);
        return questionService.ask(productId, user, request);
    }

    @GetMapping("/seller/questions")
    public List<ProductQuestionResponse> listForSeller(HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.SELLER, Role.ADMIN, Role.SUPERADMIN);
        return questionService.listForSeller(user.id());
    }

    @PatchMapping("/seller/questions/{questionId}/answer")
    public ProductQuestionResponse answer(@PathVariable UUID questionId,
            @Valid @RequestBody ProductAnswerRequest request, HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.SELLER, Role.ADMIN, Role.SUPERADMIN);
        return questionService.answer(questionId, user, request);
    }
}
