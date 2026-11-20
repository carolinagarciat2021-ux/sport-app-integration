-- =====================================================================
-- V5__imagenes_producto.sql  —  Fotos de productos subidas desde el computador
-- Ejecutar UNA vez en Aiven (después de V4) y ANTES de arrancar el backend nuevo.
-- Las fotos se guardan en la base de datos (no en el disco de Render, que se borra).
-- =====================================================================

CREATE TABLE imagen_producto (
  id_imagen      BIGINT      NOT NULL AUTO_INCREMENT,
  tipo_contenido VARCHAR(50) NOT NULL,
  datos          MEDIUMBLOB  NOT NULL,
  fecha          DATETIME    NOT NULL,
  PRIMARY KEY (id_imagen)
);

-- Para deshacer: DROP TABLE imagen_producto;
