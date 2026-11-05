-- =====================================================================
-- PA3 - Consultas de verificación en MySQL (evidencia "coincide con MySQL")
-- Ejecutar en MySQL Workbench sobre la base pedidosdb DESPUÉS de registrar
-- los pedidos del escenario (ver docs/GUIA_EVIDENCIAS_PA3.md).
-- Son las mismas sentencias que ejecuta PedidoReporteJdbcRepository, con los
-- parámetros escritos a mano solo para la comprobación manual.
-- =====================================================================
USE pedidosdb;

-- 0. Todos los pedidos registrados
SELECT id, cliente, producto_codigo, cantidad, total, fecha
FROM pedido
ORDER BY id;

-- 1. Caso 1 - Búsqueda avanzada (cliente contiene 'Ana' y total >= 1000)
SELECT id, cliente, producto_codigo, producto_nombre, precio_unitario, cantidad, total, fecha
FROM pedido
WHERE LOWER(cliente) LIKE LOWER('%Ana%') ESCAPE '!'
  AND total >= 1000
ORDER BY id DESC;

-- 2. Caso 2 - Clientes frecuentes (minPedidos = 2)
SELECT
    cliente,
    COUNT(*)              AS total_pedidos,
    SUM(cantidad)         AS total_unidades,
    SUM(total)            AS importe_acumulado,
    ROUND(AVG(total), 2)  AS ticket_promedio
FROM pedido
GROUP BY cliente
HAVING COUNT(*) >= 2
ORDER BY total_pedidos DESC, importe_acumulado DESC, cliente ASC;

-- 3. Resumen general (lo muestra la tarjeta "Resumen general" de la JSP)
SELECT COUNT(*) AS total_pedidos,
       COALESCE(SUM(cantidad), 0) AS total_unidades,
       COALESCE(SUM(total), 0)    AS importe_acumulado,
       COALESCE(AVG(total), 0)    AS ticket_promedio
FROM pedido;
