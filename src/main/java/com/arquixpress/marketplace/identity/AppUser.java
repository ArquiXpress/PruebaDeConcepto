package com.arquixpress.marketplace.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String roles;

    protected AppUser() {
    }

    public static AppUser create(UUID id, String email, String password, String displayName, Set<Role> roles) {
        AppUser user = new AppUser();
        user.id = id;
        user.email = email;
        user.password = password;
        user.displayName = displayName;
        user.roles = roles.stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(","));
        return user;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String password() {
        return password;
    }

    public String displayName() {
        return displayName;
    }

    public Set<Role> roleSet() {
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Role::valueOf)
                .collect(java.util.stream.Collectors.toSet());
    }

    public AuthUser toAuthUser() {
        return new AuthUser(id, email, displayName, roleSet());
    }
}
