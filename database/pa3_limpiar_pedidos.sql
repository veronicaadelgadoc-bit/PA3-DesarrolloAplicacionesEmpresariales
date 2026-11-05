-- =====================================================================
-- PA3 - (Opcional) Dejar la tabla pedido vacía antes de grabar las evidencias,
-- para que los resultados coincidan exactamente con la guía.
-- ATENCIÓN: borra todos los pedidos de pedidosdb.
-- No modifica el stock de inventario-service (SQL Server).
-- =====================================================================
USE pedidosdb;
DELETE FROM pedido;
ALTER TABLE pedido AUTO_INCREMENT = 1;
