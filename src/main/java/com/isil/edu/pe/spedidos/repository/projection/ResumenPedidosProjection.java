package com.isil.edu.pe.spedidos.repository.projection;

import java.math.BigDecimal;

/**
 * Resultado obtenido directamente desde una consulta
 * agregada ejecutada mediante Spring JDBC.
 */
public record ResumenPedidosProjection(
    long totalPedidos,
    long totalUnidades,
    BigDecimal importeAcumulado,
    BigDecimal ticketPromedio
) {
}
