package com.arquixpress.marketplace.sellers;

import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;
import com.arquixpress.marketplace.notifications.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/*
 * RF-21: Gestión de vendedores
 */
class SellerApplicationServiceTest {

    private NamedParameterJdbcTemplate jdbc;
    private ObjectMapper mapper;
    private AppUserRepository userRepository;
    private NotificationService notifications;
    private NotificationOutboxRepository outbox;
    private SellerApplicationService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(NamedParameterJdbcTemplate.class);
        mapper = new ObjectMapper();
        userRepository = mock(AppUserRepository.class);
        notifications = mock(NotificationService.class);
        outbox = mock(NotificationOutboxRepository.class);
        service = new SellerApplicationService(jdbc, mapper, userRepository, notifications, outbox);
    }

    @Test
    void create_shouldRejectJuridicaWithoutCompanyName() {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), Set.of(Role.CLIENT));
        SellerApplicationRequest request = new SellerApplicationRequest(
                "JURIDICA", "NIT", "123456789", null, null, null,
                null, null, null, "electronica",
                List.of(new SellerApplicationRequest.ProductDraftRequest(
                        "Producto", "Desc", "http://img.png", null, BigDecimal.valueOf(10000), 5)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(user, request));
        assertEquals("El nombre de la empresa es obligatorio", ex.getMessage());
    }

    @Test
    void create_shouldRejectProductWithoutImage() {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), Set.of(Role.CLIENT));
        SellerApplicationRequest request = new SellerApplicationRequest(
                "NATURAL", "CC", "123456789", null, null, null,
                null, null, null, "electronica",
                List.of(new SellerApplicationRequest.ProductDraftRequest(
                        "Producto Sin Foto", "Desc", null, List.of(), BigDecimal.valueOf(10000), 5)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(user, request));
        assertEquals("Cada producto debe tener al menos una foto", ex.getMessage());
    }

    @Test
    void create_shouldPersistApplicationWithStatusPendingReview() {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), Set.of(Role.CLIENT));
        SellerApplicationRequest request = new SellerApplicationRequest(
                "NATURAL", "CC", "123456789", "doc.pdf", "base64content", "application/pdf",
                null, null, "3001234567", "electronica",
                List.of(new SellerApplicationRequest.ProductDraftRequest(
                        "Mesa", "Mesa de madera", "http://img.png", null, BigDecimal.valueOf(50000), 3)));

        when(jdbc.update(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class))).thenReturn(1);

        SellerApplicationResponse result = service.create(user, request);

        assertNotNull(result);
        assertEquals("PENDING_REVIEW", result.status());
        assertEquals(user.id(), result.userId());
        assertEquals("electronica", result.category());
        verify(jdbc).update(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class));
    }

    @Test
    void approve_shouldThrowWhenApplicationAlreadyReviewed() {
        UUID applicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CurrentUser reviewer = new CurrentUser(UUID.randomUUID(), Set.of(Role.ADMIN));

        when(jdbc.queryForObject(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenAnswer(inv -> {
                    org.springframework.jdbc.core.RowMapper<?> rm = inv.getArgument(2);
                    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                    when(rs.getObject("id", UUID.class)).thenReturn(applicationId);
                    when(rs.getObject("user_id", UUID.class)).thenReturn(userId);
                    when(rs.getString("display_name")).thenReturn("Vendedor");
                    when(rs.getString("email")).thenReturn("vendedor@mail.com");
                    when(rs.getString("seller_type")).thenReturn("NATURAL");
                    when(rs.getString("legal_document_type")).thenReturn("CC");
                    when(rs.getString("legal_document_number")).thenReturn("1234");
                    when(rs.getString("document_file_name")).thenReturn(null);
                    when(rs.getString("document_file_content")).thenReturn(null);
                    when(rs.getString("document_file_mime_type")).thenReturn(null);
                    when(rs.getString("company_name")).thenReturn(null);
                    when(rs.getString("company_description")).thenReturn(null);
                    when(rs.getString("contact_phone")).thenReturn(null);
                    when(rs.getString("category")).thenReturn("electronica");
                    when(rs.getString("products_json")).thenReturn("[]");
                    when(rs.getString("status")).thenReturn("APPROVED");
                    when(rs.getObject("reviewed_by", UUID.class)).thenReturn(UUID.randomUUID());
                    when(rs.getTimestamp("reviewed_at")).thenReturn(null);
                    when(rs.getString("review_note")).thenReturn(null);
                    when(rs.getInt("approved_product_count")).thenReturn(0);
                    when(rs.getTimestamp("created_at")).thenReturn(null);
                    return rm.mapRow(rs, 0);
                });

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.approve(applicationId, reviewer, null));
        assertEquals("Esta solicitud ya fue revisada", ex.getMessage());
    }

    @Test
    void reject_shouldThrowWhenApplicationAlreadyReviewed() {
        UUID applicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CurrentUser reviewer = new CurrentUser(UUID.randomUUID(), Set.of(Role.ADMIN));

        when(jdbc.queryForObject(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenAnswer(inv -> {
                    org.springframework.jdbc.core.RowMapper<?> rm = inv.getArgument(2);
                    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                    when(rs.getObject("id", UUID.class)).thenReturn(applicationId);
                    when(rs.getObject("user_id", UUID.class)).thenReturn(userId);
                    when(rs.getString("display_name")).thenReturn("Vendedor");
                    when(rs.getString("email")).thenReturn("vendedor@mail.com");
                    when(rs.getString("seller_type")).thenReturn("NATURAL");
                    when(rs.getString("legal_document_type")).thenReturn("CC");
                    when(rs.getString("legal_document_number")).thenReturn("1234");
                    when(rs.getString("document_file_name")).thenReturn(null);
                    when(rs.getString("document_file_content")).thenReturn(null);
                    when(rs.getString("document_file_mime_type")).thenReturn(null);
                    when(rs.getString("company_name")).thenReturn(null);
                    when(rs.getString("company_description")).thenReturn(null);
                    when(rs.getString("contact_phone")).thenReturn(null);
                    when(rs.getString("category")).thenReturn("electronica");
                    when(rs.getString("products_json")).thenReturn("[]");
                    when(rs.getString("status")).thenReturn("REJECTED");
                    when(rs.getObject("reviewed_by", UUID.class)).thenReturn(UUID.randomUUID());
                    when(rs.getTimestamp("reviewed_at")).thenReturn(null);
                    when(rs.getString("review_note")).thenReturn(null);
                    when(rs.getInt("approved_product_count")).thenReturn(0);
                    when(rs.getTimestamp("created_at")).thenReturn(null);
                    return rm.mapRow(rs, 0);
                });

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.reject(applicationId, reviewer, null));
        assertEquals("Esta solicitud ya fue revisada", ex.getMessage());
    }

    @Test
    void create_shouldNormalizeCategoryToLowerCase() {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), Set.of(Role.CLIENT));
        SellerApplicationRequest request = new SellerApplicationRequest(
                "NATURAL", "CC", "111", null, null, null,
                null, null, null, "  ELECTRONICA  ",
                List.of(new SellerApplicationRequest.ProductDraftRequest(
                        "Producto", "Desc", "http://img.png", null, BigDecimal.valueOf(10000), 1)));

        when(jdbc.update(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class))).thenReturn(1);

        SellerApplicationResponse result = service.create(user, request);

        assertEquals("electronica", result.category());
    }
}
