# Informe de pruebas de desempeno - Arquitectura distribuida ArquiXpress

Fecha de ejecucion: 2026-05-21

Arquitectura evaluada: despliegue distribuido en 6 maquinas virtuales con Nginx Load Balancer en VM2, dos instancias API, PostgreSQL writer y replicas de lectura.

## Resumen ejecutivo

Las pruebas ejecutadas muestran que la arquitectura distribuida responde correctamente bajo carga de lectura y mantiene disponibilidad ante fallos parciales. Los escenarios evaluados fueron busqueda en catalogo, lectura de catalogo usando replicas, caida de una instancia API y caida de readers en VM6.

En todos los escenarios reportados se cumplio el umbral de latencia definido para busquedas y lecturas de catalogo: p95 menor a 2 segundos. Tambien se obtuvo 0% de errores HTTP en las pruebas incluidas.

## Arquitectura bajo prueba

| VM | IP | Rol |
|---|---|---|
| VM1 | 10.43.98.219 | Frontend |
| VM2 | 10.43.100.124 | Nginx Load Balancer, db-ha-controller, db-read-lb |
| VM3 | 10.43.100.129 | API 1 |
| VM4 | 10.43.99.98 | API 2 |
| VM5 | 10.43.97.250 | PostgreSQL writer + reader1 |
| VM6 | 10.43.100.188 | PostgreSQL reader2 + reader3 |

Endpoint principal usado para las pruebas:

```text
http://10.43.100.124
```

## Resultados consolidados

| Prueba | Carga | Duracion | p95 | Requests | Throughput | Errores | Estado |
|---|---:|---:|---:|---:|---:|---:|---|
| Busqueda de catalogo | 50 VUs | 1 min | 20.3 ms | 276,587 | 4,609 req/s | 0.00% | Aprobada |
| Catalogo con readers | 200 VUs | 45 s | 48.16 ms | 285,001 | 6,329 req/s | 0.00% | Aprobada |
| Busqueda con API1 caida | 50 VUs | 1 min | 19.55 ms | 255,735 | 4,261 req/s | 0.00% | Aprobada |
| Catalogo con readers de VM6 caidos | 50 VUs | 45 s | 12.83 ms | 259,360 | 5,762 req/s | 0.00% | Aprobada |

## Prueba 1: Busqueda de catalogo

Objetivo: validar que el endpoint de busqueda de productos mantiene baja latencia bajo carga concurrente.

Configuracion:

```text
VUs: 50
Duracion: 1 minuto
Endpoint: /api/products?query=pro&page=0&size=20
Umbral: p95 < 2000 ms
```

Resultados:

```text
p95: 20.3 ms
avg: 10.45 ms
max: 640.45 ms
http_req_failed: 0.00%
http_reqs: 276,587
throughput: 4,609 req/s
checks_succeeded: 100%
```

Conclusion: la prueba fue aprobada. La busqueda cumplio ampliamente el umbral de desempeno, con p95 de 20.3 ms frente al limite de 2,000 ms.

## Prueba 2: Catalogo con readers

Objetivo: validar el rendimiento de lecturas de catalogo usando la arquitectura actual con `db-read-lb` y replicas de lectura.

Configuracion:

```text
VUs: 200
Duracion: 45 segundos
Endpoint: /api/products
Umbral: p95 < 2000 ms
```

Resultados:

```text
p95: 48.16 ms
avg: 31.29 ms
max: 345.7 ms
http_req_failed: 0.00%
http_reqs: 285,001
throughput: 6,329 req/s
checks_succeeded: 100%
```

Conclusion: la prueba fue aprobada. La arquitectura de lectura con replicas sostuvo 200 usuarios virtuales con baja latencia y sin errores.

Nota: al finalizar la prueba se presento un error de permisos al guardar el archivo JSON de resumen:

```text
permission denied: /scripts/results-vm-3-readers-200.json
```

Este error no afecta el resultado de la prueba de carga. La ejecucion termino correctamente; solo fallo la escritura del resumen en el volumen montado.

## Prueba 3: Busqueda con API1 caida

Objetivo: comprobar que el Load Balancer mantiene servicio cuando una instancia backend queda fuera de linea.

Accion de falla ejecutada:

```bash
ssh -i ~/.ssh/arquixpress_deploy deploy@10.43.100.129 "docker stop arquixpress-api1"
```

Configuracion de carga:

```text
VUs: 50
Duracion: 1 minuto
Endpoint: /api/products?query=pro&page=0&size=20
Umbral: p95 < 2000 ms
```

Resultados:

```text
p95: 19.55 ms
avg: 11.38 ms
max: 244.41 ms
http_req_failed: 0.00%
http_reqs: 255,735
throughput: 4,261 req/s
checks_succeeded: 100%
```

Conclusion: la prueba fue aprobada. Con API1 detenida, el Load Balancer siguio atendiendo trafico por la instancia restante. Esto valida continuidad del servicio ante caida parcial del backend.

## Prueba 4: Catalogo con readers de VM6 caidos

Objetivo: validar que las lecturas siguen funcionando cuando se pierden los readers alojados en VM6.

Accion de falla ejecutada:

```bash
ssh -i ~/.ssh/arquixpress_deploy deploy@10.43.100.188 "docker stop pg-reader2 pg-reader3"
```

Readers detenidos:

```text
pg-reader2
pg-reader3
```

Configuracion de carga:

```text
VUs: 50
Duracion: 45 segundos
Endpoint: /api/products
Umbral: p95 < 2000 ms
```

Resultados:

```text
p95: 12.83 ms
avg: 8.51 ms
max: 2.31 s
http_req_failed: 0.00%
http_reqs: 259,360
throughput: 5,762 req/s
checks_succeeded: 100%
```

Conclusion: la prueba fue aprobada. Aunque `pg-reader2` y `pg-reader3` estaban detenidos, el sistema mantuvo las lecturas disponibles mediante el reader restante en VM5.

## Conclusiones generales

La arquitectura distribuida de ArquiXpress cumple los objetivos de desempeno evaluados para operaciones de lectura. Las pruebas de busqueda y catalogo tuvieron latencias p95 muy por debajo del umbral de 2 segundos.

Tambien se valido tolerancia a fallos parciales:

- Con API1 caida, el Load Balancer mantuvo el servicio disponible.
- Con los readers de VM6 caidos, las lecturas siguieron funcionando mediante el reader de VM5.

Los resultados respaldan que la separacion entre balanceador, APIs y capa de lectura de base de datos aporta continuidad operativa y buen rendimiento para consultas de catalogo.

## Evidencia principal

Resultados destacados:

```text
Busqueda 50 VUs:
p95 = 20.3 ms
errores = 0.00%
throughput = 4,609 req/s

Catalogo con readers 200 VUs:
p95 = 48.16 ms
errores = 0.00%
throughput = 6,329 req/s

Busqueda con API1 caida:
p95 = 19.55 ms
errores = 0.00%
throughput = 4,261 req/s

Catalogo con readers VM6 caidos:
p95 = 12.83 ms
errores = 0.00%
throughput = 5,762 req/s
```
