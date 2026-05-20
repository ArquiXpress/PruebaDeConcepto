# Comparacion: Distribucion en 6 VMs vs Docker Local - 2026-05-20

## Objetivo

Evaluar antes del despliegue si la distribucion propuesta en 6 VMs tiene sentido frente a la arquitectura actual en Docker.

Distribucion propuesta:

```text
VM1: Frontend
VM2: Nginx LB + db-ha-controller + db-read-lb + orquestador
VM3: API1
VM4: API2
VM5: PostgreSQL writer + PostgreSQL reader1
VM6: PostgreSQL reader2 + PostgreSQL reader3
```

Arquitectura Docker actual:

```text
Host Docker:
- frontend
- lb
- db-ha-controller
- db-read-lb
- api-1
- api-2
- postgres
- postgres-replica-1
- postgres-replica-2
- postgres-replica-3
```

Importante: Docker local permite simular grupos de servicios, pero no reproduce aislamiento fisico real de CPU, RAM, disco y red entre VMs.

## Mapeo Usado para Simular VMs

```text
VM2 simulada:
- lb
- db-ha-controller
- db-read-lb

VM5 simulada:
- postgres
- postgres-replica-1

VM6 simulada:
- postgres-replica-2
- postgres-replica-3
```

## Estado Base

Antes de fallas:

```powershell
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod 'http://localhost:8080/api/products?page=0&size=1'
```

Resultado:

```text
lb: up
db-ha-controller: healthy
db-read-lb: healthy
api-1: up
api-2: up
postgres: healthy
postgres-replica-1: healthy
postgres-replica-2: healthy
postgres-replica-3: healthy

/actuator/health: UP
/api/products: responde correctamente
```

## Prueba 1: Caida Simulada de VM6

Se detuvieron:

```powershell
docker compose stop postgres-replica-2 postgres-replica-3
```

Esto simula:

```text
VM6 caida:
- reader2 caida
- reader3 caida
```

Queda disponible:

```text
VM5:
- writer
- reader1
```

Verificacion:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod 'http://localhost:8080/api/products?page=0&size=1'
```

Resultado:

```text
HEALTH=UP
PRODUCT_TOTAL=1
```

Luego se restauraron:

```powershell
docker compose start postgres-replica-2 postgres-replica-3
```

Resultado final:

```text
postgres-replica-1: healthy
postgres-replica-2: healthy
postgres-replica-3: healthy
db-read-lb: healthy
```

### Conclusion de Prueba 1

La distribucion propuesta tolera la caida de VM6 para lecturas, porque `reader1` queda vivo en VM5.

Esto es mejor que la alternativa:

```text
VM5: writer
VM6: reader1 + reader2 + reader3
```

En esa alternativa, si cae VM6 se pierden todas las replicas de lectura.

## Prueba 2: Caida Simulada de VM2

Se detuvieron:

```powershell
docker compose stop lb db-ha-controller db-read-lb
```

Esto simula:

```text
VM2 caida:
- Nginx LB caido
- db-ha-controller caido
- db-read-lb caido
- orquestador inaccesible
```

Verificacion desde el cliente normal:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Resultado:

```text
FAILED: No es posible conectar con el servidor remoto
```

Verificacion interna directa hacia API1:

```powershell
docker run --rm --network pruebadeconcepto_default curlimages/curl:8.10.1 \
  -s -o /dev/null -w "%{http_code}" http://api-1:8080/actuator/health
