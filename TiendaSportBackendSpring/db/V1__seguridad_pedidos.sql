-- =====================================================================
-- V1__seguridad_pedidos.sql  —  Migración Fase 1 (MySQL 8 / Aiven)
-- Orden: 00_respaldo.sql -> 01_verificar.sql (todo en 0) -> este script.
-- Ejecutar UNA SOLA VEZ y ANTES de desplegar el backend nuevo
-- (el backend usa ddl-auto=validate y exige estas columnas).
-- =====================================================================

-- ---- 1) pedido: total y dirección de entrega ----
ALTER TABLE pedido
  ADD COLUMN total DECIMAL(12,2) NOT NULL DEFAULT 0,
  ADD COLUMN direccion_entrega VARCHAR(150) NULL;

-- Pedidos ya existentes: total = suma de sus detalles; dirección = la actual del cliente
SET SQL_SAFE_UPDATES = 0;   -- Workbench bloquea UPDATE sin WHERE (error 1175)
UPDATE pedido p
JOIN (SELECT id_pedido, SUM(precio_unitario * cantidad) AS suma
      FROM detalle_pedido GROUP BY id_pedido) d ON d.id_pedido = p.id_pedido
SET p.total = d.suma;

UPDATE pedido p
JOIN clientes c ON c.id_clientes = p.id_cliente
SET p.direccion_entrega = c.direccion;
SET SQL_SAFE_UPDATES = 1;

-- ---- 2) clientes: correo único ----
ALTER TABLE clientes ADD CONSTRAINT uq_clientes_correo UNIQUE (correo);

-- ---- 3) clientes: password e id_rol obligatorios ----
-- OJO: confirma con SHOW CREATE TABLE (01_verificar.sql) que id_rol es INT y
-- password VARCHAR(255) antes de ejecutar estas dos líneas; si el tipo es otro,
-- avísame para ajustarlas.
ALTER TABLE clientes MODIFY password VARCHAR(255) NOT NULL;
ALTER TABLE clientes MODIFY id_rol INT NOT NULL;

-- ---- 4) producto: sin precios ni stock negativos ----
ALTER TABLE producto
  ADD CONSTRAINT chk_producto_precio_mayorista CHECK (precio_mayorista >= 0),
  ADD CONSTRAINT chk_producto_costo CHECK (costo_producto >= 0),
  ADD CONSTRAINT chk_producto_stock CHECK (stock >= 0);

-- ---- Para deshacer (solo si algo sale mal) ----
-- ALTER TABLE pedido DROP COLUMN total, DROP COLUMN direccion_entrega;
-- ALTER TABLE clientes DROP INDEX uq_clientes_correo;
-- ALTER TABLE producto DROP CHECK chk_producto_precio_mayorista, DROP CHECK chk_producto_costo, DROP CHECK chk_producto_stock;
-- (o restaurar desde las tablas *_resp_20261116)
