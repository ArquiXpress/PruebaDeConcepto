# ArquiXpress Marketplace POC

POC de arquitectura para validar si el enfoque del SAD es viable antes de invertir en la implementacion completa.

## Base reutilizada

El repositorio incluye el esqueleto `Moravia-Hotel-Angular` como referencia estructural. La migracion no se hace por copia literal del dominio hotelero, sino por adaptacion al marketplace:

- se conservan patrones de layout, guardas, formularios y servicios;
- se renombraran las pantallas al lenguaje del SAD;
- se documenta el mapeo en `docs/moravia-to-arquixpress-mapping.md`;
- se mantiene la POC y su evidencia en `docs/` y `load/`.

## Decision base

- Estilo: monolito modular con separacion por dominios (`catalog`, `orders`, `payments`, `notifications`, `identity`, `promotions`).
- Backend: Spring Boot, alineado con la restriccion del SAD.
- Base de datos: PostgreSQL en la POC, porque el diagrama fisico de despliegue adjunto usa PostgreSQL con replica. El SAD tambien menciona MySQL en otros puntos; esa inconsistencia queda documentada en `docs/architecture-poc.md`.
- Integraciones externas: pasarela de pagos y notificaciones simuladas con adaptadores internos para controlar latencia/fallo durante pruebas.
- Frontend: mockup navegable incluido en `src/main/resources/static` para cubrir catalogo, carrito, favoritos, usuario e inventario antes del pago.

## Estado actual de la adaptacion visual

- UI estatica de mercado ya disponible en `src/main/resources/static`.
- El frontend Angular de `Moravia-Hotel-Angular/proyecto` sigue siendo la referencia para el port de componentes.
- La migracion completa de componentes se debe hacer por fases para evitar mezclar nombres de hotel con nombres de marketplace.

## Ejecutar

```powershell
docker compose up --build
```

API: `http://localhost:8080`

Mockup UI: `http://localhost:8080/`

Health check:

```powershell
curl http://localhost:8080/actuator/health
```

Busqueda:

```powershell
curl "http://localhost:8080/api/products/search?query=pro&page=0&size=20"
```

Checkout idempotente:

```powershell
$body = '{"items":[{"productId":"11111111-1111-1111-1111-111111111111","quantity":1}]}'
curl -Method POST http://localhost:8080/api/checkout -Headers @{"Content-Type"="application/json";"Idempotency-Key"="demo-1";"X-User-Id"="00000000-0000-0000-0000-000000000001";"X-Roles"="CLIENT"} -Body $body
```

## Carga

Ver `load/README.md`.

## Alcance real de esta POC

Esta POC valida primero los riesgos de arquitectura mas caros:

- busqueda en hora pico con cache local e indices;
- checkout consistente sin ordenes/cobros duplicados;
- control de concurrencia para no sobreventa;
- degradacion controlada ante pasarela lenta o fallida;
- notificaciones desacopladas por outbox;
- RBAC minimo por rol.

No valida todavia Angular real, replica automatica completa de PostgreSQL a nivel de cloud, CDN/imagenes, autenticacion JWT real ni observabilidad completa con dashboards.
