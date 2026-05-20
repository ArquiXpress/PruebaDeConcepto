# Arquitectura Aplicada: Load Balancer, DB HA, Replicas y Catalogo

Este documento describe las decisiones arquitectonicas implementadas en la POC de ArquiXpress, con base en las pruebas de carga/failover existentes y en las correcciones solicitadas.

## Resumen Ejecutivo

Con la restriccion de **6 maquinas virtuales**, la arquitectura recomendada e implementada para la POC es:

```text
Usuario
  -> Frontend
  -> Nginx Load Balancer
  -> api-1 / api-2
  -> DB HA Controller
  -> PostgreSQL writer activo

Catalogo:
  api-1 / api-2
    -> DB Read LB
    -> postgres-replica-1 / postgres-replica-2 / postgres-replica-3
```

Decisiones principales:

- El frontend consume el Load Balancer, no backends directos.
- `db-router` y `db-arbiter` se fusionaron en un solo `DB HA Controller`.
- Las APIs escriben contra un endpoint estable: `db-ha-controller:5432`.
- El catalogo usa tres replicas de lectura con `READ_REPLICA_ENABLED=true`, balanceadas por `db-read-lb`.
- No se implementan multiples writers activos porque rompen el modelo transaccional seguro de checkout/stock/cupones.
- El catalogo sigue como modulo interno del monolito modular, no como microservicio separado.

## 1. Frontend hacia Load Balancer

La solucion mas segura es:

```text
Frontend -> Load Balancer -> Backends
```

No:

```text
Frontend -> Backend directo
```

### Justificacion

Con Load Balancer:

- no se exponen `api-1` y `api-2` directamente;
- se centraliza TLS, CORS, headers, rate limiting y WAF si aplica;
- se puede sacar un backend enfermo sin cambiar el frontend;
- se evita acoplar el navegador a IPs o nodos concretos;
- se habilita escalamiento horizontal de API.

En la POC, Angular envia `/api` al Load Balancer mediante:

- `frontend/proxy.conf.json`

```json
{
  "/api": {
    "target": "http://lb:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

El Load Balancer usado es Nginx:

- `ops/nginx.conf`

```nginx
upstream arquixpress_api {
  zone arquixpress_api 64k;
  least_conn;
  server api-1:8080 resolve max_fails=3 fail_timeout=10s;
  server api-2:8080 resolve max_fails=3 fail_timeout=10s;
}
```

## 2. DB HA Controller

Antes la POC tenia dos componentes separados:

- `db-router`
- `db-arbiter`

Eso era util para explicar responsabilidades, pero no era la mejor vista arquitectonica final. Se fusionaron en:

- `db-ha-controller`
- `ops/postgres/db-ha-controller.sh`

Responsabilidades del `DB HA Controller`:

- exponer un endpoint estable para escrituras;
- monitorear el writer principal;
- promover la replica si el writer cae;
- redirigir conexiones hacia el writer activo;
- evitar volver automaticamente al primario viejo si ya se promovio la replica.

Flujo normal:

```text
api-1 / api-2
  -> db-ha-controller:5432
  -> postgres:5432
```

Ante falla del writer:

```text
api-1 / api-2
  -> db-ha-controller:5432
  -> primera replica sana promovida:
     postgres-replica-1 / postgres-replica-2 / postgres-replica-3
