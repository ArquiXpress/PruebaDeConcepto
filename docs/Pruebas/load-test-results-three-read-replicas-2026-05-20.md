# Resultados de Pruebas: 3 Replicas de Lectura - 2026-05-20

## Objetivo

Comprobar si para ArquiXpress vale la pena tener varias bases de datos de lectura, comparando:

- **Writer-only**: las lecturas de catalogo van al writer `postgres`.
- **3 readers**: las lecturas de catalogo van a `db-read-lb`, que reparte conexiones entre `postgres-replica-1`, `postgres-replica-2` y `postgres-replica-3`.

La prueba busca responder dos preguntas:

1. Si mejora el rendimiento de busqueda/listado de catalogo.
2. Si mejora la disponibilidad cuando una replica falla.

## Arquitectura Probada

Estado actual en Docker:

```text
Escrituras:
api-1 / api-2 -> db-ha-controller -> postgres

Lecturas de catalogo:
api-1 / api-2 -> db-read-lb -> postgres-replica-1 / postgres-replica-2 / postgres-replica-3
```

Conteo:

```text
Writers activos: 1
Readers activos: 3
PostgreSQL totales: 4
```

Verificacion de roles:

```text
postgres|f
postgres-replica-1|t
postgres-replica-2|t
postgres-replica-3|t
```

Interpretacion:

- `postgres|f`: el primario no esta en recovery, por tanto es writer.
- `replica-*|t`: las replicas estan en recovery, por tanto son standby/readers.

## Script de Prueba

Se agrego:

- `load/catalog_read_replica_compare.js`

Este script evita depender de una sola busqueda cacheada. Varía:

- pagina;
- tamano de pagina;
- categoria;
- llamadas con y sin categoria.

Con esto se fuerza una mezcla mas realista de lecturas de catalogo que el script anterior `search_peak.js`, el cual podia quedar demasiado beneficiado por cache al repetir siempre la misma query.

## Comandos Ejecutados

### Escenario A: writer-only

Se desactivo temporalmente:

```yaml
READ_REPLICA_ENABLED: "false"
```

Luego:

```powershell
docker compose up -d --force-recreate api-1 api-2 lb

$load=(Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default `
  -e BASE_URL=http://lb:8080 `
  -e VUS=50 `
  -e DURATION=45s `
  -v "${load}:/scripts" `
  grafana/k6 run --summary-export /scripts/results-writer-only-50.json /scripts/catalog_read_replica_compare.js

docker run --rm --network pruebadeconcepto_default `
  -e BASE_URL=http://lb:8080 `
  -e VUS=200 `
  -e DURATION=45s `
  -v "${load}:/scripts" `
  grafana/k6 run --summary-export /scripts/results-writer-only-200.json /scripts/catalog_read_replica_compare.js
```

### Escenario B: 3 readers

Se activo:

```yaml
READ_REPLICA_ENABLED: "true"
READ_DB_URL: jdbc:postgresql://db-read-lb:5432/arquixpress
```

Luego:

```powershell
docker compose up -d --force-recreate api-1 api-2 lb

$load=(Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default `
  -e BASE_URL=http://lb:8080 `
  -e VUS=50 `
  -e DURATION=45s `
  -v "${load}:/scripts" `
  grafana/k6 run --summary-export /scripts/results-3-readers-50.json /scripts/catalog_read_replica_compare.js

docker run --rm --network pruebadeconcepto_default `
  -e BASE_URL=http://lb:8080 `
  -e VUS=200 `
  -e DURATION=45s `
  -v "${load}:/scripts" `
  grafana/k6 run --summary-export /scripts/results-3-readers-200.json /scripts/catalog_read_replica_compare.js
```

### Escenario C: una replica caida

```powershell
docker compose stop postgres-replica-2

Invoke-RestMethod -Uri 'http://localhost:8080/api/products?page=0&size=1' -TimeoutSec 30

$load=(Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default `
  -e BASE_URL=http://lb:8080 `
  -e VUS=50 `
  -e DURATION=20s `
  -v "${load}:/scripts" `
  grafana/k6 run --summary-export /scripts/results-3-readers-one-down-50.json /scripts/catalog_read_replica_compare.js

docker compose start postgres-replica-2
```

## Resultados

| Escenario | VUS | Duracion | Requests | RPS | Avg | Med | P90 | P95 | Max | Errores |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Writer-only | 50 | 45s | 48,702 | 1,081.55 | 45.48ms | 31.78ms | 87.58ms | 123.02ms | 1,695.25ms | 0% |
| Writer-only | 200 | 45s | 112,219 | 2,489.25 | 79.08ms | 73.73ms | 115.82ms | 135.49ms | 985.63ms | 0% |
| 3 readers | 50 | 45s | 34,319 | 761.92 | 64.57ms | 45.39ms | 126.43ms | 175.37ms | 1,712.67ms | 0% |
| 3 readers | 200 | 45s | 88,829 | 1,967.31 | 100.09ms | 92.41ms | 150.56ms | 176.84ms | 713.20ms | 0% |
| 3 readers, 1 caida | 50 | 20s | 36,167 | 1,806.16 | 27.06ms | 23.31ms | 44.54ms | 53.84ms | 269.93ms | 0% |

