# Plan de pruebas de la implementación básica - 2026-04-21

## Objetivo

Validar la demo funcional de ArquiXpress antes del pago, con foco en:

- catálogo;
- carrito;
- favoritos;
- usuario mockeado;
- inventario visible;
- checkout simulado;
- lectura desde réplica cuando está habilitada.

## Casos mínimos

1. Abrir `/` y verificar que el mockup carga.
2. Buscar un producto desde el catálogo.
3. Agregar y quitar productos del carrito.
4. Marcar y desmarcar favoritos.
5. Cambiar el usuario demo y confirmar que el rol cambia en la sesión.
6. Ejecutar `Simular pago` con productos en carrito.
7. Confirmar que el checkout responde con el estado del pago mock.
8. Verificar que `GET /api/products` y `GET /api/products/{id}` siguen funcionando.

## Pruebas de arquitectura

- Desactivar la réplica de lectura y confirmar que el catálogo sigue operando sobre el datasource principal.
- Activar `READ_REPLICA_ENABLED=true` y comprobar que las consultas de catálogo se resuelven por JDBC hacia la réplica.
- Simular caída del primario PostgreSQL y revisar la documentación de failover para confirmar que el router/promotor sigue la réplica.

## Criterio de aceptación

La POC se considera suficiente para exposición académica si:

- la pantalla principal funciona sin construir un frontend adicional;
- el catálogo se puede usar para preparar una compra;
- el pago sigue siendo simulado, no externo;
- el alcance excluido del profesor queda fuera de la demo;
- existe este plan documentado para la revisión.
