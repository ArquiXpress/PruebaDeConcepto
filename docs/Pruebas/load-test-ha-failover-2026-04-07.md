# Prueba de alta disponibilidad con arbitro y promocion automatica - 2026-04-07

## Objetivo

Validar que la arquitectura pueda recuperarse ante la caida del primario PostgreSQL mediante:

- un router TCP de base de datos;
- un arbitro de consenso/promocion;
- una replica PostgreSQL capaz de asumir el rol primario.

## Escenario

Infraestructura activa:

- `api-1` y `api-2` detras de Nginx.
- `db-router` exponiendo el acceso a base de datos para la aplicacion.
- `db-arbiter` monitoreando el estado del primario.
- `postgres` como primario.
- `postgres-replica` como standby.

## Prueba ejecutada

### 1) Caida del primario

Se detuvo el primario con:

```powershell
docker compose stop postgres
```

### 2) Verificacion de recuperacion

Se consulto la salud de la aplicacion:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Resultado:

- la aplicacion siguio respondiendo `UP`;
- el router y el arbitro mantuvieron el servicio disponible;
- la replica fue promovida.

### 3) Verificacion tecnica de promocion

Se ejecuto en la replica:

```sql
select pg_is_in_recovery();
```

Resultado:

- `f`

Interpretacion:

- la replica dejo de estar en modo recovery;
- por lo tanto, asumio el rol de primario.

## Hallazgos

- La arquitectura ya no depende exclusivamente del primario original para seguir atendiendo la aplicacion.
- La promocion automatica funciono a nivel de POC.
- El sistema sigue teniendo consistencia eventual en lecturas desde replica cuando se usa el canal de lectura separado.

## Conclusion

Esta ultima prueba de 2026-04-07 confirma una mejora concreta frente a la arquitectura anterior:

- antes, la caida del primario dejaba la aplicacion no disponible;
- ahora, el arbitro promueve la replica y la aplicacion sigue operativa.

La solucion sigue siendo una POC y todavia requiere endurecimiento operativo, pero ya demuestra una estrategia real de failover automatico controlado.
