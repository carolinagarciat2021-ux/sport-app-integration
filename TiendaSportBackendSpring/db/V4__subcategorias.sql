-- =====================================================================
-- V4__subcategorias.sql  —  Tipos de prenda dentro de cada categoría
-- Ejecutar UNA vez en Aiven (después de V1, V2 y V3) y ANTES de arrancar
-- el backend nuevo. Si Workbench protesta por el modo seguro, no aplica aquí
-- (no hay UPDATE ni DELETE sin WHERE).
-- =====================================================================

CREATE TABLE subcategoria (
  id_subcategoria INT          NOT NULL AUTO_INCREMENT,
  id_categoria    INT          NOT NULL,
  nombre          VARCHAR(80)  NOT NULL,
  imagen_url      VARCHAR(500) NULL,
  PRIMARY KEY (id_subcategoria),
  UNIQUE KEY uq_subcategoria_nombre (id_categoria, nombre)
);

ALTER TABLE producto
  ADD COLUMN id_subcategoria INT NULL,
  ADD INDEX idx_producto_subcategoria (id_subcategoria),
  ADD CONSTRAINT fk_producto_subcategoria FOREIGN KEY (id_subcategoria)
      REFERENCES subcategoria (id_subcategoria) ON DELETE SET NULL;

-- Para deshacer:
--   ALTER TABLE producto DROP FOREIGN KEY fk_producto_subcategoria, DROP COLUMN id_subcategoria;
--   DROP TABLE subcategoria;
