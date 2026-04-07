# Arquitectura POC ArquiXpress

## Lectura del SAD y diagramas

El SAD define ArquiXpress como marketplace de comercio electronico con compradores, vendedores, administradores y logistica. La decision principal es un monolito modular: una sola unidad desplegable, pero separada por dominios para mantener bajo acoplamiento y facilitar una futura evolucion a microservicios.

Diagramas considerados:

- Contenedores: SPA Angular, Marketplace API, Marketplace DB, pasarela de pagos externa y servicio externo de notificaciones.
- Dominio/ER: usuarios/roles/permisos, catalogo/productos/publicaciones, carrito/checkout/pedido, pago/transaccion, reserva de stock, envio, promociones, publicidad y notificaciones.
- Procesos: busqueda con cache en hora pico, checkout/pagos, reintento de pago, actualizacion de stock, estado de pedido, RBAC, promociones y publicidad pagada.
- Despliegue: frontend en servidor web, load balancer, dos instancias backend Spring Boot, PostgreSQL principal y replica por WAL streaming.

## ASR priorizados para la POC

| ASR | Necesidad | Tactica en POC |
| --- | --- | --- |
| ASR-01 | Checkout consistente | Orden + lineas + pago con estados explicitos y transacciones cortas |
| ASR-02 | Busqueda p95 <= 2s | Indices + cache Caffeine para busquedas repetidas |
| ASR-03 | Evitar cobros dobles | `Idempotency-Key` unico en `payment_transaction` |
| ASR-05 | Pasarela fallida no tumba marketplace | Adaptador de pago con resultado pendiente y timeouts simulables |
| ASR-06 | Notificaciones no frenan compras | Outbox persistente y publicador programado desacoplado |
| ASR-07 | RBAC | Guard de roles por endpoint; mock de identidad con headers |
| ASR-09 | No sobreventa | `update ... where stock_available >= quantity` atomico + versionado |
| ASR-10 | Recuperacion | Estados persistentes `PENDING_PAYMENT`, `PAID`, `PAYMENT_REJECTED` |
| ASR-11 | Mantenibilidad | Paquetes por modulo de dominio y dependencias explicitas |
| ASR-14 | Picos de uso | Stateless API, health checks, cache, DB centralizada y scripts k6 |
| ASR-16 | Historial correcto | Pedido y lineas con precio snapshot; consulta desde fuente persistente |
| ASR-17 | Estados de envio validos | Transiciones logisticas monotonicamente crecientes |
| ASR-18 | Cambios de reglas no rompen compras | El precio queda copiado en `order_line.unit_price` al crear orden |

## Decision ajustada: transacciones cortas

El ADR-01 del SAD sugiere ejecutar todas las operaciones criticas, incluida la pasarela, dentro de una misma transaccion. Para la POC se ajusta esa decision: no se mantiene una transaccion de base de datos abierta mientras se llama a la pasarela.

Razon tecnica: bajo carga, una llamada externa lenta dentro de una transaccion retiene conexiones y locks, aumentando el riesgo de incumplir ASR-05 y ASR-14. La POC conserva la consistencia mediante:

1. transaccion corta para reservar stock, crear orden y registrar intento de pago pendiente;
2. llamada a pasarela fuera de la transaccion;
3. transaccion corta para aplicar resultado e insertar evento de notificacion.

Esto mantiene recuperabilidad: si el proceso cae entre pasos, el pedido queda `PENDING_PAYMENT` y puede reintentarse con una nueva llave idempotente.

## Inconsistencia MySQL/PostgreSQL

El SAD menciona MySQL en ADR-01 y en varios diagramas de proceso. El diagrama fisico adjunto usa PostgreSQL con VM principal y replica por WAL streaming. Esta POC elige PostgreSQL por estar alineado con despliegue productivo y por soportar bien pruebas de concurrencia, bloqueo, replicas e indices.

Si el equipo decide MySQL, el diseno de dominio y servicios se mantiene; hay que adaptar migraciones SQL y el `docker-compose.yml`.

Decision formal: `docs/adr/ADR-006-postgresql.md`.

## Modulos implementados

- `identity`: usuario actual mockeado por headers y control RBAC basico.
- `catalog`: busqueda y detalle de productos con cache.
- `orders`: checkout, pedidos, lineas y estados logisticos.
- `payments`: transacciones con idempotencia y pasarela mock parametrizable.
- `notifications`: outbox para que notificaciones no bloqueen checkout.
- `promotions`: endpoint minimo para validar permiso administrativo.

## Endpoints relevantes

| Endpoint | ASR | Descripcion |
| --- | --- | --- |
| `GET /api/products/search` | ASR-02, ASR-14 | Busqueda cacheada con filtros basicos |
| `GET /api/products/{id}` | ASR-02, ASR-15 | Detalle de producto |
| `POST /api/checkout` | ASR-01, ASR-03, ASR-09 | Crea orden, reserva stock y procesa pago |
| `POST /api/payments/{orderId}/retry` | ASR-03, ASR-05, ASR-10 | Reintenta pago pendiente |
| `GET /api/orders/{orderId}` | ASR-16 | Consulta estado de pedido |
| `PATCH /api/logistics/orders/{id}/shipment` | ASR-17 | Actualiza estado logistico con roles validos |
| `POST /api/admin/promotions` | ASR-07, ASR-18 | Mock de creacion de campana restringida a admin |
| `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` | ASR-10, ASR-14 | Salud y metricas |

## Que indicaria que no vale la pena continuar asi

- `search_peak.js` no logra p95 < 2s aun con cache caliente e indices basicos.
- `checkout_concurrency.js` produce stock negativo, ordenes duplicadas o errores de integridad frecuentes.
- Al aumentar latencia/falla de pasarela, el pool de conexiones se agota o la API deja de responder.
- Cambios en promociones/publicidad requieren tocar codigo interno de checkout/catalogo sin interfaces claras.
- RBAC real con JWT introduce acoplamiento fuerte entre controladores y datos sensibles.

Si aparecen esos sintomas, antes de saltar a microservicios conviene endurecer el monolito: cache distribuida para busqueda, outbox formal para pagos/notificaciones, particion de lecturas, replicas de lectura y limites de concurrencia por endpoint.
