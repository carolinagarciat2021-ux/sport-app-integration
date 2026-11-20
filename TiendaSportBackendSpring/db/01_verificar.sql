-- =====================================================================
-- 01_verificar.sql  —  SOLO LECTURA. Ejecutar ANTES de la migración.
-- Cada consulta debe devolver 0 filas (salvo las dos primeras, que son informativas).
-- Si alguna devuelve filas, avísame y las corregimos antes de migrar.
-- =====================================================================

-- (informativa) Estructura real de las tablas: pégame este resultado
SHOW CREATE TABLE clientes;
SHOW CREATE TABLE pedido;
SHOW CREATE TABLE detalle_pedido;
SHOW CREATE TABLE producto;
SHOW CREATE TABLE producto_variante;

-- 1) CORREOS REPETIDOS (impiden crear el índice UNIQUE)
SELECT correo, COUNT(*) AS veces, GROUP_CONCAT(id_clientes) AS ids
FROM clientes GROUP BY correo HAVING COUNT(*) > 1;

-- 2) Clientes sin contraseña o sin rol (impiden NOT NULL)
SELECT id_clientes, correo FROM clientes WHERE password IS NULL OR password = '' OR id_rol IS NULL;

-- 3) Roles fuera de 1, 2, 3
SELECT id_clientes, correo, id_rol FROM clientes WHERE id_rol NOT IN (1, 2, 3);

-- 4) Pedidos con un estado distinto a los cinco válidos
SELECT id_pedido, estado FROM pedido
WHERE estado NOT IN ('Pendiente', 'Confirmado', 'Enviado', 'Entregado', 'Cancelado');

-- 5) Productos con precio, costo o stock negativos (impiden los CHECK)
SELECT id_producto, nombre, precio_mayorista, costo_producto, stock FROM producto
WHERE precio_mayorista < 0 OR costo_producto < 0 OR stock < 0;
