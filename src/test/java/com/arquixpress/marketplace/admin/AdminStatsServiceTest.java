package com.arquixpress.marketplace.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/*
 * RF-43: Permitir al administrador consultar un registro de acciones
 *        administrativas relevantes realizadas en la plataforma.
 */
class AdminStatsServiceTest {

    private EntityManager em;
    private AdminStatsService adminStatsService;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        adminStatsService = new AdminStatsService(em);
    }

    @Test
    void getStats_deberiaRetornarConteosCorrectosDeRecursos() {
        Query qUsuarios  = mock(Query.class);
        Query qProductos = mock(Query.class);
        Query qPedidos   = mock(Query.class);
        Query qRevenue   = mock(Query.class);

        when(em.createQuery("SELECT COUNT(u) FROM AppUser u")).thenReturn(qUsuarios);
        when(em.createQuery("SELECT COUNT(p) FROM Product p")).thenReturn(qProductos);
        when(em.createQuery("SELECT COUNT(o) FROM OrderEntity o")).thenReturn(qPedidos);
        when(em.createQuery(
                "SELECT SUM(o.total) FROM OrderEntity o WHERE o.status = 'COMPLETED'"))
                .thenReturn(qRevenue);

        when(qUsuarios.getSingleResult()).thenReturn(10L);
        when(qProductos.getSingleResult()).thenReturn(25L);
        when(qPedidos.getSingleResult()).thenReturn(5L);
        when(qRevenue.getSingleResult()).thenReturn(null);

        AdminStatsResponse result = adminStatsService.getStats();

        assertEquals(10L, result.totalUsers());
        assertEquals(25L, result.totalProducts());
        assertEquals(5L,  result.totalOrders());
        assertEquals(0L,  result.totalRevenue());
    }

    @Test
    void getStats_deberiaCalcularRevenueCorrectamenteCuandoHayPedidosCompletados() {
        Query qUsuarios  = mock(Query.class);
        Query qProductos = mock(Query.class);
        Query qPedidos   = mock(Query.class);
        Query qRevenue   = mock(Query.class);

        when(em.createQuery("SELECT COUNT(u) FROM AppUser u")).thenReturn(qUsuarios);
        when(em.createQuery("SELECT COUNT(p) FROM Product p")).thenReturn(qProductos);
        when(em.createQuery("SELECT COUNT(o) FROM OrderEntity o")).thenReturn(qPedidos);
        when(em.createQuery(
                "SELECT SUM(o.total) FROM OrderEntity o WHERE o.status = 'COMPLETED'"))
                .thenReturn(qRevenue);

        when(qUsuarios.getSingleResult()).thenReturn(3L);
        when(qProductos.getSingleResult()).thenReturn(8L);
        when(qPedidos.getSingleResult()).thenReturn(2L);
        when(qRevenue.getSingleResult()).thenReturn(150000L);

        AdminStatsResponse result = adminStatsService.getStats();

        assertEquals(150000L, result.totalRevenue());
    }
}