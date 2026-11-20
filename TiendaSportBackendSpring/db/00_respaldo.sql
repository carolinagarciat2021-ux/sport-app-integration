-- =====================================================================
-- 00_respaldo.sql  —  RESPALDO antes de migrar (Fase 1)
-- Ejecutar en MySQL Workbench conectado a la base de Aiven.
-- Copia las tablas que se van a tocar a tablas *_resp_20261116.
-- Se usa CREATE TABLE ... LIKE (conserva la llave primaria; Aiven exige
-- llave primaria en todas las tablas) y luego INSERT ... SELECT.
-- Si ya existen, bórralas primero (DROP TABLE) o cambia la fecha del sufijo.
-- =====================================================================

CREATE TABLE clientes_resp_20261116       LIKE clientes;
INSERT INTO clientes_resp_20261116        SELECT * FROM clientes;

CREATE TABLE pedido_resp_20261116         LIKE pedido;
INSERT INTO pedido_resp_20261116          SELECT * FROM pedido;

CREATE TABLE detalle_pedido_resp_20261116 LIKE detalle_pedido;
INSERT INTO detalle_pedido_resp_20261116  SELECT * FROM detalle_pedido;

CREATE TABLE producto_resp_20261116       LIKE producto;
INSERT INTO producto_resp_20261116        SELECT * FROM producto;

-- Comprobación: cada par debe dar el mismo número de filas
SELECT 'clientes' tabla, (SELECT COUNT(*) FROM clientes) original, (SELECT COUNT(*) FROM clientes_resp_20261116) respaldo
UNION ALL SELECT 'pedido', (SELECT COUNT(*) FROM pedido), (SELECT COUNT(*) FROM pedido_resp_20261116)
UNION ALL SELECT 'detalle_pedido', (SELECT COUNT(*) FROM detalle_pedido), (SELECT COUNT(*) FROM detalle_pedido_resp_20261116)
UNION ALL SELECT 'producto', (SELECT COUNT(*) FROM producto), (SELECT COUNT(*) FROM producto_resp_20261116);

-- Respaldo completo alternativo (desde la consola de Windows, con mysqldump):
--   mysqldump -h HOST -P PUERTO -u USUARIO -p --ssl-mode=REQUIRED --set-gtid-purged=OFF NOMBRE_BD > respaldo_tiendasport.sql
-- (guarda ese archivo FUERA del repositorio de Git)
