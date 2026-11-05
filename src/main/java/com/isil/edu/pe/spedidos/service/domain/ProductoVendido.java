package com.isil.edu.pe.spedidos.service.domain;

import java.math.BigDecimal;

public record ProductoVendido(
    String productoCodigo,
    String productoNombre,
    long totalPedidos,
    long totalUnidades,
    BigDecimal importeAcumulado
) {
}

