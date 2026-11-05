package com.isil.edu.pe.spedidos.repository.projection;

import java.math.BigDecimal;

/**
 * [PA3] Proyección de una fila agregada de la consulta de clientes frecuentes.
 *
 * <p>El resultado no corresponde a una entidad {@code Pedido} sino a un
 * consolidado calculado con {@code GROUP BY cliente ... HAVING COUNT(*) >= :minPedidos};
 * por eso no se reutiliza {@code PedidoRowMapper} y se define este record.</p>
 *
 * @param cliente          nombre del cliente agrupado
 * @param totalPedidos     COUNT(*) de pedidos del cliente
 * @param totalUnidades    SUM(cantidad)
 * @param importeAcumulado SUM(total)
 * @param ticketPromedio   AVG(total), redondeado a 2 decimales
 */
public record ClienteFrecuenteProjection(
    String cliente,
    long totalPedidos,
    long totalUnidades,
    BigDecimal importeAcumulado,
    BigDecimal ticketPromedio
) {
}
