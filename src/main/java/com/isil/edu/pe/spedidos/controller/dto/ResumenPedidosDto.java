package com.isil.edu.pe.spedidos.controller.dto;

import com.isil.edu.pe.spedidos.service.domain.ResumenPedidos;
import java.math.BigDecimal;

public record ResumenPedidosDto(
    long totalPedidos,
    long totalUnidades,
    BigDecimal importeAcumulado,
    BigDecimal ticketPromedio
) {

  public static ResumenPedidosDto from(ResumenPedidos resumen) {
    return new ResumenPedidosDto(
        resumen.totalPedidos(),
        resumen.totalUnidades(),
        resumen.importeAcumulado(),
        resumen.ticketPromedio()
    );
  }

}
