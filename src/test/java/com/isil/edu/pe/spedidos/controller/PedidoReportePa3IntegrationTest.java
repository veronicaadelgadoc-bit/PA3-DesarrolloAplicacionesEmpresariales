package com.isil.edu.pe.spedidos.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.isil.edu.pe.spedidos.support.ReportesPa3DatosPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * [PA3] Pruebas de integración de la vista MVC, la API JSON y las reglas de
 * seguridad del módulo de reportes.
 *
 * <p>Se usa {@code webAppContextSetup(...).apply(springSecurity())} para que la
 * cadena real de filtros de Spring Security participe en cada solicitud, y H2
 * (perfil test) como base de datos.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoReportePa3IntegrationTest {

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

    ReportesPa3DatosPrueba.cargar(jdbcTemplate);
  }

  // ===================== Vista MVC / JSP =====================

  @Test
  @DisplayName("Render reportes-pedidos view with Model data when CONSULTA requests GET /reportes/pedidos")
  void renderReportViewWithModelDataWhenConsultaRequestsReportesPedidos() throws Exception {
    // Arrange / Act
    ResultActions response = mockMvc.perform(get("/reportes/pedidos")
        .param("cliente", "Ana")
        .param("totalMinimo", "1000")
        .param("minPedidos", "2")
        .with(user("consulta").roles("CONSULTA")));

    // Assert: el ViewResolver resuelve el nombre lógico a la JSP.
    response.andExpect(status().isOk())
        .andExpect(view().name("reportes-pedidos"))
        .andExpect(forwardedUrl("/WEB-INF/views/reportes-pedidos.jsp"))
        .andExpect(model().attributeExists("resumen"))
        .andExpect(model().attribute("pedidosEncontrados", hasSize(2)))
        .andExpect(model().attribute("clientesFrecuentes", hasSize(2)));
  }

  @Test
  @DisplayName("Redirect anonymous user to login when accessing GET /reportes/pedidos")
  void redirectAnonymousUserToLoginWhenAccessingReportesPedidos() throws Exception {
    // Arrange / Act
    ResultActions response = mockMvc.perform(get("/reportes/pedidos"));

    // Assert: Spring Security intercepta antes del controlador.
    response.andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", endsWith("/login")));
  }

  @Test
  @DisplayName("Return 400 and error message in view when minPedidos is zero on GET /reportes/pedidos")
  void returnBadRequestWithMessageInViewWhenMinPedidosIsZero() throws Exception {
    // Arrange / Act
    ResultActions response = mockMvc.perform(get("/reportes/pedidos")
        .param("minPedidos", "0")
        .with(user("operador").roles("OPERADOR")));

    // Assert: el Service rechaza el umbral y no se entrega un resultado válido.
    response.andExpect(status().isBadRequest())
        .andExpect(view().name("reportes-pedidos"))
        .andExpect(model().attributeExists("errorClientes"))
        .andExpect(model().attributeDoesNotExist("clientesFrecuentes"));
  }

  @Test
  @DisplayName("Reject POST /reportes/pedidos when CONSULTA sends valid CSRF token")
  void rejectPostToReportesWhenConsultaSendsValidCsrfToken() throws Exception {
    // Arrange / Act
    ResultActions response = mockMvc.perform(post("/reportes/pedidos")
        .with(user("consulta").roles("CONSULTA"))
        .with(csrf()));

    // Assert: el módulo de reportes es de solo lectura.
    response.andExpect(status().isForbidden());
  }

  // ===================== API REST / JSON =====================

  @Test
  @DisplayName("Return only expected orders as JSON when CONSULTA calls busqueda-avanzada")
  void returnOnlyExpectedOrdersAsJsonWhenConsultaCallsBusquedaAvanzada() throws Exception {
    // Arrange / Act
    ResultActions response = mockMvc.perform(get("/api/v1/pedidos/busqueda-avanzada")
        .param("cliente", "Ana")
        .param("totalMinimo", "1000")
        .with(user("consulta").roles("CONSULTA")));

    // Assert
    response.andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].cliente").value("Ana Torres"))
        .andExpect(jsonPath("$[0].total").value(2500.0))
        .andExpect(jsonPath("$[1].total").value(1700.0));
  }

  @Test
  @DisplayName("Return consolidated JSON when OPERADOR calls clientes-frecuentes with minPedidos two")
  void returnConsolidatedJsonWhenOperadorCallsClientesFrecuentesWithMinPedidosTwo() throws Exception {
    // Arrange / Act
    ResultActions response = mockMvc.perform(get("/api/v1/pedidos/clientes-frecuentes")
        .param("minPedidos", "2")
        .with(user("operador").roles("OPERADOR")));

    // Assert
    response.andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].cliente").value("Ana Torres"))
        .andExpect(jsonPath("$[0].totalPedidos").value(3))
        .andExpect(jsonPath("$[0].totalUnidades").value(4))
        .andExpect(jsonPath("$[0].importeAcumulado").value(4320.0))
        .andExpect(jsonPath("$[0].ticketPromedio").value(1440.0))
        .andExpect(jsonPath("$[1].cliente").value("Luis Ramos"));
  }

  @Test
  @DisplayName("Return 400 JSON error when busqueda-avanzada receives negative totalMinimo")
  void returnBadRequestJsonWhenBusquedaAvanzadaReceivesNegativeTotalMinimo() throws Exception {
    // Arrange / Act
    ResultActions response = mockMvc.perform(get("/api/v1/pedidos/busqueda-avanzada")
        .param("cliente", "Ana")
        .param("totalMinimo", "-5")
        .with(user("consulta").roles("CONSULTA")));

    // Assert
    response.andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.parametro").value("totalMinimo"));
  }

  @Test
  @DisplayName("Return 400 JSON error when clientes-frecuentes receives minPedidos zero")
  void returnBadRequestJsonWhenClientesFrecuentesReceivesMinPedidosZero() throws Exception {
    // Arrange / Act
    ResultActions response = mockMvc.perform(get("/api/v1/pedidos/clientes-frecuentes")
        .param("minPedidos", "0")
        .with(user("consulta").roles("CONSULTA")));

    // Assert
    response.andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.parametro").value("minPedidos"));
  }

  @Test
  @DisplayName("Return 400 JSON error when totalMinimo has an invalid number format")
  void returnBadRequestJsonWhenTotalMinimoHasInvalidFormat() throws Exception {
    // Arrange / Act
    ResultActions response = mockMvc.perform(get("/api/v1/pedidos/busqueda-avanzada")
        .param("cliente", "Ana")
        .param("totalMinimo", "abc")
        .with(user("consulta").roles("CONSULTA")));

    // Assert
    response.andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.parametro").value("totalMinimo"));
  }
}