```

La configuracion aplicada en `docker-compose.yml` es:

```yaml
DB_URL: jdbc:postgresql://db-ha-controller:5432/arquixpress
```

Esto corrige el problema anterior, donde las APIs apuntaban directo a:

```yaml
DB_URL: jdbc:postgresql://postgres:5432/arquixpress
```

## 3. Replicas de Lectura

La POC activa lectura desde varias replicas para catalogo:

```yaml
READ_REPLICA_ENABLED: "true"
READ_DB_URL: jdbc:postgresql://db-read-lb:5432/arquixpress
```

En Docker se levantan tres readers fisicos:

```text
postgres-replica-1 -> puerto host 5433
postgres-replica-2 -> puerto host 5434
postgres-replica-3 -> puerto host 5435
```

Las lecturas no apuntan a una replica concreta. Pasan por HAProxy:

```text
api-1 / api-2 -> db-read-lb:5432 -> replica disponible
```

`db-read-lb` usa `ops/postgres/read-lb.cfg` con `roundrobin`, por lo que las conexiones de lectura se reparten entre las tres replicas sanas. Esto se parece al patron de Aurora Reader Endpoint: la aplicacion ve un endpoint estable y la capa de base decide a que reader enviar la conexion.

La clase que encapsula el datasource de lectura es:

- `src/main/java/com/arquixpress/marketplace/catalog/ReadReplicaConfig.java`

La clase que decide leer desde replica o primario es:

- `src/main/java/com/arquixpress/marketplace/catalog/application/CatalogService.java`

Solo se deben enviar a replica operaciones tolerantes a consistencia eventual:

- busqueda de productos;
- detalle de producto;
- listados de catalogo;
- ofertas visibles al cliente.

No deben ir a replica:

- checkout;
- pagos;
- stock;
- ordenes;
- cupones;
- redenciones de cupon;
- aprobaciones/rechazos de ofertas.

## 4. Muchas Readers y una Writer

Si existen muchas readers y una sola writer:

```text
writer-1 -> escrituras
reader-1 -> lecturas
reader-2 -> lecturas
reader-3 -> lecturas
```

Si `writer-1` falla, **solo una reader se promueve**:

```text
writer-1 -> caida
reader-1 -> promovida a writer-2
reader-2 -> sigue como reader
reader-3 -> sigue como reader
```

No todas las readers se vuelven writers. Eso seria peligroso.

En AWS Aurora este comportamiento lo maneja el servicio:

- writer endpoint apunta al writer activo;
- reader endpoint distribuye lecturas;
- ante failover, Aurora promueve una replica y actualiza el writer endpoint.

Referencias oficiales:

- Aurora endpoints: https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.Endpoints.Cluster.html
- Aurora reader endpoint: https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.Endpoints.Reader.html
- Aurora high availability: https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Concepts.AuroraHighAvailability.html

## 5. Por Que No Implementar Dos Writers Activos

No se implementan dos writers activos porque el dominio requiere consistencia fuerte.

Ejemplo de sobreventa:

```text
Producto A tiene stock = 1

writer-1 lee stock = 1 y vende 1 unidad
writer-2 lee stock = 1 y vende 1 unidad

Resultado: 2 ventas sobre 1 unidad
```

Ejemplo de cupon:

```text
Cliente X usa TECH20 en writer-1
Cliente X usa TECH20 en writer-2 antes de resolver replicacion

Resultado: cupon usado dos veces
```

La consulta atomica de stock actual funciona porque existe una sola fuente de verdad de escritura:

```sql
update product
   set stock_available = stock_available - :quantity
 where id = :productId
   and stock_available >= :quantity
