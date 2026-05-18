package com.arquixpress.marketplace.promotions;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository {
    Invitation save(Invitation invitation);
    Optional<Invitation> findById(UUID id);
}
