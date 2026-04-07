# Pruebas de carga POC

Escenarios iniciales alineados con el SAD:

- `search_peak.js`: valida ASR-02 y ASR-14. Umbral: p95 < 2s para busqueda.
- `checkout_concurrency.js`: valida ASR-01, ASR-03 y ASR-09. Usa `Idempotency-Key` y reserva atomica de stock.
- `rbac_smoke.js`: valida ASR-07 con una prueba minima de autorizacion por rol.

Ejecucion local si `k6` esta instalado:

```powershell
k6 run .\load\search_peak.js
k6 run .\load\checkout_concurrency.js
k6 run .\load\rbac_smoke.js
```

Ejecucion sin instalar k6, usando Docker y la red de Compose:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://lb:8080 -e VUS=50 -e DURATION=1m -v "${load}:/scripts" grafana/k6 run /scripts/search_peak.js
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://lb:8080 -e VUS=20 -e DURATION=30s -v "${load}:/scripts" grafana/k6 run /scripts/checkout_concurrency.js
```

Para probar no sobreventa, ejecutar `checkout_concurrency.js` contra el producto con stock 1:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://lb:8080 -e PRODUCT_ID=33333333-3333-3333-3333-333333333333 -e VUS=20 -e DURATION=30s -v "${load}:/scripts" grafana/k6 run /scripts/checkout_concurrency.js
```

Interpretacion minima:

- Si busqueda supera p95 de 2s con cache caliente, revisar indices/consulta y considerar cache distribuida.
- Si checkout genera stock negativo o multiples ordenes con la misma llave, la arquitectura no cumple ASR-03/ASR-09.
- Si la pasarela mock queda lenta y la API se degrada masivamente, revisar pools, timeouts y procesamiento asincrono.
