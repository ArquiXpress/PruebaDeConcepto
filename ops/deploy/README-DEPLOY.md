# Despliegue Distribuido en 6 VMs

Este directorio prepara un despliegue academico de ArquiXpress en 6 maquinas virtuales usando Docker Compose y un unico script orquestador ejecutado desde VM2.

## Distribucion

```text
VM1 Canguro  10.43.98.219   Frontend
VM2 Mono     10.43.100.124  Nginx LB + db-ha-controller + db-read-lb + orquestador
VM3 Burro    10.43.100.129  API 1
VM4 Foca     10.43.99.98    API 2
VM5 M0n0     10.43.97.250   PostgreSQL writer + PostgreSQL reader 1
VM6 Cebra    10.43.101.72   PostgreSQL reader 2 + PostgreSQL reader 3
```

## A. Prerrequisitos por VM

Todas las VMs deben tener Ubuntu Server o equivalente y:

```text
- usuario deploy
- SSH activo
- Docker Engine
- Docker Compose plugin
- git
- rsync
- curl
- nc/netcat
```

En VM2 tambien es util tener cliente PostgreSQL (`psql`) para pruebas manuales.

Validacion basica:

```bash
docker --version
docker compose version
git --version
rsync --version
curl --version
nc -h
```

El usuario `deploy` debe poder ejecutar Docker sin sudo:

```bash
sudo usermod -aG docker deploy
```

Luego cerrar y abrir la sesion.

## B. Configurar SSH desde VM2

En VM2:

```bash
ssh-keygen -t ed25519 -f /home/deploy/.ssh/arquixpress_deploy -C arquixpress-deploy
```

Copiar la llave publica:

```bash
ssh-copy-id -i /home/deploy/.ssh/arquixpress_deploy.pub deploy@10.43.98.219
ssh-copy-id -i /home/deploy/.ssh/arquixpress_deploy.pub deploy@10.43.100.129
ssh-copy-id -i /home/deploy/.ssh/arquixpress_deploy.pub deploy@10.43.99.98
ssh-copy-id -i /home/deploy/.ssh/arquixpress_deploy.pub deploy@10.43.97.250
ssh-copy-id -i /home/deploy/.ssh/arquixpress_deploy.pub deploy@10.43.101.72
```

Registrar hosts:

```bash
ssh-keyscan -H 10.43.98.219 10.43.100.129 10.43.99.98 10.43.97.250 10.43.101.72 >> ~/.ssh/known_hosts
```

Probar:

```bash
ssh -i /home/deploy/.ssh/arquixpress_deploy deploy@10.43.98.219 hostname
ssh -i /home/deploy/.ssh/arquixpress_deploy deploy@10.43.100.129 hostname
ssh -i /home/deploy/.ssh/arquixpress_deploy deploy@10.43.99.98 hostname
ssh -i /home/deploy/.ssh/arquixpress_deploy deploy@10.43.97.250 hostname
ssh -i /home/deploy/.ssh/arquixpress_deploy deploy@10.43.101.72 hostname
```

## C. Puertos Requeridos

```text
VM2 -> todas: 22
Cliente -> VM1: 80
Cliente/frontend -> VM2: 80
VM2 -> VM3: 8080
VM2 -> VM4: 8080
VM3 -> VM2: 15432, 15433
VM4 -> VM2: 15432, 15433
VM2 -> VM5: 5432, 5433
VM2 -> VM6: 5432, 5433
VM5 reader1 -> VM5 writer: 5432
VM6 readers -> VM5 writer: 5432
```

## D. Secuencia de Ejecucion

En VM2, dentro del repositorio:

```bash
cp ops/deploy/inventory.env.example ops/deploy/inventory.env
```

Editar:

```bash
nano ops/deploy/inventory.env
```

Minimo cambiar:

```text
GIT_REPO_URL=https://github.com/<usuario>/<repo>.git
APP_DB_PASSWORD=...
POSTGRES_PASSWORD=...
REPL_PASSWORD=...
```

Ejecutar solamente desde VM2:

```bash
bash ops/deploy/deploy-all.sh
```

No se deben ejecutar scripts manualmente en VM1, VM3, VM4, VM5 o VM6.

## E. Health Checks

Frontend:

```bash
curl -I http://10.43.98.219
```

Nginx API LB:

```bash
curl http://10.43.100.124/health
```

API por Load Balancer:

```bash
curl http://10.43.100.124/actuator/health
curl 'http://10.43.100.124/api/products?page=0&size=1'
```

DB writer endpoint:

```bash
PGPASSWORD="$APP_DB_PASSWORD" psql -h 10.43.100.124 -p 15432 -U "$APP_DB_USER" -d "$APP_DB_NAME" -c "select pg_is_in_recovery();"
```

