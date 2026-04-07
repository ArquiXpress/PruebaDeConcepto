# ADR-006: PostgreSQL como base de datos relacional de la POC

## Estado

Aceptada.

## Contexto

el diagrama fisico de despliegue adjunto modela una VM de base de datos con PostgreSQL Server y replica por WAL streaming.

La POC necesita validar principalmente:

- ASR-01: checkout consistente;
- ASR-03: no duplicar cobros;
- ASR-09: no sobreventa bajo concurrencia;
- ASR-14: sostener picos de busqueda, carrito y checkout;
- ASR-16: historial correcto.

## Decision

La POC adopta PostgreSQL 16 como base de datos relacional oficial.

## Razonamiento

- Alinea la POC con el diagrama fisico de despliegue productivo.
- Soporta bien transacciones ACID, updates atomicos, indices e integridad referencial.
- Permite evolucionar a replicas de lectura con WAL streaming, tal como aparece en la vista fisica.
- Reduce ambiguedad para las pruebas de carga: el comportamiento observado corresponde a la tecnologia objetivo de despliegue.

## Consecuencias

Positivas:

- La validacion de concurrencia y stock es representativa para el despliegue propuesto.
- Se evita reescribir migraciones y configuracion despues de las pruebas.
- Es una buena base para replica de lectura y observabilidad de consultas.



## Validacion inicial

El 6 de abril de 2026 se ejecuto la POC con PostgreSQL 16 en Docker:

- Busqueda, 50 VUs por 1 minuto: p95 35.47ms, 0% errores, 235,948 requests.
- Checkout concurrente, 20 VUs por 30s: p95 368.9ms, 0% errores esperados, 6,113 requests.
- Checkout con producto de stock 1, 20 VUs por 30s: p95 60.19ms, 0% errores esperados, 23,941 requests.
- Verificacion directa en DB: producto de stock 1 quedo en `stock_available = 0` y solo 1 unidad pagada; no hubo stock negativo.
