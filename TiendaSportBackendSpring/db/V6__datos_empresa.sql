-- =====================================================================
-- V6__datos_empresa.sql  —  Datos de la empresa editables por el Administrador
-- Ejecutar UNA vez en Aiven (después de V5) y ANTES de arrancar el backend nuevo.
-- Hay una sola fila (id 1). Los valores iniciales son los que me diste.
-- =====================================================================

CREATE TABLE datos_empresa (
  id_empresa         INT           NOT NULL,
  nombre             VARCHAR(80)   NOT NULL,
  lema               VARCHAR(150)  NOT NULL,
  whatsapp           VARCHAR(20)   NOT NULL,
  mensaje_whatsapp   VARCHAR(200)  NULL,
  correo             VARCHAR(150)  NOT NULL,
  direccion          VARCHAR(200)  NULL,
  horario            VARCHAR(200)  NULL,
  tienda_virtual     VARCHAR(200)  NULL,
  instagram          VARCHAR(300)  NULL,
  facebook           VARCHAR(300)  NULL,
  envio_gratis_desde DECIMAL(12,2) NOT NULL DEFAULT 0,
  PRIMARY KEY (id_empresa)
) DEFAULT CHARSET = utf8mb4;

INSERT INTO datos_empresa
  (id_empresa, nombre, lema, whatsapp, mensaje_whatsapp, correo, direccion, horario, tienda_virtual, instagram, facebook, envio_gratis_desde)
VALUES
  (1, 'Tienda Sport', 'Ropa deportiva para toda la familia', '573127622295',
   'Hola, tengo una duda sobre un producto de Tienda Sport.', 'tiendasport@gmail.com',
   'Carrera 108 N° 60-61', 'Atención al cliente: horario de oficina', 'Tienda virtual: abierta las 24 horas',
   '', '', 500000);

-- Para deshacer: DROP TABLE datos_empresa;
