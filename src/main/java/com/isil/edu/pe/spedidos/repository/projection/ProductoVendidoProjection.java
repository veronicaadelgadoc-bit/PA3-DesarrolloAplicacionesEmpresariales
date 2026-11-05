package com.isil.edu.pe.spedidos.repository.projection;

import java.math.BigDecimal;

/**
 * Resultado consolidado de ventas por producto.
 */
public record ProductoVendidoProjection(
    String productoCodigo,
    String productoNombre,
    long totalPedidos,
    long totalUnidades,
    BigDecimal importeAcumulado
) {
}
