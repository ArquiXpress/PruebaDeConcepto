package com.arquixpress.marketplace.identity;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(authService.login(request));
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return LoginResponse.from(authService.register(request));
    }

    @PutMapping("/profile")
    public LoginResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request, HttpServletRequest http) {
        return LoginResponse.from(authService.updateProfile(CurrentUser.from(http), request));
    }

    @PostMapping("/password-reset")
    public PasswordResetResponse requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/password-reset/confirm")
    public LoginResponse confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        return LoginResponse.from(authService.confirmPasswordReset(request));
    }

    @PostMapping("/operators")
    public LoginResponse createOperator(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        return LoginResponse.from(authService.createOperator(request, CurrentUser.from(http)));
    }

    @GetMapping("/users")
    public List<LoginResponse> users() {
        return authService.listDemoUsers().stream().map(LoginResponse::from).toList();
    }

    @GetMapping("/users/{id}")
    public LoginResponse user(@PathVariable UUID id) {
        return LoginResponse.from(authService.findById(id));
    }
}