Nota: la prueba con una replica caida duró 20s, por eso no debe compararse como rendimiento directo contra las corridas de 45s. Sirve para disponibilidad.

## Comparacion

### 50 VUS

```text
Writer-only p95: 123.02ms
3 readers p95: 175.37ms
Resultado: 3 readers fue 42.55% mas lento en p95.
```

Throughput:

```text
Writer-only RPS: 1,081.55
3 readers RPS: 761.92
Resultado: 3 readers proceso 29.55% menos requests por segundo.
```

### 200 VUS

```text
Writer-only p95: 135.49ms
3 readers p95: 176.84ms
Resultado: 3 readers fue 30.52% mas lento en p95.
```

Throughput:

```text
Writer-only RPS: 2,489.25
3 readers RPS: 1,967.31
Resultado: 3 readers proceso 20.97% menos requests por segundo.
```

## Verificacion de Balanceo

Se consulto `db-read-lb` varias veces desde la red de Docker:

```powershell
1..9 | ForEach-Object {
  docker run --rm --network pruebadeconcepto_default `
    -e PGPASSWORD=arquixpress `
    postgres:16-alpine `
    psql -h db-read-lb -U arquixpress -d arquixpress -tAc "select inet_server_addr();"
}
```

Resultado:

```text
172.18.0.6
172.18.0.5
172.18.0.4
172.18.0.5
172.18.0.4
172.18.0.6
172.18.0.4
172.18.0.6
172.18.0.5
```

Mapeo:

```text
postgres-replica-1 -> 172.18.0.4
postgres-replica-2 -> 172.18.0.6
postgres-replica-3 -> 172.18.0.5
```

Conclusion: `db-read-lb` si reparte conexiones entre las tres replicas.

## Prueba de Disponibilidad

Se detuvo:

```powershell
docker compose stop postgres-replica-2
```

Luego se consulto catalogo:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/products?page=0&size=1'
```

Resultado:

```text
La API respondio correctamente.
```

Despues se ejecuto k6 con 50 VUS durante 20s:

```text
p95: 53.84ms
errores: 0%
requests: 36,167
```

Conclusion: aunque una replica caiga, el endpoint de lectura sigue funcionando porque HAProxy saca la replica no disponible y usa las restantes.

## Interpretacion Arquitectonica

### Lo que si queda comprobado

1. **Varias replicas mejoran disponibilidad de lectura.**
   Si una replica cae, catalogo sigue respondiendo sin cambiar el frontend ni las APIs.

2. **El balanceador de lectura funciona.**
   `db-read-lb` reparte conexiones entre `postgres-replica-1`, `postgres-replica-2` y `postgres-replica-3`.

3. **Las replicas estan correctamente configuradas como standby/readers.**
   `pg_is_in_recovery()` devuelve `true` en las tres replicas.

4. **Un solo writer sigue siendo la decision correcta.**
   Checkout, stock, cupones y ordenes deben escribir en un unico punto de verdad para evitar doble gasto, sobreventa y cupones usados dos veces.

### Lo que no queda comprobado

No se comprobo mejora de rendimiento con 3 readers en esta POC local. De hecho, las pruebas muestran peor p95 y menor RPS.

La causa mas probable no es que las replicas sean inutiles en general, sino que en esta POC:

- todas las bases corren en la misma maquina fisica;
- Docker comparte CPU, memoria y disco entre writer y readers;
- HAProxy agrega un salto de red adicional;
- el dataset es pequeno;
- el catalogo ya tiene cache e indices;
- no hay suficiente carga real de I/O como para que separar lecturas compense el costo de red/proxy.

## Decision Recomendada

Para esta POC:

```text
Mantener 3 replicas como demostracion de escalabilidad y disponibilidad.
No venderlas como mejora de rendimiento comprobada en local.
```

Para produccion o despliegue serio:

```text
Usar 1 writer + N readers solo si los readers estan en recursos separados
o usar AWS Aurora con writer endpoint + reader endpoint.
```

Con 6 VMs reales, mi recomendacion sigue siendo:

```text
VM 1: Frontend
VM 2: Load Balancer + DB HA Controller
VM 3: API 1
VM 4: API 2
VM 5: PostgreSQL writer
VM 6: PostgreSQL reader/standby
```

Si el profesor exige varias readers, se puede demostrar con Docker como en esta POC. Pero para que realmente mejoren rendimiento, las replicas deben tener recursos separados o ir a Aurora/RDS.

## Conclusion Final

Las 3 replicas **si valen la pena para disponibilidad y demostracion arquitectonica**.

Las 3 replicas **no demostraron valer la pena para rendimiento en esta maquina local**, porque fueron mas lentas que leer del writer:

```text
50 VUS: p95 subio de 123.02ms a 175.37ms.
200 VUS: p95 subio de 135.49ms a 176.84ms.
```

Decision tecnica:

```text
Mantener 1 writer activo.
Mantener varias replicas para lecturas tolerantes a consistencia eventual.
No usar multiples writers activos.
No prometer mejora de performance local; justificar valor por disponibilidad, failover y similitud con Aurora Reader Endpoint.
```
