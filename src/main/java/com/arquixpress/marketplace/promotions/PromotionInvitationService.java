package com.arquixpress.marketplace.promotions;

import com.arquixpress.marketplace.notifications.NotificationService;
import java.util.UUID;

public class PromotionInvitationService {
    private final InvitationRepository invitations;
    private final NotificationService notifications;

    public PromotionInvitationService(InvitationRepository invitations, NotificationService notifications) {
        this.invitations = invitations;
        this.notifications = notifications;
    }

    public Invitation inviteSeller(UUID promotionId, UUID sellerId) {
        Invitation inv = new Invitation(promotionId, sellerId);
        Invitation saved = invitations.save(inv);
        notifications.notify(sellerId, "PROMO_INVITE", "Invitacion a promocion", "Has sido invitado", "/promociones");
        return saved;
    }

    public Invitation respondInvitation(UUID invitationId, boolean accept) {
        Invitation inv = invitations.findById(invitationId).orElseThrow(() -> new IllegalArgumentException("Invitacion no encontrada"));
        if (accept) inv.accept(); else inv.decline();
        invitations.save(inv);
        return inv;
    }
}
