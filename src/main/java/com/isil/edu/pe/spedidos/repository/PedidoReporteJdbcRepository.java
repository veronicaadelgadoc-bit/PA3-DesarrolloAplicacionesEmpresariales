package com.isil.edu.pe.spedidos.repository;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.repository.mapper.PedidoRowMapper;
import com.isil.edu.pe.spedidos.repository.projection.ClienteFrecuenteProjection;
import com.isil.edu.pe.spedidos.repository.projection.ProductoVendidoProjection;
import com.isil.edu.pe.spedidos.repository.projection.ResumenPedidosProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de consultas y reportes implementado
 * mediante Spring JDBC.
 */
@Repository
public class PedidoReporteJdbcRepository {

  private static final String COLUMNAS_PEDIDO = """
      id,
      cliente,
      producto_codigo,
      producto_nombre,
      precio_unitario,
      cantidad,
      total,
      fecha
      """;

  private final JdbcTemplate jdbcTemplate;

  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  private final PedidoRowMapper pedidoRowMapper;

  public PedidoReporteJdbcRepository(JdbcTemplate jdbcTemplate,
      NamedParameterJdbcTemplate namedParameterJdbcTemplate,
      PedidoRowMapper pedidoRowMapper) {

    this.jdbcTemplate = jdbcTemplate;
    this.namedParameterJdbcTemplate =
        namedParameterJdbcTemplate;
    this.pedidoRowMapper = pedidoRowMapper;
  }

  /**
   * Obtiene indicadores consolidados de todos
   * los pedidos registrados.
   */
  public ResumenPedidosProjection obtenerResumen() {
    String sql = """
        SELECT
            COUNT(*) AS total_pedidos,
            COALESCE(SUM(cantidad), 0)
                AS total_unidades,
            COALESCE(SUM(total), 0)
                AS importe_acumulado,
            COALESCE(AVG(total), 0)
                AS ticket_promedio
        FROM pedido
        """;

    return jdbcTemplate.queryForObject(
        sql,
        (rs, rowNum) ->
            new ResumenPedidosProjection(
                rs.getLong("total_pedidos"),
                rs.getLong("total_unidades"),
                valorMonetario(
                    rs.getBigDecimal(
                        "importe_acumulado"
                    )
                ),
                valorMonetario(
                    rs.getBigDecimal(
                        "ticket_promedio"
                    )
                )
            )
    );
  }

  /**
   * Busca pedidos utilizando un parámetro nombrado.
   */
  public List<Pedido> buscarPorCliente(String cliente) {
    String sql = """
        SELECT %s
        FROM pedido
        WHERE LOWER(cliente)
              LIKE LOWER(:cliente)
        ORDER BY id DESC
        """.formatted(COLUMNAS_PEDIDO);

    Map<String, Object> parametros = Map.of(
        "cliente",
        "%" + cliente.trim() + "%"
    );

    return namedParameterJdbcTemplate.query(
        sql,
        parametros,
        pedidoRowMapper
    );
  }

