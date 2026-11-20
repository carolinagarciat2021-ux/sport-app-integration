-- =====================================================================
-- V7__envio_y_linea_de_tiempo.sql  —  Transportadora, línea de tiempo de pedidos y correo de avisos
-- Ejecutar UNA vez en Aiven (después de V6) y ANTES de arrancar el backend nuevo.
-- Si Workbench protesta por el modo seguro (error 1175), ya está controlado dentro del script.
-- =====================================================================

-- 1) Empresa de reparto y número de guía de cada pedido (se llenan al pasarlo a "Enviado")
ALTER TABLE pedido
  ADD COLUMN transportadora VARCHAR(80) NULL,
  ADD COLUMN numero_guia    VARCHAR(60) NULL;

-- 2) Línea de tiempo: una fila cada vez que un pedido cambia de estado
CREATE TABLE pedido_estado_historial (
  id_historial BIGINT       NOT NULL AUTO_INCREMENT,
  id_pedido    INT          NOT NULL,
  estado       VARCHAR(20)  NOT NULL,
  fecha        DATETIME     NOT NULL,
  por_usuario  VARCHAR(150) NULL,
  PRIMARY KEY (id_historial),
  INDEX idx_historial_pedido (id_pedido)
);

-- Pedidos que ya existen: una fila con su estado actual y su fecha, para que la línea de tiempo no salga vacía
SET SQL_SAFE_UPDATES = 0;
INSERT INTO pedido_estado_historial (id_pedido, estado, fecha, por_usuario)
SELECT id_pedido, estado, fecha, NULL FROM pedido;
SET SQL_SAFE_UPDATES = 1;

-- 3) Correo del administrador que recibe un aviso por cada pedido nuevo
ALTER TABLE datos_empresa ADD COLUMN correo_avisos VARCHAR(150) NULL;
SET SQL_SAFE_UPDATES = 0;
UPDATE datos_empresa SET correo_avisos = 'carolinagarciat2021@gmail.com' WHERE id_empresa = 1;
SET SQL_SAFE_UPDATES = 1;

-- Para deshacer:
--   ALTER TABLE pedido DROP COLUMN transportadora, DROP COLUMN numero_guia;
--   DROP TABLE pedido_estado_historial;
--   ALTER TABLE datos_empresa DROP COLUMN correo_avisos;
