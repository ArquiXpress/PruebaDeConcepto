# Resultados de carga - 2026-04-06 - 200 VUS

Ambiente:

- Docker Desktop con red Compose `pruebadeconcepto_default`.
- API Spring Boot en `api:8080`.
- PostgreSQL 16 en contenedor `arquixpress-postgres`.
- k6 ejecutado con `grafana/k6` dentro de la red de Compose.

## Busqueda en hora pico

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://api:8080 -e VUS=200 -e DURATION=1m -v "${load}:/scripts" grafana/k6 run /scripts/search_peak.js
```

Resultado:

- Requests: 487,190
- Throughput: 8,116 req/s
- p95: 63.14ms
- Errores: 0%
- Umbral ASR-02: cumplido frente a p95 < 2s

## Checkout concurrente

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://api:8080 -e VUS=200 -e DURATION=30s -v "${load}:/scripts" grafana/k6 run /scripts/checkout_concurrency.js
```

Resultado:

- Requests: 41,842
- Throughput: 1,388 req/s
- p95: 364.99ms
- Errores esperados: 0%
- Umbral: cumplido frente a p95 < 3s

## Checkout con stock 1

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://api:8080 -e PRODUCT_ID=33333333-3333-3333-3333-333333333333 -e VUS=200 -e DURATION=30s -v "${load}:/scripts" grafana/k6 run /scripts/checkout_concurrency.js
```

Resultado:

- Requests: 67,014
- Throughput: 2,228 req/s
- p95: 276.41ms
- Errores esperados: 0%
- Verificacion DB: `stock_available = 0`, 1 orden pagada, 0 stock negativo.

## Lectura tecnica

Con 200 VUS la arquitectura sigue cumpliendo los objetivos base de la POC:

- la busqueda aguanta sin degradacion critica;
- el checkout no muestra doble cobro;
- la reserva de stock mantiene integridad;
- PostgreSQL sigue siendo el primer cuello de botella probable, no la API.

En comparacion con la corrida de 50 VUS:

- la latencia p95 de busqueda subio, pero sigue muy lejos del umbral;
- checkout tambien sube en latencia, pero conserva comportamiento correcto;
- el sistema aguanta el salto a 200 VUS sin romper los ASR principales.

## Decision

Estos resultados refuerzan la decision de mantener la POC como monolito modular con PostgreSQL por ahora. El siguiente paso razonable no es repartir servicios sin necesidad, sino introducir un load balancer y 2 backends para medir si la ganancia real es de throughput o si el limite sigue estando en la base de datos.
