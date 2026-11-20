-- =====================================================================
-- V3__descuentos_mayoristas.sql  —  Escala de descuentos mayoristas
-- Ejecutar UNA vez en Aiven, ANTES de desplegar el backend que la usa
-- (después de V1 y V2). Antes, un respaldo rápido de pedido:
--   CREATE TABLE pedido_resp_v3 LIKE pedido; INSERT INTO pedido_resp_v3 SELECT * FROM pedido;
-- =====================================================================

-- 1) Escalones de descuento (los edita el Administrador desde la página)
CREATE TABLE descuento_escala (
  id_escala    INT           NOT NULL AUTO_INCREMENT,
  monto_minimo DECIMAL(12,2) NOT NULL,
  porcentaje   DECIMAL(5,2)  NOT NULL,
  PRIMARY KEY (id_escala),
  UNIQUE KEY uq_descuento_monto (monto_minimo),
  CONSTRAINT chk_descuento_monto CHECK (monto_minimo > 0),
  CONSTRAINT chk_descuento_porcentaje CHECK (porcentaje > 0 AND porcentaje <= 90)
);

-- Escala inicial sugerida (el Administrador puede cambiarla o borrarla desde la web)
INSERT INTO descuento_escala (monto_minimo, porcentaje) VALUES
  (200000, 5),
  (500000, 10),
  (1000000, 15);

-- 2) pedido: guardar subtotal, descuento aplicado y total final
ALTER TABLE pedido
  ADD COLUMN subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
  ADD COLUMN descuento_porcentaje DECIMAL(5,2) NOT NULL DEFAULT 0,
  ADD COLUMN descuento_valor DECIMAL(12,2) NOT NULL DEFAULT 0;

-- Pedidos anteriores: no tenían descuento, así que subtotal = total
SET SQL_SAFE_UPDATES = 0;   -- Workbench bloquea UPDATE sin WHERE (error 1175)
UPDATE pedido SET subtotal = total;
SET SQL_SAFE_UPDATES = 1;

-- Para deshacer:
--   ALTER TABLE pedido DROP COLUMN subtotal, DROP COLUMN descuento_porcentaje, DROP COLUMN descuento_valor;
--   DROP TABLE descuento_escala;
