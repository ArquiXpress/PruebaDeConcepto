package com.arquixpress.marketplace.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class RollbackService {
    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void resetDatabase() {
        // Ejecutar truncates en orden de dependencias (tablas sin FK primero)
        em.createNativeQuery("TRUNCATE TABLE notification_outbox CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE payment_transaction CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE order_line CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE marketplace_order CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE app_user CASCADE").executeUpdate();

        // Reiniciar secuencias de PostgreSQL si existen
        em.createNativeQuery("ALTER SEQUENCE IF EXISTS product_id_seq RESTART WITH 1").executeUpdate();

        // Reinsertar datos iniciales
        insertInitialData();
    }

    @Transactional
    private void insertInitialData() {
        // Usuario cliente para pruebas
        em.createNativeQuery(
            "INSERT INTO app_user (id, email, password, display_name, roles, created_at, updated_at) " +
            "VALUES ('00000000-0000-0000-0000-000000000001', 'cliente@test.com', '$2a$10$MoEJpgnumJOhZE5QSK29m.dAOI5OwuMb1aVOXbY9lMJ3q2MtIQwRa', 'Cliente Test', 'CLIENT', NOW(), NOW())"
        ).executeUpdate();

        // Usuario admin para pruebas
        em.createNativeQuery(
            "INSERT INTO app_user (id, email, password, display_name, roles, created_at, updated_at) " +
            "VALUES ('00000000-0000-0000-0000-000000000099', 'admin@test.com', '$2a$10$y7sH7b168L8fxwc70t51XuSPIfNATzeyrK.supCLf86VKOZycp92C', 'Admin Test', 'ADMIN', NOW(), NOW())"
        ).executeUpdate();
    }
}
