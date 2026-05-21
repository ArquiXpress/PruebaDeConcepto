package com.arquixpress.marketplace.admin;

import com.arquixpress.marketplace.identity.AppUser;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
 * RF-33: Bloqueo de usuarios administrativamente
 * RF-43: Registro de acciones administrativas
 */
class AdminUserServiceTest {

    private AppUserRepository userRepository;
    private AdminUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(AppUserRepository.class);
        service = new AdminUserService(userRepository);
    }

    // RF-33: Bloqueo usuarios / gestión de roles

    @Test
    void updateUserRoles_shouldAssignNewRolesToUser() {
        UUID userId = UUID.randomUUID();
        AppUser user = AppUser.create(userId, "user@mail.com", "pass", "Usuario", Set.of(Role.CLIENT));
        CurrentUser admin = new CurrentUser(UUID.randomUUID(), Set.of(Role.ADMIN));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse result = service.updateUserRoles(userId, Set.of("SELLER"), admin);

        assertTrue(result.roles().contains("SELLER"));
        verify(userRepository).save(user);
    }

    @Test
    void updateUserRoles_shouldThrowWhenTargetIsSuperAdmin() {
        UUID userId = UUID.randomUUID();
        AppUser user = AppUser.create(userId, "super@mail.com", "pass", "Superadmin", Set.of(Role.SUPERADMIN));
        CurrentUser admin = new CurrentUser(UUID.randomUUID(), Set.of(Role.ADMIN));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUserRoles(userId, Set.of("CLIENT"), admin));
        assertEquals("La cuenta superadmin no se puede modificar desde el panel", ex.getMessage());
    }

    @Test
    void updateUserRoles_shouldThrowWhenAdminTriesToModifyAnotherAdmin() {
        UUID userId = UUID.randomUUID();
        AppUser user = AppUser.create(userId, "admin2@mail.com", "pass", "Admin2", Set.of(Role.ADMIN));
        CurrentUser regularAdmin = new CurrentUser(UUID.randomUUID(), Set.of(Role.ADMIN));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUserRoles(userId, Set.of("CLIENT"), regularAdmin));
        assertEquals("Solo el superadministrador puede modificar cuentas administradoras", ex.getMessage());
    }

    @Test
    void updateUserRoles_shouldThrowWhenRolesIsEmpty() {
        UUID userId = UUID.randomUUID();
        AppUser user = AppUser.create(userId, "user@mail.com", "pass", "Usuario", Set.of(Role.CLIENT));
        CurrentUser admin = new CurrentUser(UUID.randomUUID(), Set.of(Role.ADMIN));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUserRoles(userId, Set.of(), admin));
        assertEquals("Selecciona al menos un rol", ex.getMessage());
    }

    @Test
    void updateUserRoles_shouldThrowWhenTryingToAssignSuperAdmin() {
        UUID userId = UUID.randomUUID();
        AppUser user = AppUser.create(userId, "user@mail.com", "pass", "Usuario", Set.of(Role.CLIENT));
        CurrentUser admin = new CurrentUser(UUID.randomUUID(), Set.of(Role.ADMIN));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUserRoles(userId, Set.of("SUPERADMIN"), admin));
        assertEquals("El rol SUPERADMIN no se puede asignar desde el panel", ex.getMessage());
    }

    @Test
    void updateUserRoles_shouldThrowWhenNonSuperAdminTriesToAssignAdminRole() {
        UUID userId = UUID.randomUUID();
        AppUser user = AppUser.create(userId, "user@mail.com", "pass", "Usuario", Set.of(Role.CLIENT));
        CurrentUser regularAdmin = new CurrentUser(UUID.randomUUID(), Set.of(Role.ADMIN));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUserRoles(userId, Set.of("ADMIN"), regularAdmin));
        assertEquals("Solo el superadministrador puede asignar el rol ADMIN", ex.getMessage());
    }

    @Test
    void updateUserRoles_shouldAllowSuperAdminToAssignAdminRole() {
        UUID userId = UUID.randomUUID();
        AppUser user = AppUser.create(userId, "user@mail.com", "pass", "Usuario", Set.of(Role.CLIENT));
        CurrentUser superAdmin = new CurrentUser(UUID.randomUUID(), Set.of(Role.SUPERADMIN));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse result = service.updateUserRoles(userId, Set.of("ADMIN"), superAdmin);

        assertTrue(result.roles().contains("ADMIN"));
    }

    @Test
    void makeAdmin_shouldThrowWhenUserNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.makeAdmin(userId));
    }

    @Test
    void makeAdmin_shouldAddAdminRoleToUser() {
        UUID userId = UUID.randomUUID();
        AppUser user = AppUser.create(userId, "user@mail.com", "pass", "Usuario", Set.of(Role.CLIENT));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse result = service.makeAdmin(userId);

        assertTrue(result.roles().contains("ADMIN"));
    }

    // RF-43: Registro de acciones administrativas (via persistencia)

    @Test
    void updateUserRoles_shouldPersistChangesViaRepository() {
        UUID userId = UUID.randomUUID();
        AppUser user = AppUser.create(userId, "user@mail.com", "pass", "Usuario", Set.of(Role.CLIENT));
        CurrentUser admin = new CurrentUser(UUID.randomUUID(), Set.of(Role.ADMIN));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateUserRoles(userId, Set.of("SELLER"), admin);

        verify(userRepository, times(1)).save(user);
    }
}