```

Resultado:

```text
API1_INTERNAL_HEALTH_HTTP=503
```

Luego se restauraron:

```powershell
docker compose start db-ha-controller db-read-lb lb
```

Resultado final:

```text
/actuator/health: UP
```

### Conclusion de Prueba 2

VM2 es un punto critico.

Si cae VM2, se pierde:

```text
- entrada HTTP principal
- balanceo hacia API1/API2
- endpoint estable de escritura
- endpoint estable de lectura
- capacidad automatica de failover DB
```

Aunque API1, API2 y las DB sigan vivas, el sistema deja de ser usable por el cliente normal.

Esto confirma que tener Nginx LB, `db-ha-controller` y `db-read-lb` en el mismo nodo es aceptable para POC, pero no es alta disponibilidad completa.

## Prueba 3: Caida Simulada de VM5

No se ejecuto en esta corrida porque VM5 contiene:

```text
- writer
- reader1
```

Detener `postgres` dispararia promocion real de `postgres-replica-2` o `postgres-replica-3`.

Eso cambia el cluster local a modo failover:

```text
postgres-replica-2 o postgres-replica-3 pasa a writer
postgres viejo no puede volver automaticamente como writer
```

Para no dejar el ambiente local en estado divergente, esta prueba debe ejecutarse en:

- una corrida aislada;
- volumenes temporales;
- o directamente en las VMs cuando ya se acepte probar failover real.

Resultado esperado por diseno:

```text
VM5 cae:
- se pierde writer
- se pierde reader1
- quedan reader2 y reader3 en VM6
- db-ha-controller en VM2 promueve una de las replicas de VM6
- escrituras pasan al nuevo writer en VM6
```

Riesgo posterior:

```text
VM5 no puede volver como writer viejo.
Debe reconstruirse como replica del nuevo writer.
```

## Comparacion Contra Docker Local Actual

### Docker actual

Ventajas:

- facil de correr;
- todos los componentes se levantan con `docker compose`;
- sirve para probar logica de rutas, replicas, balanceo y scripts;
- permite simular fallas por grupos de contenedores.

Desventajas:

- todos comparten la misma maquina fisica;
- si cae el host Docker, cae todo;
- las pruebas de rendimiento de replicas no representan VMs reales;
- no hay aislamiento real de disco/red entre writer y readers.

### Distribucion en 6 VMs

Ventajas:

- separa frontend, APIs y DBs;
- permite falla parcial real;
- writer no comparte VM con `db-ha-controller`;
- readers quedan repartidas entre VM5 y VM6;
- si cae VM6, aun queda reader1;
- si cae VM5, quedan replicas promovibles en VM6.

Desventajas:

- VM2 queda como punto critico;
- sin una segunda VM de LB/controller no hay HA completa del plano de entrada/control;
- requiere orquestacion por SSH desde un unico script;
- requiere configuracion de red, firewall y credenciales SSH entre VMs.

## Veredicto

La distribucion propuesta es mejor que Docker local para despliegue real porque introduce separacion fisica:

```text
VM5: writer + reader1
VM6: reader2 + reader3
```

La prueba de caida de VM6 confirma que la decision de poner `reader1` junto al writer aporta continuidad de lectura cuando se pierde VM6.

Pero VM2 sigue siendo un punto unico de falla:

```text
VM2: Nginx LB + db-ha-controller + db-read-lb
```

Esto no invalida la arquitectura para POC, pero debe declararse como trade-off.

## Recomendacion Final

Usar esta distribucion:

```text
VM1: Frontend
VM2: Nginx LB + db-ha-controller + db-read-lb + orquestador
VM3: API1
VM4: API2
VM5: PostgreSQL writer + PostgreSQL reader1
VM6: PostgreSQL reader2 + PostgreSQL reader3
```

Defensa tecnica:

```text
Con 6 VMs se prioriza separar el writer del controlador HA y mantener replicas promovibles fuera del nodo writer.
VM2 queda como punto critico aceptado por restriccion de infraestructura.
En produccion se duplicaria VM2 con Keepalived/VRRP, DNS failover o un balanceador administrado.
```

## Mitigacion Productiva de VM2

Para eliminar el punto unico de falla de VM2 se necesitara una de estas opciones:

```text
1. Dos VMs de LB/controller + Keepalived/VRRP.
2. Balanceador administrado externo.
3. DNS failover.
4. Aurora/RDS endpoints para reemplazar db-ha-controller y db-read-lb.
```

Con solo 6 VMs y sin IP flotante externa, el punto critico de entrada/control no desaparece: solo se mueve.
