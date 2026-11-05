package com.isil.edu.pe.spedidos.repository.mapper;

import com.isil.edu.pe.spedidos.entity.Pedido;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * Convierte una fila recuperada mediante JDBC
 * en un objeto Pedido.
 */
@Component
public class PedidoRowMapper implements RowMapper<Pedido> {

  /**
   * Mapea una fila del ResultSet a un objeto Pedido.
   *
   * @param rs     el ResultSet que contiene los datos de la fila
   * @param rowNum el número de fila (no se utiliza en este caso)
   * @return un objeto Pedido con los datos mapeados
   * @throws SQLException si ocurre un error al acceder a los datos del ResultSet
   */
  @Override
  public Pedido mapRow(ResultSet rs, int rowNum)
      throws SQLException {

    return new Pedido(
        rs.getLong("id"),
        rs.getString("cliente"),
        rs.getString("producto_codigo"),
        rs.getString("producto_nombre"),
        rs.getBigDecimal("precio_unitario"),
        rs.getInt("cantidad"),
        rs.getBigDecimal("total"),
        rs.getTimestamp("fecha").toLocalDateTime()
    );
  }
}