Debe devolver:

```text
f
```

DB read endpoint:

```bash
PGPASSWORD="$APP_DB_PASSWORD" psql -h 10.43.100.124 -p 15433 -U "$APP_DB_USER" -d "$APP_DB_NAME" -c "select pg_is_in_recovery();"
```

Debe devolver:

```text
t
```

Validar readers concretos:

```bash
PGPASSWORD="$APP_DB_PASSWORD" psql -h 10.43.97.250 -p 5433 -U "$APP_DB_USER" -d "$APP_DB_NAME" -c "select pg_is_in_recovery();"
PGPASSWORD="$APP_DB_PASSWORD" psql -h 10.43.101.72 -p 5432 -U "$APP_DB_USER" -d "$APP_DB_NAME" -c "select pg_is_in_recovery();"
PGPASSWORD="$APP_DB_PASSWORD" psql -h 10.43.101.72 -p 5433 -U "$APP_DB_USER" -d "$APP_DB_NAME" -c "select pg_is_in_recovery();"
```

## F. Pruebas de Caida

### Detener API1

Desde VM2:

```bash
ssh -i "$SSH_KEY" deploy@10.43.100.129 "docker stop arquixpress-api1"
curl http://10.43.100.124/actuator/health
ssh -i "$SSH_KEY" deploy@10.43.100.129 "docker start arquixpress-api1"
```

Esperado: el LB sigue respondiendo por API2.

### Detener API2

```bash
ssh -i "$SSH_KEY" deploy@10.43.99.98 "docker stop arquixpress-api2"
curl http://10.43.100.124/actuator/health
ssh -i "$SSH_KEY" deploy@10.43.99.98 "docker start arquixpress-api2"
```

Esperado: el LB sigue respondiendo por API1.

### Apagar VM6

Esperado:

```text
- caen reader2 y reader3
- writer sigue vivo en VM5
- reader1 sigue vivo en VM5
- el catalogo debe seguir funcionando por db-read-lb usando reader1
```

Prueba:

```bash
curl 'http://10.43.100.124/api/products?page=0&size=1'
```

### Apagar VM5

Esperado:

```text
- cae writer y reader1
- db-ha-controller intenta detener pg-writer por SSH como fencing best-effort
- promueve pg-reader2 en VM6
- si reader2 falla, intenta reader3
- redirige 15432 al nuevo writer
```

Advertencia: esta prueba cambia el cluster. VM5 no debe volver como writer sin resincronizacion.

### Apagar VM2

Esperado:

```text
- se pierde entrada HTTP
- se pierde db-ha-controller
- se pierde db-read-lb
- APIs y DBs pueden seguir vivas, pero el sistema queda inaccesible por el camino normal
```

VM2 es el punto unico de falla aceptado para la POC.

## G. Riesgos y Limitaciones

- VM2 es punto unico de falla.
- El failover PostgreSQL es best-effort academico.
- Puede existir split-brain si VM5 vuelve sin resincronizar despues de promover VM6.
- La replicacion es asincronica; pueden perderse ultimos commits si el writer cae antes de replicarlos.
- `db-read-lb` usa TCP checks; no mide lag de replica.
- `db-ha-controller` no reemplaza Patroni, etcd/Consul, Keepalived, STONITH ni Aurora.
- Para produccion se recomienda Patroni + etcd/Consul, Keepalived/VRRP o servicios administrados como AWS ALB + Aurora.

## Nota Sobre Datasources Spring Boot

El backend actual ya soporta estas variables usadas en Docker local:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
READ_REPLICA_ENABLED
READ_DB_URL
READ_DB_USERNAME
READ_DB_PASSWORD
```

Los compose de API tambien pasan:

```text
SPRING_DATASOURCE_WRITE_URL
SPRING_DATASOURCE_READ_URL
```

Estas variables quedan documentadas para una evolucion futura, pero la separacion funcional actual usa `DB_URL` para escrituras y `READ_DB_URL` para lecturas de catalogo.

## Nota Sobre Frontend

El frontend es Angular y usa llamadas relativas `/api`. No es Vite ni React CRA. Por eso se dejan variables `VITE_API_BASE_URL` y `REACT_APP_API_BASE_URL` solo como compatibilidad documental.

Para VM1 se monta `ops/deploy/frontend-proxy.conf.json` sobre `/app/proxy.conf.json`, apuntando `/api` hacia:

```text
http://10.43.100.124
```

En esta POC academica el frontend se sirve con `ng serve` dentro de Docker, como ya define `frontend/Dockerfile`.
