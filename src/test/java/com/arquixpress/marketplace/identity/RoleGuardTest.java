package com.arquixpress.marketplace.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/*
 * RF-05: Aplicar roles y permisos diferenciados según el tipo de usuario.
 */
class RoleGuardTest {

    private RoleGuard roleGuard;

    @BeforeEach
    void setUp() {
        roleGuard = new RoleGuard();
    }

    @Test
    void requireAny_deberiaPermitirAccesoCuandoUsuarioTieneElRolExacto() {
        CurrentUser admin = new CurrentUser(UUID.randomUUID(), Set.of(Role.ADMIN));

        assertDoesNotThrow(() -> roleGuard.requireAny(admin, Role.ADMIN));
    }

    @Test
    void requireAny_deberiaLanzarExcepcionCuandoUsuarioNoTieneNingunRolRequerido() {
        CurrentUser cliente = new CurrentUser(UUID.randomUUID(), Set.of(Role.CLIENT));

        assertThrows(AccessDeniedProblem.class,
                () -> roleGuard.requireAny(cliente, Role.ADMIN, Role.SELLER));
    }

    @Test
    void requireAny_deberiaPermitirAccesoCuandoUsuarioTieneAlMenosUnRolValido() {
        CurrentUser vendedor = new CurrentUser(UUID.randomUUID(), Set.of(Role.CLIENT, Role.SELLER));

        assertDoesNotThrow(() -> roleGuard.requireAny(vendedor, Role.ADMIN, Role.SELLER));
    }

    @Test
    void requireAny_deberiaLanzarExcepcionCuandoRolLogisticaIntentaAccederAreaAdmin() {
        CurrentUser logistica = new CurrentUser(UUID.randomUUID(), Set.of(Role.LOGISTICS));

        assertThrows(AccessDeniedProblem.class,
                () -> roleGuard.requireAny(logistica, Role.ADMIN, Role.SUPERADMIN));
    }

    @Test
    void requireAny_deberiaPermitirAccesoASuperadminEnCualquierOperacion() {
        CurrentUser superAdmin = new CurrentUser(UUID.randomUUID(), Set.of(Role.SUPERADMIN));

        assertDoesNotThrow(() -> roleGuard.requireAny(superAdmin, Role.ADMIN, Role.SUPERADMIN));
    }
}