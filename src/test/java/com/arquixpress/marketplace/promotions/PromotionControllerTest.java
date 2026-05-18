package com.arquixpress.marketplace.promotions;

import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PromotionControllerTest {

    @Mock
    RoleGuard roles;

    PromotionController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new PromotionController(roles);
    }

    @Test
    void crearCampana_success() {
        PromotionRequest req = new PromotionRequest("Promo", Instant.now(), Instant.now().plusSeconds(3600));
        HttpServletRequest http = mock(HttpServletRequest.class);
        when(http.getHeader("X-User-Id")).thenReturn(UUID.randomUUID().toString());
        when(http.getHeader("X-Roles")).thenReturn("ADMIN");

        var resp = controller.create(req, http);
        assertThat(resp).isNotNull();
        assertThat(resp.name()).isEqualTo("Promo");
    }

    @Test
    void crearCampana_invalidDates_throws() {
        Instant now = Instant.now();
        PromotionRequest req = new PromotionRequest("Promo", now, now.minusSeconds(60));
        HttpServletRequest http = mock(HttpServletRequest.class);
        when(http.getHeader("X-User-Id")).thenReturn(UUID.randomUUID().toString());
        when(http.getHeader("X-Roles")).thenReturn("ADMIN");

        assertThatThrownBy(() -> controller.create(req, http))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha de fin");
    }
}
