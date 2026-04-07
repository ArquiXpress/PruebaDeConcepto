# Pruebas de disponibilidad y persistencia - 2026-04-06

Este reporte cubre las pruebas faltantes para justificar la arquitectura actual en tres dimensiones:

1. failover de base de datos primaria;
2. comportamiento con réplica desincronizada;
3. health checks y recuperación operativa.

## 1) Failover de base de datos primaria

### Escenario

Se detuvo el primario PostgreSQL con:

```powershell
docker compose stop postgres
```

Luego se consulto el estado de salud de la aplicacion a traves del load balancer.

### Resultado

- `http://localhost:8080/actuator/health` respondio `503 Service Unavailable`.
- En logs de `api-1` aparecio un error de conexion a PostgreSQL:
  - `HikariPool-1 - Connection is not available, request timed out`
  - `UnknownHostException: postgres`

### Interpretacion

- La arquitectura actual **no hace failover automatico de la base de datos primaria**.
- Si el primario cae, la aplicacion queda degradada y el LB no puede compensar esa falla.
- Esto confirma que PostgreSQL primario sigue siendo un punto unico de falla para escrituras y para las lecturas que dependen de JPA/Flyway.

## 2) Comportamiento con replica desincronizada

### Escenario

Se reanudo el primario y luego se pauso la reproduccion WAL en la replica con:

```powershell
docker compose exec -T postgres-replica psql -U arquixpress -d arquixpress -c "select pg_wal_replay_pause();"
```

Despues se actualizo el primario:

```sql
update product
   set title = 'Laptop ArquiXpress Pro - ACTUALIZADO'
 where id = '11111111-1111-1111-1111-111111111111';
```

Luego se consulto el detalle del producto a traves de la API:

```powershell
Invoke-RestMethod http://localhost:8080/api/products/11111111-1111-1111-1111-111111111111
```

### Resultado

- La API respondio con el valor anterior:
  - `title = "Laptop ArquiXpress Pro"`
- Luego de reanudar la replica:

```powershell
docker compose exec -T postgres-replica psql -U arquixpress -d arquixpress -c "select pg_wal_replay_resume();"
```

- La misma consulta ya devolvio:
  - `title = "Laptop ArquiXpress Pro - ACTUALIZADO"`

### Interpretacion

- La replica de lectura funciona y puede devolver datos obsoletos si esta atrasada.
- Eso valida el riesgo de consistencia eventual en el catalogo.
- El sistema tolera ese retraso en lectura, pero la decisión arquitectonica debe asumirlo explícitamente.

## 3) Health checks y recuperacion operativa

### Escenario

Tras detener el primario, se volvio a levantar con:

```powershell
docker compose up -d postgres
```

Luego se espero a que el sistema recuperara su estado estable y se consulto health otra vez.

### Resultado

- La aplicacion volvio a responder `UP` en:
  - `http://localhost:8080/actuator/health`
- El flujo completo de catalogo volvio a servir datos despues de la recuperacion del primario.

### Interpretacion

- El sistema no tiene failover transparente de BD, pero sí recupera servicio cuando el primario vuelve.
- Los health checks de la app y del LB reflejan la indisponibilidad y posterior recuperacion.
- La arquitectura requiere una estrategia externa de alta disponibilidad si se desea tolerancia real a caída del primario.

## Conclusion

Estas pruebas completan el vacio principal de disponibilidad y persistencia.

La lectura arquitectonica queda así:

- **Backend**: tolera caída parcial por duplicación y LB.
- **Catalogo**: puede leer desde replica, pero acepta consistencia eventual.
- **Base de datos primaria**: sigue siendo un punto critico de disponibilidad.
- **Recuperacion**: existe al volver a levantar el primario, pero no hay failover automatico de BD.

Por lo tanto, la arquitectura actual queda mejor justificada para la POC, pero con una limitacion importante y ahora demostrada:

- el sistema no es resiliente ante caída del primario PostgreSQL sin una capa adicional de HA.
