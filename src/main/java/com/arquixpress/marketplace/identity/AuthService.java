package com.arquixpress.marketplace.identity;

import com.arquixpress.marketplace.notifications.NotificationOutbox;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final AppUserRepository users;
    private final PasswordResetEmailService resetEmailService;
    private final NotificationOutboxRepository outbox;

    public AuthService(AppUserRepository users, PasswordResetEmailService resetEmailService,
            NotificationOutboxRepository outbox) {
        this.users = users;
        this.resetEmailService = resetEmailService;
        this.outbox = outbox;
    }

    public AuthUser login(LoginRequest request) {
        AppUser account = users.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new IllegalArgumentException("Credenciales invalidas"));
        if (!passwordMatches(request.password(), account.password())) {
            throw new IllegalArgumentException("Credenciales invalidas");
        }
        if (!isPasswordHash(account.password())) {
            account.setPassword(hashPassword(request.password()));
            users.save(account);
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
                hashPassword(request.password()),
                request.displayName().trim(),
                Set.of(Role.CLIENT));
        applyProfile(created, request.displayName(), request.avatarUrl(), request.phone(), request.address(),
                request.city(), request.documentNumber());
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
                hashPassword(request.password()),
                request.displayName().trim(),
                roles);
        applyProfile(created, request.displayName(), request.avatarUrl(), request.phone(), request.address(),
                request.city(), request.documentNumber());
        return users.save(created).toAuthUser();
    }

    public AuthUser updateProfile(CurrentUser currentUser, ProfileUpdateRequest request) {
        AppUser user = users.findById(currentUser.id())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String email = normalize(request.email());
        users.findByEmail(email)
                .filter(existing -> !existing.id().equals(user.id()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("El correo ya esta registrado");
                });
        applyProfile(user, request.displayName(), request.avatarUrl(), request.phone(), request.address(), request.city(),
                request.documentNumber());
        user.setEmail(email);
        return users.save(user).toAuthUser();
    }

    public PasswordResetResponse requestPasswordReset(PasswordResetRequest request) {
        String email = normalize(request.email());
        AppUser user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES);
        user.setResetToken(token);
        user.setResetTokenExpiresAt(expiresAt);
        users.save(user);
        resetEmailService.sendPasswordReset(user.email(), user.displayName(), token, expiresAt);
        return new PasswordResetResponse(true, expiresAt,
                "Te enviamos un correo con el enlace para restablecer la clave.");
    }

    public AuthUser confirmPasswordReset(PasswordResetConfirmRequest request) {
        if (!StringUtils.hasText(request.newPassword()) || request.newPassword().length() < 4) {
            throw new IllegalArgumentException("La clave debe tener al menos 4 caracteres");
        }
        AppUser user = users.findByResetToken(request.token().trim())
                .orElseThrow(() -> new IllegalArgumentException("Token invalido"));
        if (user.resetTokenExpiresAt() == null || user.resetTokenExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expirado");
        }
        user.setPassword(hashPassword(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        return users.save(user).toAuthUser();
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
        user.setPassword(hashPassword(newPassword));
        AppUser updated = users.save(user);

        outbox.save(new NotificationOutbox(
                "USER", userId, "PASSWORD_CHANGED",
                "{\"userId\":\"" + userId + "\"}"));
        return updated.toAuthUser();
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private void applyProfile(AppUser user, String displayName, String avatarUrl, String phone, String address,
            String city, String documentNumber) {
        if (!StringUtils.hasText(displayName)) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        user.setDisplayName(displayName.trim());
        user.setAvatarUrl(clean(avatarUrl));
        user.setPhone(clean(phone));
        user.setAddress(clean(address));
        user.setCity(clean(city));
        user.setDocumentNumber(clean(documentNumber));
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String hashPassword(String password) {
        return PASSWORD_ENCODER.encode(password);
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(storedPassword)) {
            return false;
        }
        if (isPasswordHash(storedPassword)) {
            return PASSWORD_ENCODER.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    private boolean isPasswordHash(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
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