```

Con multiples writers, esa garantia local no alcanza si cada writer tiene su propia copia.

Para multi-writer real haria falta otra arquitectura:

- particionamiento por region/shard;
- consenso;
- resolucion de conflictos;
- transacciones distribuidas;
- tecnologia multi-master;
- reglas de negocio compensatorias.

Para ArquiXpress POC, la decision correcta es:

```text
1 writer activo + n readers + promocion controlada
```

## 6. Catalogo: Modulo Interno, No Microservicio

El catalogo ya fue separado como modulo interno del monolito modular.

Documento relacionado:

- `docs/architecture-poc-catalog-module-split-2026-04-06.md`

No se recomienda extraerlo aun como microservicio porque agregaria:

- despliegue independiente;
- contratos remotos;
- retries/timeouts;
- observabilidad distribuida;
- sincronizacion de datos;
- riesgo de inconsistencia con stock/ofertas/vendedores.

Antes de extraerlo conviene agotar:

- cache;
- indices;
- replicas de lectura;
- paginacion;
- separacion modular interna;
- CDN para imagenes.

Decision:

```text
Mantener catalogo como modulo interno con cache y replicas de lectura.
```

Extraerlo solo si pruebas futuras muestran que catalogo satura de forma independiente o bloquea la evolucion del checkout.

## 7. Pruebas Ejecutadas y Evidencia

### Prueba: busqueda por Load Balancer

Comando ejecutado:

```powershell
$load=(Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default `
  -e BASE_URL=http://lb:8080 `
  -e VUS=20 `
  -e DURATION=20s `
  -v "${load}:/scripts" `
  grafana/k6 run /scripts/search_peak.js
```

Resultado:

- Requests: 13,239
- Throughput: 661.5 req/s
- p95: 64.02ms
- Errores: 0%
- Umbral cumplido: p95 < 2s

Interpretacion:

- El flujo `Frontend -> LB -> API` es viable.
- Nginx no aparece como cuello de botella en esta carga.

### Prueba: checkout por Load Balancer

Comando ejecutado:

```powershell
$load=(Resolve-Path .\load).Path
docker run --rm --network pruebadeconcepto_default `
  -e BASE_URL=http://lb:8080 `
  -e VUS=10 `
  -e DURATION=15s `
  -v "${load}:/scripts" `
  grafana/k6 run /scripts/checkout_concurrency.js
```

Resultado:

- Requests: 5,102
- Throughput: 339.5 req/s
- p95: 72.72ms
- Errores: 0%
- Umbral cumplido: p95 < 3s

Interpretacion:

- Checkout se mantiene estable detras del Load Balancer.
- No se observaron errores de llave duplicada.

### Evidencia previa: 2 backends + LB

Documento:

- `docs/load-test-results-lb-2026-04-06.md`

Resultados:

- Busqueda 50 VUS: p95 154.87ms, 0% errores.
- Busqueda 200 VUS: p95 95.8ms, 0% errores.
- Checkout 50 VUS: p95 1.18s.
- Checkout 200 VUS: p95 955.58ms.

Conclusion:

- Dos backends detras de Nginx son viables.
- El LB no es el cuello principal.

### Evidencia previa: replica de lectura

Documento:

- `docs/load-test-results-read-replica-2026-04-06.md`
- `docs/load-test-results-three-read-replicas-2026-05-20.md`

Comparacion:

- Sin replica, busqueda 50 VUS: p95 154.87ms.
- Con replica, busqueda 50 VUS: p95 49.9ms.
- A 200 VUS el beneficio no fue lineal.

Conclusion:

- La replica de lectura ayuda en cargas moderadas.
- Agregar readers no garantiza mejora lineal.
- Conviene medir antes de sobredimensionar.
- En la prueba local con 3 readers, las replicas mejoraron disponibilidad pero no rendimiento, porque todos los contenedores comparten la misma maquina fisica y se agrega el costo de `db-read-lb`.

### Evidencia previa: failover

Documento:

- `docs/load-test-ha-failover-2026-04-07.md`

Prueba:

- se detuvo `postgres`;
- se promovio una replica standby;
- `pg_is_in_recovery()` devolvio `false`.

Conclusion:

- La promocion automatica es viable en POC.
- Para produccion conviene Aurora, Patroni, repmgr o una solucion administrada.

## 8. Arquitectura con 6 VMs

Con 6 VMs, la distribucion recomendada para produccion sigue priorizando disponibilidad real por maquina:

```text
VM 1: Frontend / servidor web
VM 2: Nginx Load Balancer + DB HA Controller
VM 3: Backend API 1
VM 4: Backend API 2
VM 5: PostgreSQL writer
VM 6: PostgreSQL reader + standby promovible
```

La POC Docker, para demostrar el experimento de varias bases solicitado, levanta mas readers que maquinas dedicadas:

```text
postgres              -> writer activo
postgres-replica-1    -> reader / standby
postgres-replica-2    -> reader / standby
postgres-replica-3    -> reader / standby
db-read-lb            -> balanceador de lecturas
```

Ventajas:

- respeta la separacion frontend/LB/API/BD;
- mantiene alta disponibilidad de backend;
- habilita replicas de lectura para catalogo en la POC;
- permite promocion de reader a writer;
- cabe en 6 VMs.

Desventajas:

- en una distribucion estricta de 6 VMs solo hay una reader dedicada; para varias readers reales se deben co-ubicar contenedores o usar Aurora/RDS;
- el `DB HA Controller` comparte VM con el LB;
- no reemplaza una solucion HA productiva.

## 9. Variante Recomendada con AWS Aurora

Si AWS Aurora esta permitido, la mejor evolucion es:

```text
Frontend/CDN
  -> AWS Application Load Balancer
  -> api-1 / api-2
  -> Aurora writer endpoint
  -> Aurora reader endpoint
```

Aurora reemplaza:

- `postgres`;
- las replicas PostgreSQL;
- `DB HA Controller`;
- scripts de promocion.

Configuracion esperada:

```yaml
DB_URL: jdbc:postgresql://arquixpress.cluster-xxxx.us-east-1.rds.amazonaws.com:5432/arquixpress
READ_REPLICA_ENABLED: "true"
READ_DB_URL: jdbc:postgresql://arquixpress.cluster-ro-xxxx.us-east-1.rds.amazonaws.com:5432/arquixpress
```

Interpretacion:

- `cluster-...` es writer endpoint.
- `cluster-ro-...` es reader endpoint.

Aurora permite escalar readers sin cambiar la aplicacion, porque el reader endpoint distribuye conexiones entre replicas.

## 10. Decisiones Que Valen La Pena

Si vale la pena:

- Frontend hacia Load Balancer.
- Dos backends detras de Nginx.
- Un writer activo.
- Replica de lectura para catalogo.
- Promocion controlada de reader a writer.
- Catalogo como modulo interno separado.
- Aurora si se permite AWS administrado.

No vale la pena por ahora:

- Multiples writers activos.
- Extraer catalogo a microservicio.
- Gastar varias de las 6 VMs solo en readers.
- Mantener router y arbiter como componentes separados.

## 11. Estado Implementado en la POC

Cambios aplicados:

- `docker-compose.yml`
  - reemplaza `db-router` y `db-arbiter` por `db-ha-controller`;
  - cambia `DB_URL` a `jdbc:postgresql://db-ha-controller:5432/arquixpress`;
  - activa `READ_REPLICA_ENABLED=true`;
  - agrega `postgres-replica-1`, `postgres-replica-2` y `postgres-replica-3`;
  - agrega `db-read-lb` como endpoint estable de lectura;
  - agrega health checks para las tres replicas, `db-read-lb` y `db-ha-controller`;
  - hace que las APIs esperen a la BD HA y al endpoint de lectura antes de arrancar.

- `ops/nginx.conf`
  - mantiene dos backends detras de Nginx;
  - agrega resolver DNS interno de Docker (`127.0.0.11`) para evitar 502 cuando `api-1` o `api-2` se recrean y cambian de IP;
  - conserva `least_conn` para repartir trafico entre backends disponibles.

- `ops/postgres/db-ha-controller.sh`
  - enruta conexiones al writer activo;
  - promueve una replica sana si cae el primario;
  - acepta varios candidatos con `REPLICA_HOSTS`;
  - conserva la replica promovida como writer hasta intervencion manual.

- `ops/postgres/read-lb.cfg`
  - balancea lecturas entre tres replicas PostgreSQL;
  - usa `roundrobin` y health checks TCP.

- Se eliminaron los scripts separados:
  - `ops/postgres/db-router.sh`;
  - `ops/postgres/db-arbiter.sh`.

La arquitectura de la POC queda alineada con la decision final:

```text
Frontend -> LB -> API -> DB HA Controller -> writer activo
Catalogo -> DB Read LB -> 3 read replicas
```

Conteo actual en la POC:

```text
Writers activos: 1
Readers activos: 3
DBs PostgreSQL totales: 4
```
