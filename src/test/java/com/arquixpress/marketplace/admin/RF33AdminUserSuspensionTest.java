package com.arquixpress.marketplace.admin;

import com.arquixpress.marketplace.identity.AppUser;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.UserStatus;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RF33AdminUserSuspensionTest {

    private AppUserRepository users;
    private AdminUserService service;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        service = new AdminUserService(users);
    }

    @Test
    void adminCanSuspendBlockAndReactivateUser() {
        UUID userId = UUID.randomUUID();
        AppUser user = AppUser.create(userId, "cliente@arquixpress.com", "pwd", "Cliente", Set.of(Role.CLIENT));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse suspended = service.suspendUser(userId);
        AdminUserResponse blocked = service.blockUser(userId);
        AdminUserResponse active = service.reactivateUser(userId);

        assertThat(suspended.status()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(blocked.status()).isEqualTo(UserStatus.BLOCKED);
        assertThat(active.status()).isEqualTo(UserStatus.ACTIVE);
        verify(users, times(3)).save(user);
    }

    @Test
    void superAdminAccountCannotBeSuspendedOrBlocked() {
        UUID userId = UUID.randomUUID();
        AppUser superadmin = AppUser.create(userId, "root@arquixpress.com", "pwd", "Root", Set.of(Role.SUPERADMIN));
        when(users.findById(userId)).thenReturn(Optional.of(superadmin));

        assertThatThrownBy(() -> service.blockUser(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("superadmin");
    }
}
