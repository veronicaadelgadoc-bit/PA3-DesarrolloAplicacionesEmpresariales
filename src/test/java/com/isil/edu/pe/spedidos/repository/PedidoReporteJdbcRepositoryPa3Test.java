package com.isil.edu.pe.spedidos.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.repository.projection.ClienteFrecuenteProjection;
import com.isil.edu.pe.spedidos.support.ReportesPa3DatosPrueba;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * [PA3] Pruebas de integración de las dos consultas nuevas de
 * {@link PedidoReporteJdbcRepository} sobre H2 (perfil test).
 *
 * <p>Cada prueba se revierte al finalizar ({@code @Transactional}).</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoReporteJdbcRepositoryPa3Test {

  @Autowired
  private PedidoReporteJdbcRepository repository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    ReportesPa3DatosPrueba.cargar(jdbcTemplate);
  }

  @Test
  @DisplayName("Return only orders matching cliente and totalMinimo ordered by id desc when advanced search is executed")
  void returnOnlyMatchingOrdersOrderedByIdDescWhenAdvancedSearchIsExecuted() {
    // Arrange
    String cliente = "ana";                       // minúsculas: la búsqueda no distingue
    BigDecimal totalMinimo = new BigDecimal("1000");

    // Act
    List<Pedido> pedidos = repository.buscarPedidosPorClienteYTotalMinimo(cliente, totalMinimo);

    // Assert: de 3 pedidos de Ana solo 2 superan 1000 (2500 y 1700), en orden id DESC.
    assertEquals(2, pedidos.size());
    assertTrue(pedidos.stream().allMatch(p -> p.getCliente().equals("Ana Torres")));
    assertTrue(pedidos.stream().allMatch(p -> p.getTotal().compareTo(totalMinimo) >= 0));
    assertTrue(pedidos.get(0).getId() > pedidos.get(1).getId());
    assertEquals(0, new BigDecimal("2500.00").compareTo(pedidos.get(0).getTotal()));
    assertEquals(0, new BigDecimal("1700.00").compareTo(pedidos.get(1).getTotal()));
  }

  @Test
  @DisplayName("Include order whose total equals totalMinimo when advanced search uses the exact amount")
  void includeOrderWhenTotalEqualsTotalMinimoInAdvancedSearch() {
    // Arrange / Act
    List<Pedido> pedidos = repository.buscarPedidosPorClienteYTotalMinimo(
        "Carla", new BigDecimal("850.00"));

    // Assert: el filtro es inclusivo (total >= :totalMinimo).
    assertEquals(1, pedidos.size());
    assertEquals("MON-001", pedidos.get(0).getProductoCodigo());
  }

  @Test
  @DisplayName("Return no rows and keep table intact when cliente contains SQL injection or LIKE wildcards")
  void returnNoRowsAndKeepTableIntactWhenClienteContainsInjectionOrWildcards() {
    // Arrange
    String intentoInyeccion = "' OR '1'='1";
    String comodin = "%";

    // Act
    List<Pedido> porInyeccion = repository.buscarPedidosPorClienteYTotalMinimo(
        intentoInyeccion, BigDecimal.ZERO);
    List<Pedido> porComodin = repository.buscarPedidosPorClienteYTotalMinimo(
        comodin, BigDecimal.ZERO);

    // Assert: el texto viaja como parámetro y se compara literalmente.
    assertTrue(porInyeccion.isEmpty());
    assertTrue(porComodin.isEmpty());
    Integer filas = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pedido", Integer.class);
    assertEquals(ReportesPa3DatosPrueba.TOTAL_PEDIDOS, filas);
  }

  @Test
  @DisplayName("Return aggregated values per customer with GROUP BY and HAVING when minPedidos is two")
  void returnAggregatedValuesPerCustomerWhenMinPedidosIsTwo() {
    // Arrange
    int minPedidos = 2;

    // Act
    List<ClienteFrecuenteProjection> clientes = repository.listarClientesFrecuentes(minPedidos);

    // Assert: HAVING excluye a Carla Diaz (1 pedido).
    assertEquals(2, clientes.size());

    ClienteFrecuenteProjection ana = clientes.get(0);
    assertEquals("Ana Torres", ana.cliente());
    assertEquals(3L, ana.totalPedidos());
    assertEquals(4L, ana.totalUnidades());
    assertEquals(0, new BigDecimal("4320.00").compareTo(ana.importeAcumulado()));
    assertEquals(new BigDecimal("1440.00"), ana.ticketPromedio());

    ClienteFrecuenteProjection luis = clientes.get(1);
    assertEquals("Luis Ramos", luis.cliente());
    assertEquals(2L, luis.totalPedidos());
    assertEquals(4L, luis.totalUnidades());
    assertEquals(0, new BigDecimal("2860.00").compareTo(luis.importeAcumulado()));
    assertEquals(new BigDecimal("1430.00"), luis.ticketPromedio());

    assertTrue(clientes.stream().noneMatch(c -> c.cliente().equals("Carla Diaz")));
  }

  @Test
  @DisplayName("Return empty list when minPedidos exceeds every customer order count")
  void returnEmptyListWhenMinPedidosExceedsEveryCustomerOrderCount() {
    // Arrange / Act
    List<ClienteFrecuenteProjection> clientes = repository.listarClientesFrecuentes(4);

    // Assert
    assertTrue(clientes.isEmpty());
  }
}
