-- =====================================================================
-- V2__registro_actividad.sql  —  Historial de actividad (auditoría)
-- Ejecutar UNA vez en Aiven, ANTES de desplegar el backend que lo usa
-- (con ddl-auto=validate, si la tabla no existe el backend no arranca).
-- Sin llaves foráneas a propósito: el registro debe sobrevivir aunque se
-- elimine el pedido, el producto o el usuario al que se refiere.
-- =====================================================================

CREATE TABLE registro_actividad (
  id_registro     BIGINT       NOT NULL AUTO_INCREMENT,
  fecha           DATETIME     NOT NULL,
  id_usuario      INT          NULL,
  correo_usuario  VARCHAR(150) NULL,
  rol_usuario     VARCHAR(20)  NULL,
  accion          VARCHAR(40)  NOT NULL,
  entidad         VARCHAR(30)  NULL,
  id_entidad      INT          NULL,
  detalle         VARCHAR(1000) NULL,
  PRIMARY KEY (id_registro),
  INDEX idx_actividad_fecha (fecha),
  INDEX idx_actividad_usuario (id_usuario),
  INDEX idx_actividad_accion (accion)
);

-- OPCIONAL (protección extra): impedir que alguien edite o borre registros, incluso con acceso
-- directo a la base de datos. Si tu usuario de Aiven no tiene permiso para crear triggers, omítelo.
-- DELIMITER //
-- CREATE TRIGGER registro_actividad_no_update BEFORE UPDATE ON registro_actividad
--   FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El historial de actividad no se puede modificar';
-- CREATE TRIGGER registro_actividad_no_delete BEFORE DELETE ON registro_actividad
--   FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El historial de actividad no se puede borrar';
-- //
-- DELIMITER ;

-- Para deshacer: DROP TABLE registro_actividad;
