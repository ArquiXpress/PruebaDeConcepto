# Prueba de failover con `api-1` caido - 2026-04-06

Escenario:

- Docker Compose con `lb:8080` como load balancer.
- `api-1` detenido de forma manual.
- Solo `api-2` disponible detras de Nginx.
- PostgreSQL 16 activo y compartido.
- k6 ejecutado con `grafana/k6`.

## Estado previo

Se detuvo `api-1` con:

```powershell
docker compose stop api-1
```

El LB siguio atendiendo requests usando `api-2`.

## Busqueda en hora pico - 50 VUS

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://lb:8080 -e VUS=50 -e DURATION=1m -v "${load}:/scripts" grafana/k6 run /scripts/search_peak.js
```

Resultado:

- Requests: 119,528
- Throughput: 1,328 req/s
- p95: 63.26ms
- Errores: 0%
- Umbral ASR-02: cumplido frente a p95 < 2s

## Checkout concurrente - 50 VUS

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://lb:8080 -e VUS=50 -e DURATION=30s -v "${load}:/scripts" grafana/k6 run /scripts/checkout_concurrency.js
```

Resultado:

- Requests: 14,083
- Throughput: 468 req/s
- p95: 267.46ms
- Errores esperados: 0%
- Umbral: cumplido frente a p95 < 3s

## Conclusion

Con `api-1` caido, el sistema siguio operando con `api-2` sin errores funcionales.

Lectura tecnica:

- El load balancer sigue respondiendo y deriva el trafico a la instancia restante.
- La busqueda mantuvo latencia baja y estable.
- El checkout tambien se mantuvo dentro de umbral.
- Para esta POC, dos backends + LB si dan tolerancia a falla parcial; el sistema no depende de una sola instancia.
