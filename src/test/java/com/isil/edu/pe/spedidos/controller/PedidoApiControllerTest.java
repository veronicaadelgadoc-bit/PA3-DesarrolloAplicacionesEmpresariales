package com.isil.edu.pe.spedidos.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.service.PedidoService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PedidoApiControllerTest {

  private static final String ORDERS_PATH = "/api/v1/orders";

  private static final String SUMMARY_PATH = ORDERS_PATH + "/resumen";

  private PedidoService pedidoService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
      pedidoService = mock(PedidoService.class);
      mockMvc = MockMvcBuilders
          .standaloneSetup(new PedidoApiController(pedidoService))
          .build();
  }

  @Test
  @DisplayName("Return empty JSON array from PedidoApiController when no orders exist")
  void returnEmptyJsonArrayFromPedidoApiControllerWhenNoOrdersExist()
      throws Exception {
    // Arrange: definir la única respuesta del colaborador necesaria.
    when(pedidoService.listarPedidos()).thenReturn(List.of());

    // Act: ejercer el contrato HTTP del SUT.
    ResultActions response = mockMvc.perform(get(ORDERS_PATH));

    // Assert: resultado observable, no detalles internos de implementación.
    response
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json("[]"));
  }

  @Test
  @DisplayName("Return zero order count from PedidoApiController when no orders exist")
  void returnZeroOrderCountFromPedidoApiControllerWhenNoOrdersExist()
      throws Exception {
    // Arrange
    when(pedidoService.listarPedidos()).thenReturn(List.of());

    // Act
    ResultActions response = mockMvc.perform(get(SUMMARY_PATH));

    // Assert
    response
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.modulo").value("Sistema de Pedidos"))
        .andExpect(jsonPath("$.totalPedidos").value(0));
  }

  @Test
  @DisplayName("Return order count of two from PedidoApiController when two orders exist")
  void returnOrderCountOfTwoFromPedidoApiControllerWhenTwoOrdersExist()
      throws Exception {
    // Arrange: datos mínimos para comprobar el conteo.
    List<Pedido> orders = List.of(new Pedido(), new Pedido());
    when(pedidoService.listarPedidos()).thenReturn(orders);

    // Act
    ResultActions response = mockMvc.perform(get(SUMMARY_PATH));

    // Assert
    response
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPedidos").value(2));
  }

  @Test
  @DisplayName("Return total units of five from PedidoApiController when quantities are two and three")
  void returnTotalUnitsOfFiveFromPedidoApiControllerWhenQuantitiesAreTwoAndThree()
      throws Exception {

    // Arrange
    Pedido first = new Pedido();
    first.setCantidad(2);

    Pedido second = new Pedido();
    second.setCantidad(3);

    when(pedidoService.listarPedidos())
        .thenReturn(List.of(first, second));

    // Act
    ResultActions response = mockMvc.perform(
        get("/api/v1/orders/resumen")
    );

    // Assert
    response
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPedidos").value(2))
        .andExpect(jsonPath("$.totalUnidades").value(5));
  }

}
