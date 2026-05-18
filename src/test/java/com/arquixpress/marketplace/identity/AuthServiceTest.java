package com.arquixpress.marketplace.identity;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arquixpress.marketplace.notifications.NotificationOutbox;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;

/*
RF-01: registro de usuarios
RF-02: inicio de sesión
RF-03: recuperación/cambio de contraseña
RF-04: gestión de perfil
*/

class AuthServiceTest {

    private AppUserRepository users;
    private PasswordResetEmailService resetEmailService;
    private NotificationOutboxRepository outbox;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        resetEmailService = mock(PasswordResetEmailService.class);
        outbox = mock(NotificationOutboxRepository.class);

        authService = new AuthService(users, resetEmailService, outbox);
    }

    @Test
    void register_shouldCreateClientUserWhenEmailIsNew() {
        RegisterRequest request = new RegisterRequest(
                "  CLIENTE@MAIL.COM  ",
                "1234",
                " Juan Cliente ",
                "3001234567",
                "Calle 123",
                "Bogota",
                "123456",
                null,
                null
        );

        when(users.findByEmail("cliente@mail.com")).thenReturn(Optional.empty());

        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthUser result = authService.register(request);

        assertEquals("cliente@mail.com", result.email());
        assertEquals("Juan Cliente", result.displayName());
        assertTrue(result.roles().contains(Role.CLIENT));

        verify(users).save(any(AppUser.class));
        verify(outbox).save(any(NotificationOutbox.class));
    }

    @Test
    void register_shouldThrowExceptionWhenEmailAlreadyExists() {
        AppUser existing = AppUser.create(
                UUID.randomUUID(),
                "cliente@mail.com",
                "1234",
                "Cliente Existente",
                Set.of(Role.CLIENT)
        );

        RegisterRequest request = new RegisterRequest(
                "CLIENTE@MAIL.COM",
                "1234",
                "Otro Cliente",
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(users.findByEmail("cliente@mail.com")).thenReturn(Optional.of(existing));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals("El correo ya esta registrado", exception.getMessage());

        verify(users, never()).save(any(AppUser.class));
    }

    @Test
    void login_shouldReturnUserWhenCredentialsAreValid() {
        AppUser user = AppUser.create(
                UUID.randomUUID(),
                "cliente@mail.com",
                "1234",
                "Cliente Demo",
                Set.of(Role.CLIENT)
        );

        when(users.findByEmail("cliente@mail.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest(" CLIENTE@MAIL.COM ", "1234");

        AuthUser result = authService.login(request);

        assertEquals("cliente@mail.com", result.email());
        assertEquals("Cliente Demo", result.displayName());
        assertTrue(result.roles().contains(Role.CLIENT));
    }

    @Test
    void login_shouldThrowExceptionWhenPasswordIsInvalid() {
        AppUser user = AppUser.create(
                UUID.randomUUID(),
                "cliente@mail.com",
                "1234",
                "Cliente Demo",
                Set.of(Role.CLIENT)
        );

        when(users.findByEmail("cliente@mail.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("cliente@mail.com", "mala");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals("Credenciales invalidas", exception.getMessage());
    }

    @Test
    void requestPasswordReset_shouldGenerateTokenAndSendEmail() {
        AppUser user = AppUser.create(
                UUID.randomUUID(),
                "cliente@mail.com",
                "1234",
                "Cliente Demo",
                Set.of(Role.CLIENT)
        );

        when(users.findByEmail("cliente@mail.com")).thenReturn(Optional.of(user));
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordResetResponse response = authService.requestPasswordReset(
                new PasswordResetRequest(" cliente@mail.com ")
        );

        assertTrue(response.accepted());
        assertEquals("Te enviamos un correo con el enlace para restablecer la clave.", response.message());
        verify(users).save(user);
        verify(resetEmailService).sendPasswordReset(
                eq("cliente@mail.com"),
                eq("Cliente Demo"),
                any(String.class),
                any(Instant.class)
        );
    }

    @Test
    void confirmPasswordReset_shouldChangePasswordWhenTokenIsValid() {
        AppUser user = AppUser.create(
                UUID.randomUUID(),
                "cliente@mail.com",
                "1234",
                "Cliente Demo",
                Set.of(Role.CLIENT)
        );

        user.setResetToken("token123");
        user.setResetTokenExpiresAt(Instant.now().plusSeconds(600));

        when(users.findByResetToken("token123")).thenReturn(Optional.of(user));
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthUser result = authService.confirmPasswordReset(
                new PasswordResetConfirmRequest(" token123 ", "nueva123")
        );

        assertEquals("cliente@mail.com", result.email());
        assertEquals("nueva123", user.password());
        verify(users).save(user);
    }

    @Test
    void updateProfile_shouldUpdateUserDataWhenEmailIsAvailable() {
        UUID userId = UUID.randomUUID();

        AppUser user = AppUser.create(
                userId,
                "cliente@mail.com",
                "1234",
                "Cliente Demo",
                Set.of(Role.CLIENT)
        );

        CurrentUser currentUser = new CurrentUser(userId, Set.of(Role.CLIENT));

        ProfileUpdateRequest request = new ProfileUpdateRequest(
                "Nuevo Nombre",
                "nuevo@mail.com",
                null,
                "3009999999",
                "Nueva Direccion",
                "Bogota",
                "999"
        );

        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(users.findByEmail("nuevo@mail.com")).thenReturn(Optional.empty());
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthUser result = authService.updateProfile(currentUser, request);

        assertEquals("nuevo@mail.com", result.email());
        assertEquals("Nuevo Nombre", result.displayName());
        assertEquals("3009999999", result.phone());
        assertEquals("Nueva Direccion", result.address());
    }
}