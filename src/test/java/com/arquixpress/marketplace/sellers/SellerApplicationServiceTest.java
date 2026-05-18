package com.arquixpress.marketplace.sellers;

import com.arquixpress.marketplace.identity.AppUser;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;
import com.arquixpress.marketplace.notifications.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SellerApplicationServiceTest {

    @Mock
    NamedParameterJdbcTemplate jdbc;

    // use a real mapper to avoid mocking final behaviour
    ObjectMapper mapper;

    @Mock
    AppUserRepository userRepository;

    @Mock
    NotificationService notifications;

    @Mock
    NotificationOutboxRepository outbox;

    SellerApplicationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mapper = new ObjectMapper();
        service = new SellerApplicationService(jdbc, mapper, userRepository, notifications, outbox);
    }

    @Test
    void crearSolicitud_conProductos_exito() throws Exception {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), Set.of(Role.CLIENT));
        SellerApplicationRequest.ProductDraftRequest product = new SellerApplicationRequest.ProductDraftRequest(
                "Titulo", "Descripcion", "http://img.example/1.png", null, new BigDecimal("10.0"), 5);
        SellerApplicationRequest req = new SellerApplicationRequest("natural", "DNI", "123", null, null, null,
                null, null, null, "categoria", List.of(product));

        when(jdbc.update(anyString(), any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class))).thenReturn(1);

        SellerApplicationResponse resp = service.create(user, req);

        assertThat(resp).isNotNull();
        assertThat(resp.userId()).isEqualTo(user.id());
        assertThat(resp.status()).isEqualTo("PENDING_REVIEW");
        assertThat(resp.category()).isEqualTo("categoria");
    }

    @Test
    void crearSolicitud_sinImagenes_lanzaIllegalArgumentException() {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), Set.of(Role.CLIENT));
        SellerApplicationRequest.ProductDraftRequest product = new SellerApplicationRequest.ProductDraftRequest(
                "Titulo", "Descripcion", null, List.of(), new BigDecimal("10.0"), 5);
        SellerApplicationRequest req = new SellerApplicationRequest("natural", "DNI", "123", null, null, null,
                null, null, null, "Categoria", List.of(product));

        assertThatThrownBy(() -> service.create(user, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cada producto debe tener al menos una foto");
    }

    @Test
    void aprobarSolicitud_invocaNotificacionesYGuardaUsuario() {
        UUID appId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SellerApplicationRequest.ProductDraftRequest product = new SellerApplicationRequest.ProductDraftRequest(
                "Titulo", "Descripcion", "http://img.example/1.png", null, new BigDecimal("10.0"), 5);
        AdminSellerApplicationResponse application = new AdminSellerApplicationResponse(
                appId, userId, "Name", "email@example.com", "NAT", "DNI", "123",
                null, null, null, null, null, "categoria", List.of(product), "PENDING_REVIEW",
                null, null, null, 1, null);

        when(jdbc.queryForObject(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(application);

        AppUser user = AppUser.create(userId, "email@example.com", "pw", "Name", Set.of(Role.CLIENT));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jdbc.update(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class))).thenReturn(1);

        service.approve(appId, new CurrentUser(UUID.randomUUID(), Set.of(Role.ADMIN)), new SellerApplicationDecisionRequest("ok"));

        verify(userRepository).save(any(AppUser.class));
        verify(notifications).notify(eq(userId), anyString(), anyString(), anyString(), anyString());
        verify(outbox).save(any());
    }
}
