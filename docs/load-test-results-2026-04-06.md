# Resultados de carga - 2026-04-06

Ambiente:

- Docker Desktop con red Compose `pruebadeconcepto_default`.
- API Spring Boot en `api:8080`.
- PostgreSQL 16 en contenedor `arquixpress-postgres`.
- k6 ejecutado con `grafana/k6`.

## Busqueda en hora pico

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://api:8080 -e VUS=50 -e DURATION=1m -v "${load}:/scripts" grafana/k6 run /scripts/search_peak.js
```

Resultado:

- Requests: 235,948
- Throughput: 3,933 req/s
- p95: 35.47ms
- Errores: 0%
- Umbral ASR-02: cumplido frente a p95 < 2s

Nota: el primer intento fallo por conectividad usando `host.docker.internal` desde el contenedor k6 y luego por una query JPQL ambigua en PostgreSQL (`lower(bytea)`). Se corrigio la query separando casos de busqueda.

## Checkout concurrente

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://api:8080 -e VUS=20 -e DURATION=30s -v "${load}:/scripts" grafana/k6 run /scripts/checkout_concurrency.js
```

Resultado:

- Requests: 6,113
- Throughput: 203 req/s
- p95: 368.9ms
- Errores esperados: 0%
- Umbral: cumplido frente a p95 < 3s

## Checkout con stock 1

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://api:8080 -e PRODUCT_ID=33333333-3333-3333-3333-333333333333 -e VUS=20 -e DURATION=30s -v "${load}:/scripts" grafana/k6 run /scripts/checkout_concurrency.js
```

Resultado:

- Requests: 23,941
- Throughput: 798 req/s
- p95: 60.19ms
- Errores esperados: 0%
- Verificacion DB: `stock_available = 0`, 1 unidad pagada, sin stock negativo.

## Decision

Con estos resultados iniciales, PostgreSQL queda aceptado para la POC. La arquitectura de monolito modular sigue siendo viable para la siguiente iteracion, con la salvedad de que falta validar JWT real, observabilidad, replica de lectura y carga con dataset mas grande.
