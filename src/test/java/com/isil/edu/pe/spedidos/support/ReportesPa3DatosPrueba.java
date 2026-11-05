package com.isil.edu.pe.spedidos.support;

import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * [PA3] Datos de prueba compartidos por las pruebas de reportes.
 *
 * <p>Escenario (mismo que el de las evidencias manuales):</p>
 * <pre>
 * # | cliente     | producto | cant | total
 * 1 | Ana Torres  | MON-001  |  2   | 1700.00
 * 2 | Ana Torres  | TEC-001  |  1   |  120.00
 * 3 | Luis Ramos  | LAP-001  |  1   | 2500.00
 * 4 | Ana Torres  | LAP-001  |  1   | 2500.00
 * 5 | Luis Ramos  | TEC-001  |  3   |  360.00
 * 6 | Carla Diaz  | MON-001  |  1   |  850.00
 * </pre>
 *
 * <p>Resultados esperados:</p>
 * <ul>
 *   <li>cliente="ana", totalMinimo=1000 → pedidos 4 y 1 (en ese orden).</li>
 *   <li>minPedidos=2 → Ana Torres (3 pedidos, 4 uds, 4320.00, 1440.00) y
 *       Luis Ramos (2 pedidos, 4 uds, 2860.00, 1430.00); Carla Diaz no aparece.</li>
 * </ul>
 */
public final class ReportesPa3DatosPrueba {

  public static final int TOTAL_PEDIDOS = 6;

  private ReportesPa3DatosPrueba() {
  }

  public static void cargar(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.update("DELETE FROM pedido");
    insertar(jdbcTemplate, "Ana Torres", "MON-001", "Monitor", "850.00", 2, "1700.00");
    insertar(jdbcTemplate, "Ana Torres", "TEC-001", "Teclado", "120.00", 1, "120.00");
    insertar(jdbcTemplate, "Luis Ramos", "LAP-001", "Laptop", "2500.00", 1, "2500.00");
    insertar(jdbcTemplate, "Ana Torres", "LAP-001", "Laptop", "2500.00", 1, "2500.00");
    insertar(jdbcTemplate, "Luis Ramos", "TEC-001", "Teclado", "120.00", 3, "360.00");
    insertar(jdbcTemplate, "Carla Diaz", "MON-001", "Monitor", "850.00", 1, "850.00");
  }

  private static void insertar(JdbcTemplate jdbcTemplate, String cliente, String codigo,
      String nombre, String precio, int cantidad, String total) {

    jdbcTemplate.update(
        """
        INSERT INTO pedido (cliente, producto_codigo, producto_nombre,
                            precio_unitario, cantidad, total, fecha)
        VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """,
        cliente, codigo, nombre, new BigDecimal(precio), cantidad, new BigDecimal(total));
  }
}
