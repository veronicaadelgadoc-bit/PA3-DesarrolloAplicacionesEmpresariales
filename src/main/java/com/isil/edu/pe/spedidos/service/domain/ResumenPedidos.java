package com.isil.edu.pe.spedidos.service.domain;

import java.math.BigDecimal;

public record ResumenPedidos(
    long totalPedidos,
    long totalUnidades,
    BigDecimal importeAcumulado,
    BigDecimal ticketPromedio
) {
}
