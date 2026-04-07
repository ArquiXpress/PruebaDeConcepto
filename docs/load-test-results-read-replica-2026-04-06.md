# Resultados de carga con 2 backends + replica de lectura - 2026-04-06

Ambiente:

- Docker Compose con red `pruebadeconcepto_default`.
- Load balancer Nginx en `lb:8080`.
- Dos instancias Spring Boot: `api-1:8080` y `api-2:8080`.
- PostgreSQL 16 primario en `arquixpress-postgres`.
- PostgreSQL 16 replica de lectura en `arquixpress-postgres-replica`.
- k6 ejecutado con `grafana/k6`.

## Busqueda en hora pico - 50 VUS

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://lb:8080 -e VUS=50 -e DURATION=1m -v "${load}:/scripts" grafana/k6 run /scripts/search_peak.js
```

Resultado:

- Requests: 139,116
- Throughput: 2,318 req/s
- p95: 49.9ms
- Errores: 0%
- Umbral ASR-02: cumplido frente a p95 < 2s

## Busqueda en hora pico - 200 VUS

Comando:

```powershell
$load = (Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default -e BASE_URL=http://lb:8080 -e VUS=200 -e DURATION=1m -v "${load}:/scripts" grafana/k6 run /scripts/search_peak.js
```

Resultado:

- Requests: 178,346
- Throughput: 2,966 req/s
- p95: 97.98ms
- Errores: 0%
- Umbral ASR-02: cumplido frente a p95 < 2s

## Lectura

Comparado con la arquitectura de 2 backends sin replica de lectura:

- A 50 VUS la busqueda mejoro en p95 de `154.87ms` a `49.9ms`.
- A 200 VUS la busqueda quedo muy similar en p95, de `95.8ms` a `97.98ms`, pero con throughput algo menor.

Interpretacion:

- La replica de lectura si ayuda en cargas moderadas y reduce latencia media en busquedas.
- Bajo 200 VUS el cuello deja de ser la sola lectura y empieza a aparecer costo de red, LB y mezcla de consultas.
- El beneficio es real, pero no lineal.

## Conclusion

Para este marketplace, la combinacion de 2 backends + replica de lectura es util si el objetivo es optimizar consultas de catalogo sin tocar el flujo de escritura.

No conviene usar la replica para checkout, reservas o actualizaciones de stock.
La replica aporta valor para:

- busqueda de productos,
- detalle de producto,
- consultas de catalogo de solo lectura.

La estrategia sigue siendo viable para la POC, pero el siguiente paso ya no es agregar mas nodos por intuicion: es medir si el costo operativo de la replica compensa la mejora real en tus ASR.
