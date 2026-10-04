-- Setup desde cero de la base `factura_arca` (TP FacturaArca).
--
-- Uso (RESETEA TODA LA DB):
--   1) Crear la base si no existe, como usuario postgres:
--        CREATE DATABASE factura_arca;
--   2) Conectado a factura_arca, ejecutar este script
--      (pgAdmin > Query Tool, o: psql -U postgres -d factura_arca -f setup_desde_cero.sql).
--   3) Ejecutar la app
--        - Hibernate (ddl-auto=update) crea las 14 tablas en el esquema `model`.
--        - DataSeeder carga los datos de prueba si las tablas estan vacias.
--
-- Re-ejecutar este script borra TODO el contenido del esquema `model` y lo deja
-- listo para una carga fresca.

DROP SCHEMA IF EXISTS model CASCADE;
CREATE SCHEMA model;
