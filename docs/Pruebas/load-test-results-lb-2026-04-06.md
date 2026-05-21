# Resultados de carga con 2 backends + load balancer - 2026-04-06

Ambiente:

- Docker Compose con red `pruebadeconcepto_default`.
- Load balancer Nginx en `lb:8080`.
- Dos instancias Spring Boot: `api-1:8080` y `api-2:8080`.
- PostgreSQL 16 en contenedor `arquixpress-postgres`.
- k6 ejecutado con `grafana/k6`.

## Busqueda en hora pico - 50 VUS

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://lb:8080 -e VUS=50 -e DURATION=1m -v "${load}:/scripts" grafana/k6 run /scripts/search_peak.js
```

Resultado:

- Requests: 67,081
- Throughput: 1,118 req/s
- p95: 154.87ms
- Errores: 0%
- Umbral ASR-02: cumplido frente a p95 < 2s

## Busqueda en hora pico - 200 VUS

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://lb:8080 -e VUS=200 -e DURATION=1m -v "${load}:/scripts" grafana/k6 run /scripts/search_peak.js
```

Resultado:

- Requests: 182,326
- Throughput: 3,037 req/s
- p95: 95.8ms
- Errores: 0%
- Umbral ASR-02: cumplido frente a p95 < 2s

## Checkout concurrente - 50 VUS

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://lb:8080 -e VUS=50 -e DURATION=30s -v "${load}:/scripts" grafana/k6 run /scripts/checkout_concurrency.js
```

Resultado:

- Requests: 2,613
- Throughput: 87 req/s
- p95: 1.18s
- Errores esperados: 0%
- Umbral: cumplido frente a p95 < 3s

## Checkout concurrente - 200 VUS

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://lb:8080 -e VUS=200 -e DURATION=30s -v "${load}:/scripts" grafana/k6 run /scripts/checkout_concurrency.js
```

Resultado:

- Requests: 14,334
- Throughput: 474 req/s
- p95: 955.58ms
- Errores esperados: 0%
- Umbral: cumplido frente a p95 < 3s

## Decision

La arquitectura con dos backends detras de Nginx es viable para la POC.

Observaciones:

- La busqueda se mantiene muy por debajo del umbral de 2s incluso con 200 VUS.
- El checkout sigue cumpliendo el umbral de 3s con 200 VUS, aunque ya muestra mayor latencia que la arquitectura de un solo backend.
- El cuello de botella principal no parece ser el load balancer; la carga se distribuye, pero PostgreSQL y el flujo de checkout siguen dominando el costo.
- El siguiente paso util ya no es solo subir VUS, sino comparar con y sin LB y medir el impacto real en throughput, p95 y comportamiento bajo falla de una instancia.
