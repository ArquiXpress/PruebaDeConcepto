# ArquiXpress Marketplace POC

POC de arquitectura para validar si el enfoque del SAD es viable antes de invertir en la implementacion completa.

## Decision base

- Estilo: monolito modular con separacion por dominios (`catalog`, `orders`, `payments`, `notifications`, `identity`, `promotions`).
- Backend: Spring Boot, alineado con la restriccion del SAD.
- Base de datos: PostgreSQL en la POC, porque el diagrama fisico de despliegue adjunto usa PostgreSQL con replica. El SAD tambien menciona MySQL en otros puntos; esa inconsistencia queda documentada en `docs/architecture-poc.md`.
- Integraciones externas: pasarela de pagos y notificaciones simuladas con adaptadores internos para controlar latencia/fallo durante pruebas.
- Frontend: frontera Angular documentada, no implementada en esta iteracion.

## Ejecutar

```powershell
docker compose up --build
```

API: `http://localhost:8080`

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

No valida todavia UI Angular, replica real de PostgreSQL, CDN/imagenes, autenticacion JWT real ni observabilidad completa con dashboards.
