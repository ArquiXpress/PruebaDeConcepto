# Frontend ArquiXpress

SPA Angular para el marketplace ArquiXpress.

## Proposito

- consumir el backend modular en Spring Boot;
- mostrar catalogo, carrito y checkout;
- servir como base para el port completo del SAD;
- mantener el dominio separado del proyecto hotelero Moravia.

## Desarrollo local

```bash
cd frontend
npm install
npm start
```

La app corre en `http://localhost:4200`.

## Backend esperado

El proxy de desarrollo redirige `/api` hacia `http://lb:8080`, que es el balanceador del entorno Docker.

## Estado actual

- catalogo inicial;
- carrito en memoria/localStorage;
- checkout contra backend;
- selector de usuario demo con roles.
