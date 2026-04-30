package com.arquixpress.marketplace.admin;

import com.arquixpress.marketplace.identity.AppUser;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.identity.Role;
import java.util.Arrays;
import java.util.List;
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
                user.roles()
            ))
            .collect(Collectors.toList());
    }

    @Transactional
    public AdminUserResponse makeAdmin(UUID userId) {
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Set<Role> currentRoles = Arrays.stream(user.roles().split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(Role::valueOf)
            .collect(Collectors.toSet());

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
            user.roles()
        );
    }

    @Transactional
    public AdminUserResponse updateUserRoles(UUID userId, Set<String> roleNames) {
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Set<Role> newRoles = roleNames.stream()
            .map(Role::valueOf)
            .collect(Collectors.toSet());

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
            user.roles()
        );
    }
}
