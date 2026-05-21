# Implementacion del backend monolito modular ArquiXpress

Fecha: 2026-04-23

## Objetivo

Construir el backend de ArquiXpress sobre el proyecto actual del repositorio, excluyendo por completo `TheMoraviaHotel` y `Moravia-Hotel-Angular`, y manteniendo estas decisiones de arquitectura:

- monolito modular;
- Spring Boot como runtime;
- PostgreSQL como base de datos principal;
- caching local para busqueda;
- controles de idempotencia para pagos;
- notificaciones desacopladas por outbox;
- autenticacion liviana por headers para la POC.

## Criterio de reutilizacion

Se tomo como base el backend ya existente en `src/main/java/com/arquixpress/marketplace`, porque ya estaba orientado a marketplace y ya expresaba mejor el SAD que los proyectos Moravia.

No se reutilizo:

- `TheMoraviaHotel`, porque su dominio es hotelero y usa MVC con Thymeleaf;
- `Moravia-Hotel-Angular`, porque su frontend es hotelero y no corresponde al dominio de ArquiXpress.

## Estructura final por modulos

### `catalog`

Responsabilidad:

- persistencia de productos;
- busqueda por texto y categoria;
- detalle de producto;
- actualizacion de stock;
- soporte para replica de lectura opcional.

Clases clave:

- `Product`
- `ProductRepository`
- `CatalogService`
- `ProductController`
- `ProductSummary`
- `ProductStatus`
- `ReadReplicaConfig`

### `orders`

Responsabilidad:

- checkout;
- ordenes;
- lineas de orden;
- reintento de pago;
- consulta de pedido;
- actualizacion de envio.

Clases clave:

- `OrderEntity`
- `OrderLine`
- `OrderRepository`
- `CheckoutService`
- `CheckoutController`
- `LogisticsController`
- `OrderResponse`
- `CheckoutResponse`
- `PendingCheckout`
- `ShipmentStatus`
- `OrderStatus`

### `payments`

Responsabilidad:

- transaccion de pago;
- idempotencia;
- simulacion de pasarela;
- estados de pago.

Clases clave:

- `PaymentTransaction`
- `PaymentTransactionRepository`
- `PaymentGatewayClient`
- `MockPaymentGatewayClient`
- `PaymentGatewayResult`
- `PaymentStatus`

### `notifications`

Responsabilidad:

- outbox de eventos;
- publicacion asincrona simulada;
- desacoplo del checkout.

Clases clave:

- `NotificationOutbox`
- `NotificationOutboxRepository`
- `NotificationPublisher`
- `OutboxStatus`

### `identity`

Responsabilidad:

- lectura de usuario actual desde headers;
- roles de negocio;
- control de acceso por rol.

Clases clave:

- `CurrentUser`
- `Role`
- `RoleGuard`
- `AccessDeniedProblem`

### `common`

Responsabilidad:

- normalizacion de errores;
- respuesta uniforme de API.

Clases clave:

- `GlobalExceptionHandler`
- `ApiError`

### `promotions`

Responsabilidad:

- punto minimo de administracion para validar rol admin.

Clases clave:

- `PromotionController`
- `PromotionRequest`
- `PromotionResponse`

## Base de datos

Se definio PostgreSQL como base principal.

### Tablas

- `product`
- `marketplace_order`
- `order_line`
- `payment_transaction`
- `notification_outbox`

### Decisiones fisicas

- claves primarias UUID;
- timestamps en UTC;
- indices por busqueda, estado y relaciones;
- restriccion de stock no negativo;
- llave unica de idempotencia en pagos;
- versionado optimista para evitar sobrescrituras silenciosas.

## Migraciones

### `V1__init.sql`

Crea el modelo fisico del dominio principal.

### `V2__seed_products.sql`

Inserta datos iniciales para pruebas de:

- busqueda;
- stock;
- checkout;
- reintento;
- validacion de no sobreventa.

## Configuracion tecnica

### Runtime

El `application.yml` se ajusto para apuntar a PostgreSQL por defecto:

- `jdbc:postgresql://localhost:5432/arquixpress`
- usuario y clave por defecto: `arquixpress`

### Pruebas

Se dejo el backend sin H2. Las pruebas de integracion contra PostgreSQL se deben ejecutar en un entorno con PostgreSQL disponible, no con base embebida.

### Build

El backend ya no depende de H2 en ningun perfil. PostgreSQL queda como unica base de datos para runtime.

## Prueba automatizada agregada

Se creo `MarketplacePocApplicationTests` para verificar que el contexto arranca y que los beans modulares principales estan disponibles:

- `ProductRepository`
- `OrderRepository`
- `PaymentTransactionRepository`

## Documentacion generada o actualizada

- `docs/moravia-to-arquixpress-mapping.md`
- `docs/backend-monolith-implementation-2026-04-23.md`
- `docs/architecture-poc.md`
- `docs/asr-traceability.md`
- `README.md`

## Resultado tecnico

El backend queda alineado con la idea del SAD:

- una sola aplicacion desplegable;
- dominio separado por modulos;
- persistencia real en PostgreSQL;
- POC operativa para compra, pago, inventario y notificaciones;
- evidencia tecnica documentada.

## Siguientes pasos razonables

1. Conectar un frontend ArquiXpress que consuma estos endpoints.
2. Ampliar la semilla de datos para vendedor, admin y logistica.
3. Agregar pruebas de integracion sobre checkout, stock e idempotencia.
4. Si el SAD exige eso, formalizar autenticacion JWT en lugar de headers mock.
