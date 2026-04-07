# Frontend Angular - frontera de POC

El SAD define una SPA Angular con routing, guards, HTTP interceptor, state management y clientes HTTP.

Esta POC no implementa Angular todavia porque la primera validacion de viabilidad es de backend + base de datos bajo carga. La frontera esperada es:

- `HTTP Interceptor`: agrega token/JWT real o, en POC, headers `X-User-Id` y `X-Roles`.
- `API Client Services`: consume `/api/products`, `/api/checkout`, `/api/orders`, `/api/admin/promotions`.
- `Route Guards`: restringen paneles de comprador, vendedor, administrador y logistica.
- `State Management`: mantiene sesion, carrito y resultados de busqueda.

Cuando la API pase las pruebas de carga, el siguiente paso es generar el workspace Angular y conectar estos endpoints.
