package com.arquixpress.marketplace.identity;

import com.arquixpress.marketplace.notifications.NotificationOutbox;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final NotificationOutboxRepository outbox;

    public AuthService(AppUserRepository users, NotificationOutboxRepository outbox) {
        this.users  = users;
        this.outbox = outbox;
    }

    public AuthUser login(LoginRequest request) {
        AppUser account = users.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new IllegalArgumentException("Credenciales invalidas"));
        if (!account.password().equals(request.password())) {
            throw new IllegalArgumentException("Credenciales invalidas");
        }
        return account.toAuthUser();
    }

    public AuthUser register(RegisterRequest request) {
        String email = normalize(request.email());
        if (!StringUtils.hasText(request.password()) || request.password().length() < 4) {
            throw new IllegalArgumentException("La clave debe tener al menos 4 caracteres");
        }
        if (users.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("El correo ya esta registrado");
        }
        AppUser created = AppUser.create(
                UUID.randomUUID(),
                email,
                request.password(),
                request.displayName().trim(),
                Set.of(Role.CLIENT));
        AuthUser saved = users.save(created).toAuthUser();
        
        outbox.save(new NotificationOutbox(
                "USER", created.id(), "USER_REGISTERED",
                "{\"userId\":\"" + created.id() + "\"}"));
        return saved;
    }

    public AuthUser createOperator(RegisterRequest request, CurrentUser currentUser) {
        if (!currentUser.hasAny(Role.SUPERADMIN)) {
            throw new AccessDeniedProblem("Solo el superadministrador puede crear operadores");
        }
        String email = normalize(request.email());
        if (!StringUtils.hasText(request.password()) || request.password().length() < 4) {
            throw new IllegalArgumentException("La clave debe tener al menos 4 caracteres");
        }
        if (users.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("El correo ya esta registrado");
        }
        Set<Role> roles = parseOperatorRoles(request.roles());
        AppUser created = AppUser.create(
                UUID.randomUUID(),
                email,
                request.password(),
                request.displayName().trim(),
                roles);
        return users.save(created).toAuthUser();
    }

    public AuthUser findById(UUID id) {
        return users.findById(id)
                .map(AppUser::toAuthUser)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    public List<AuthUser> listDemoUsers() {
        return users.findAll().stream().map(AppUser::toAuthUser).toList();
    }

    public AuthUser changePassword(UUID userId, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 4) {
            throw new IllegalArgumentException("La clave debe tener al menos 4 caracteres");
        }
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        AppUser updated = AppUser.create(
                user.id(), user.email(), newPassword,
                user.displayName(), user.roleSet());
        users.save(updated);

        outbox.save(new NotificationOutbox(
                "USER", userId, "PASSWORD_CHANGED",
                "{\"userId\":\"" + userId + "\"}"));
        return updated.toAuthUser();
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private Set<Role> parseOperatorRoles(String roles) {
        if (!StringUtils.hasText(roles)) {
            return Set.of(Role.LOGISTICS);
        }
        Set<Role> parsed = Stream.of(roles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .map(Role::valueOf)
                .collect(java.util.stream.Collectors.toSet());
        parsed.remove(Role.CLIENT);
        parsed.remove(Role.SUPERADMIN);
        if (parsed.isEmpty()) {
            return Set.of(Role.LOGISTICS);
        }
        return parsed;
    }
}