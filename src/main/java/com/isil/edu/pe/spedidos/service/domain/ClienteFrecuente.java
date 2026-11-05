package com.isil.edu.pe.spedidos.service.domain;

import java.math.BigDecimal;

/**
 * [PA3] Modelo de servicio para el reporte de clientes frecuentes.
 *
 * <p>Desacopla a los controladores de la proyección SQL del repositorio
 * ({@code ClienteFrecuenteProjection}), igual que {@link ResumenPedidos} y
 * {@link ProductoVendido} en el proyecto base.</p>
 */
public record ClienteFrecuente(
    String cliente,
    long totalPedidos,
    long totalUnidades,
    BigDecimal importeAcumulado,
    BigDecimal ticketPromedio
) {
}
