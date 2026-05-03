package com.arquixpress.marketplace.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
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

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String phone;

    private String address;

    private String city;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private Instant resetTokenExpiresAt;

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

    public String roles() {
        return roles;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    public String phone() {
        return phone;
    }

    public String address() {
        return address;
    }

    public String city() {
        return city;
    }

    public String documentNumber() {
        return documentNumber;
    }

    public String resetToken() {
        return resetToken;
    }

    public Instant resetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    public Set<Role> roleSet() {
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Role::valueOf)
                .collect(java.util.stream.Collectors.toSet());
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public void setResetTokenExpiresAt(Instant resetTokenExpiresAt) {
        this.resetTokenExpiresAt = resetTokenExpiresAt;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AuthUser toAuthUser() {
        return new AuthUser(id, email, displayName, roleSet(), avatarUrl, phone, address, city, documentNumber);
    }
}
