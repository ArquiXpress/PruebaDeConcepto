package com.arquixpress.marketplace.promotions;

import com.arquixpress.marketplace.notifications.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PromotionInvitationServiceTest {

    @Mock
    InvitationRepository invitations;

    @Mock
    NotificationService notifications;

    PromotionInvitationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PromotionInvitationService(invitations, notifications);
    }

    @Test
    void invitarVendedor_creaInvitacion() {
        UUID promo = UUID.randomUUID();
        UUID seller = UUID.randomUUID();
        when(invitations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Invitation inv = service.inviteSeller(promo, seller);

        assertThat(inv).isNotNull();
        assertThat(inv.promotionId()).isEqualTo(promo);
        verify(notifications).notify(eq(seller), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void responderInvitacion_acceptAndDecline() {
        UUID promo = UUID.randomUUID();
        UUID seller = UUID.randomUUID();
        Invitation inv = new Invitation(promo, seller);
        when(invitations.findById(inv.id())).thenReturn(Optional.of(inv));
        when(invitations.save(any())).thenAnswer(a -> a.getArgument(0));

        Invitation accepted = service.respondInvitation(inv.id(), true);
        assertThat(accepted.status()).isEqualTo(Invitation.Status.ACCEPTED);

        Invitation declined = service.respondInvitation(inv.id(), false);
        assertThat(declined.status()).isEqualTo(Invitation.Status.DECLINED);
    }

    @Test
    void responderInvitacion_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(invitations.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.respondInvitation(id, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invitacion no encontrada");
    }
}
