# ADR-007: Mantener PostgreSQL como base principal en la POC

## Estado

Aprobado

## Contexto

La POC del marketplace requiere una base relacional con:

- transacciones consistentes para checkout;
- control de stock bajo concurrencia;
- soporte para réplica de lectura;
- consultas de catálogo con índices y filtros;
- evolución futura sin perder integridad.

Durante la evaluación arquitectónica surgió la pregunta de si MySQL podría ser una mejor opción que PostgreSQL.

## Decisión

Mantener PostgreSQL como motor principal de la POC.

## Motivos

- PostgreSQL encaja mejor con un dominio donde la consistencia y las operaciones de escritura concurrente son críticas.
- El sistema ya utiliza transacciones, control de stock e idempotencia, por lo que la estabilidad bajo concurrencia pesa más que un benchmark aislado.
- PostgreSQL ofrece mayor margen para consultas avanzadas, tipos de datos y ajustes de rendimiento a medida que el sistema crece.
- Para este caso, MySQL es viable, pero no aporta una ventaja arquitectónica clara frente a PostgreSQL.

## Evidencia de referencia

AWS resume la diferencia principal de forma útil para esta decisión:

- PostgreSQL ofrece más flexibilidad en tipos de datos, escalabilidad, simultaneidad e integridad de datos.
- PostgreSQL es más adecuado para aplicaciones empresariales con escrituras frecuentes y consultas complejas.
- MySQL puede ser una mejor opción cuando predominan lecturas y la aplicación es más simple.

Fuente:

- AWS, "¿Cuál es la diferencia entre MySQL y PostgreSQL?" https://aws.amazon.com/es/compare/the-difference-between-mysql-vs-postgresql/

## Consecuencias

### Positivas

- Se mantiene una base sólida y ya validada por carga.
- Se reduce el riesgo de migración y revalidación completa.
- Se conserva coherencia con la arquitectura actual de checkout, stock y réplica de lectura.

### Negativas

- No se explora en esta iteración una comparación experimental completa contra MySQL.
- Si en el futuro el equipo estandariza MySQL, esta decisión tendría que reevaluarse.

## Cuándo revisar esta decisión

- Si el dominio cambia hacia un perfil fuertemente orientado a lecturas simples.
- Si el equipo necesita estandarizar todo en MySQL por operación o soporte.
- Si aparecen cuellos de botella que PostgreSQL no resuelve con índices, réplicas o particionado.

