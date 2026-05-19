package com.arquixpress.marketplace.admin;

import com.arquixpress.marketplace.identity.AppUser;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.UserStatus;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private final AppUserRepository userRepository;

    public AdminUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(user -> new AdminUserResponse(
                user.id(),
                user.email(),
                user.displayName(),
                user.roles(),
                user.status()
            ))
            .collect(Collectors.toList());
    }

    @Transactional
    public AdminUserResponse makeAdmin(UUID userId) {
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Set<Role> currentRoles = user.roleSet();
        if (currentRoles.contains(Role.SUPERADMIN)) {
            throw new IllegalArgumentException("La cuenta superadmin no se puede modificar desde el panel");
        }

        currentRoles.add(Role.ADMIN);

        String rolesStr = currentRoles.stream()
            .map(Enum::name)
            .sorted()
            .collect(Collectors.joining(","));

        user.setRoles(rolesStr);
        userRepository.save(user);

        return new AdminUserResponse(
            user.id(),
            user.email(),
            user.displayName(),
            user.roles(),
            user.status()
        );
    }

    @Transactional
    public AdminUserResponse updateUserRoles(UUID userId, Set<String> roleNames, CurrentUser currentUser) {
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Set<Role> currentTargetRoles = user.roleSet();
        boolean actingAsSuperAdmin = currentUser.hasAny(Role.SUPERADMIN);

        if (currentTargetRoles.contains(Role.SUPERADMIN)) {
            throw new IllegalArgumentException("La cuenta superadmin no se puede modificar desde el panel");
        }
        if (!actingAsSuperAdmin && currentTargetRoles.contains(Role.ADMIN)) {
            throw new IllegalArgumentException("Solo el superadministrador puede modificar cuentas administradoras");
        }
        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un rol");
        }

        Set<Role> newRoles = roleNames.stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(value -> value.toUpperCase(Locale.ROOT))
            .map(Role::valueOf)
            .collect(Collectors.toSet());
        if (newRoles.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un rol");
        }
        if (newRoles.contains(Role.SUPERADMIN)) {
            throw new IllegalArgumentException("El rol SUPERADMIN no se puede asignar desde el panel");
        }
        if (!actingAsSuperAdmin && newRoles.contains(Role.ADMIN)) {
            throw new IllegalArgumentException("Solo el superadministrador puede asignar el rol ADMIN");
        }

        String rolesStr = newRoles.stream()
            .map(Enum::name)
            .sorted()
            .collect(Collectors.joining(","));

        user.setRoles(rolesStr);
        userRepository.save(user);

        return new AdminUserResponse(
            user.id(),
            user.email(),
            user.displayName(),
            user.roles(),
            user.status()
        );
    }

    @Transactional
    public AdminUserResponse suspendUser(UUID userId) {
        return changeUserStatus(userId, UserStatus.SUSPENDED);
    }

    @Transactional
    public AdminUserResponse blockUser(UUID userId) {
        return changeUserStatus(userId, UserStatus.BLOCKED);
    }

    @Transactional
    public AdminUserResponse reactivateUser(UUID userId) {
        return changeUserStatus(userId, UserStatus.ACTIVE);
    }

    private AdminUserResponse changeUserStatus(UUID userId, UserStatus status) {
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (user.roleSet().contains(Role.SUPERADMIN)) {
            throw new IllegalArgumentException("La cuenta superadmin no se puede suspender ni bloquear");
        }
        if (status == UserStatus.SUSPENDED) {
            user.suspend();
        } else if (status == UserStatus.BLOCKED) {
            user.block();
        } else {
            user.activateAccount();
        }
        userRepository.save(user);
        return new AdminUserResponse(user.id(), user.email(), user.displayName(), user.roles(), user.status());
    }
}