  /**
   * Consolida los pedidos por producto.
   */
  public List<ProductoVendidoProjection> listarProductosMasVendidos() {
    String sql = """
        SELECT
            producto_codigo,
            producto_nombre,
            COUNT(*) AS total_pedidos,
            SUM(cantidad) AS total_unidades,
            SUM(total) AS importe_acumulado
        FROM pedido
        GROUP BY
            producto_codigo,
            producto_nombre
        ORDER BY
            total_unidades DESC,
            importe_acumulado DESC
        """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) ->
            new ProductoVendidoProjection(
                rs.getString(
                    "producto_codigo"
                ),
                rs.getString(
                    "producto_nombre"
                ),
                rs.getLong(
                    "total_pedidos"
                ),
                rs.getLong(
                    "total_unidades"
                ),
                valorMonetario(
                    rs.getBigDecimal(
                        "importe_acumulado"
                    )
                )
            )
    );
  }

  // =====================================================================
  // [PA3] Consulta 1 - Búsqueda avanzada por cliente y total mínimo
  // =====================================================================

  /**
   * [PA3] Busca los pedidos cuyo cliente contiene el texto indicado
   * (sin distinguir mayúsculas) y cuyo total es mayor o igual al monto mínimo.
   *
   * <p>Los dos valores ingresados por el usuario viajan como
   * <strong>parámetros con nombre</strong> ({@code :cliente} y
   * {@code :totalMinimo}) mediante {@link NamedParameterJdbcTemplate}; nunca se
   * concatenan en el SQL. El único texto interpolado es la constante
   * {@code COLUMNAS_PEDIDO}, definida en esta misma clase.</p>
   *
   * <p>Los comodines {@code %} y {@code _} que escriba el usuario se escapan
   * con {@code !} para que se busquen literalmente ({@code ESCAPE '!'}).</p>
   *
   * <p>Las validaciones (cliente no vacío, monto &gt;= 0) pertenecen a
   * {@code PedidoReporteServiceImpl}; este repositorio solo accede a datos.</p>
   *
   * @param cliente     texto a buscar dentro del nombre del cliente
   * @param totalMinimo importe mínimo del pedido (inclusive)
   * @return pedidos que cumplen ambos filtros, ordenados por id descendente
   */
  public List<Pedido> buscarPedidosPorClienteYTotalMinimo(String cliente,
      BigDecimal totalMinimo) {

    String sql = """
        SELECT %s
        FROM pedido
        WHERE LOWER(cliente) LIKE LOWER(:cliente) ESCAPE '!'
          AND total >= :totalMinimo
        ORDER BY id DESC
        """.formatted(COLUMNAS_PEDIDO);

    MapSqlParameterSource parametros = new MapSqlParameterSource()
        .addValue("cliente", "%" + escaparLike(cliente.trim()) + "%")
        .addValue("totalMinimo", totalMinimo);

    // Se reutiliza el RowMapper existente porque el resultado es Pedido.
    return namedParameterJdbcTemplate.query(sql, parametros, pedidoRowMapper);
  }

  // =====================================================================
  // [PA3] Consulta 2 - Clientes frecuentes (GROUP BY + HAVING)
  // =====================================================================

  /**
   * [PA3] Consolida los pedidos por cliente y devuelve solo los clientes con
   * una cantidad de pedidos mayor o igual a {@code minPedidos}.
   *
   * <p>{@code GROUP BY} agrupa las filas por cliente y {@code HAVING} filtra
   * los grupos ya agregados (un {@code WHERE} no puede filtrar sobre
   * {@code COUNT(*)}). El umbral también se envía como parámetro con nombre.</p>
   *
   * @param minPedidos cantidad mínima de pedidos por cliente
   * @return proyecciones con totalPedidos, totalUnidades, importeAcumulado y
   *         ticketPromedio; ordenadas por total de pedidos e importe
   */
  public List<ClienteFrecuenteProjection> listarClientesFrecuentes(int minPedidos) {
    String sql = """
        SELECT
            cliente,
            COUNT(*)      AS total_pedidos,
            SUM(cantidad) AS total_unidades,
            SUM(total)    AS importe_acumulado,
            AVG(total)    AS ticket_promedio
        FROM pedido
        GROUP BY cliente
        HAVING COUNT(*) >= :minPedidos
        ORDER BY total_pedidos DESC, importe_acumulado DESC, cliente ASC
        """;

    MapSqlParameterSource parametros = new MapSqlParameterSource()
        .addValue("minPedidos", minPedidos);

    return namedParameterJdbcTemplate.query(
        sql,
        parametros,
        (rs, rowNum) -> new ClienteFrecuenteProjection(
            rs.getString("cliente"),
            rs.getLong("total_pedidos"),
            rs.getLong("total_unidades"),
            valorMonetario(rs.getBigDecimal("importe_acumulado")),
            // MySQL y H2 devuelven AVG(DECIMAL) con distinta escala:
            // se normaliza a 2 decimales para un resultado idéntico.
            valorMonetario(rs.getBigDecimal("ticket_promedio"))
                .setScale(2, RoundingMode.HALF_UP)
        )
    );
  }

  /**
   * [PA3] Escapa los caracteres especiales de LIKE para que el texto del
   * usuario se compare de forma literal. Se usa '!' como carácter de escape
   * porque se comporta igual en MySQL y en H2.
   */
  private String escaparLike(String texto) {
    return texto
        .replace("!", "!!")
        .replace("%", "!%")
        .replace("_", "!_");
  }

  private BigDecimal valorMonetario(
      BigDecimal valor) {

    return valor != null
        ? valor
        : BigDecimal.ZERO;
  }
}
