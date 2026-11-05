package com.isil.edu.pe.spedidos.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoReporteControllerIntegrationTest {

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();

    jdbcTemplate.update("DELETE FROM pedido");
    insertarPedido("Ana Torres", "LAP-001", "Laptop", "2500.00", 2, "5000.00");
    insertarPedido("Carlos Perez", "MOU-001", "Mouse", "50.00", 3, "150.00");
  }

  @Test
  @DisplayName("Return plain JSON and HTTP 200 when CONSULTA requests level 2 summary")
  void returnPlainJsonAndHttp200WhenConsultaRequestsLevelTwoSummary() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/estadisticas/resumen")
            .with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalPedidos").value(2))
        .andExpect(jsonPath("$._links").doesNotExist());
  }

  @Test
  @DisplayName("Return plain JSON array when level 1 resource is filtered")
  void returnPlainJsonArrayWhenLevelOneResourceIsFiltered() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos")
            .param("cliente", "Ana")
            .with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0]").exists())
        .andExpect(jsonPath("$._links").doesNotExist());
  }

  @Test
  @DisplayName("Return HAL product sales collection when report is requested")
  void returnHalProductSalesCollectionWhenReportIsRequested() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/estadisticas/productos")
            .with(user("operador").roles("OPERADOR")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
        .andExpect(jsonPath("$._embedded").exists())
        .andExpect(jsonPath("$._links.self.href").exists())
        .andExpect(jsonPath("$._links['estadisticas-resumen'].href").exists());
  }

  @Test
  @DisplayName("Return not found when legacy action style search URI is requested")
  void returnNotFoundWhenLegacyActionStyleSearchUriIsRequested() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/buscar")
            .param("cliente", "Ana")
            .with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isNotFound());
  }

  private void insertarPedido(
      String cliente,
      String productoCodigo,
      String productoNombre,
      String precioUnitario,
      int cantidad,
      String total) {

    jdbcTemplate.update(
        """
        INSERT INTO pedido (
          cliente,
          producto_codigo,
          producto_nombre,
          precio_unitario,
          cantidad,
          total,
          fecha
        ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """,
        cliente,
        productoCodigo,
        productoNombre,
        new BigDecimal(precioUnitario),
        cantidad,
        new BigDecimal(total)
    );
  }
}

