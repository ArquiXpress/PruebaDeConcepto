# Cambio arquitectonico: catalogo como modulo interno separado - 2026-04-06

## Objetivo

Separar el catalogo como un modulo interno con frontera clara dentro del monolito modular, sin extraerlo aun a un microservicio independiente.

## Alcance del cambio

- Se movio la API del catalogo a `catalog.api`.
- Se movio la logica de aplicacion del catalogo a `catalog.application`.
- Se mantuvo el flujo de escritura en checkout, pagos, ordenes e identidad sin cambios.
- La replica de lectura sigue encapsulada dentro del modulo de catalogo.

## Razon de arquitectura

El catalogo tiene un perfil distinto al resto del sistema:

- mayor intensidad de lecturas;
- mas oportunidad de cache y replica de lectura;
- menor acoplamiento con el flujo critico de checkout;
- candidato natural a una futura extraccion si la carga lo justifica.

Separarlo como modulo interno permite:

- aislar la logica de lectura;
- reducir el impacto de cambios en checkout;
- dejar una frontera clara para una posible extraccion futura;
- mantener bajo el costo de complejidad frente a un microservicio nuevo.

## Validacion tecnica

- `mvn -q -DskipTests package` paso correctamente.
- `http://localhost:8080/actuator/health` responde `UP`.
- `docker compose ps` muestra la pila completa activa:
  - `api-1`
  - `api-2`
  - `lb`
  - `postgres`
  - `postgres-replica`

## Conclusion

El catalogo ya no esta mezclado con el resto de la aplicacion como una simple carpeta de controladores y servicios.
Ahora tiene una frontera interna clara, que mejora la mantenibilidad y deja abierta una extraccion futura si la carga de negocio lo amerita.

Por ahora, la decision correcta sigue siendo:

- monolito modular para checkout, pagos e identidad;
- catalogo como modulo interno separado;
- replica de lectura y cache para optimizar consultas de solo lectura.
