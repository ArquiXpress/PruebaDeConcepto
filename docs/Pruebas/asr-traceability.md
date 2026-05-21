# Trazabilidad ASR -> POC

| ASR | Codigo / artefacto |
| --- | --- |
| ASR-01 Checkout consistente | `src/main/java/com/arquixpress/marketplace/orders/CheckoutService.java` |
| ASR-02 Busqueda rapida | `src/main/java/com/arquixpress/marketplace/catalog/CatalogService.java`, `load/search_peak.js` |
| ASR-03 Pago sin cobros dobles | `src/main/java/com/arquixpress/marketplace/payments/PaymentTransaction.java`, indice unique en `V1__init.sql` |
| ASR-05 Pasarela no tumba marketplace | `src/main/java/com/arquixpress/marketplace/payments/MockPaymentGatewayClient.java` |
| ASR-06 Notificaciones no frenan compras | `src/main/java/com/arquixpress/marketplace/notifications/NotificationOutbox.java` |
| ASR-07 Cada rol solo hace lo suyo | `src/main/java/com/arquixpress/marketplace/identity/RoleGuard.java`, `load/rbac_smoke.js` |
| ASR-09 No sobreventa | `ProductRepository.reserveStock(...)`, `load/checkout_concurrency.js` |
| ASR-10 Recuperacion | estados persistidos en `marketplace_order` y `payment_transaction` |
| ASR-11 Cambiar modulo sin danar otro | paquetes por dominio bajo `src/main/java/com/arquixpress/marketplace` |
| ASR-14 Picos de uso | `docker-compose.yml`, actuator, cache y scripts k6 |
| ASR-16 Historial correcto | `OrderRepository.findWithLines(...)` y snapshots en `order_line` |
| ASR-17 Actualizar envio sin errores | `OrderEntity.updateShipment(...)` |
| ASR-18 Cambios de reglas no rompen compras | `OrderLine.unitPrice` como snapshot |
