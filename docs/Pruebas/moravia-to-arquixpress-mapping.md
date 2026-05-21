# Mapeo Moravia -> ArquiXpress

Este documento sirve como puente entre el esqueleto reutilizable de `Moravia-Hotel-Angular` y la solucion ArquiXpress definida por el SAD.

## Criterio de adaptacion

- Se reutiliza la estructura de presentacion, layouts, guards y servicios cuando aporten valor al marketplace.
- Se renombra el dominio hotelero a dominios de comercio electronico.
- Se elimina o posterga cualquier flujo que no exista en el SAD o que compita con el alcance actual de la POC.

## Mapeo de dominios

| Moravia | ArquiXpress |
| --- | --- |
| `home` | landing de marketplace, catalogo destacado, promociones |
| `room` | producto / publicacion |
| `roomtype` | categoria / tipo de producto |
| `servicio` | servicio complementario o beneficio asociado al pedido |
| `cliente` | comprador |
| `usuario` | usuario interno / admin / vendedor |
| `reserva` | carrito, checkout, orden |
| `cuenta` | perfil de cuenta / billetera / metodo de pago |
| `dashboard` | panel administrativo |
| `reportes` | analitica y trazabilidad operativa |
| `historia` | historial de pedidos y auditoria |

## Componentes reutilizables

- `layouts/*`
- `components/header`
- `components/footer`
- `components/hamburger`
- `components/user-dropdown`
- `services/*`
- `auth.guard.ts`
- formularios y tablas CRUD base

## Componentes a reescribir con prioridad

- `home/*`
- `room/*`
- `roomtype/*`
- `reserva/*`
- `cliente/*`
- `usuario/*`
- `reportes/*`

## Artefactos ya alineados con el SAD

- `src/main/resources/static/index.html`
- `src/main/resources/static/app.js`
- `src/main/resources/static/app.css`
- `docs/architecture-poc.md`
- `docs/asr-traceability.md`

## Siguiente fase recomendada

1. Copiar la arquitectura visual de Moravia a un frontend ArquiXpress.
2. Renombrar componentes por dominio marketplace.
3. Conectar las vistas al backend POC con contratos estables.
4. Separar pruebas de concepto y documentacion por ASR.
