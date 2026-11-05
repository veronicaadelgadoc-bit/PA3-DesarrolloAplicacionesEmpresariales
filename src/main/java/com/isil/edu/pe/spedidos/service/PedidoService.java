package com.isil.edu.pe.spedidos.service;

import com.isil.edu.pe.spedidos.client.dto.ProductoInventarioResponse;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.PedidoException;
import com.isil.edu.pe.spedidos.service.impl.PedidoServiceImpl;
import java.util.List;

/**
 * Define el contrato de la capa de servicio para las operaciones de negocio
 * relacionadas con la gestión de pedidos.
 *
 * <p>Esta interfaz representa la <strong>capa de servicio</strong> dentro de la
 * arquitectura de la aplicación. Su responsabilidad es exponer las operaciones
 * que pueden ser utilizadas por la capa de presentación, sin revelar cómo se
 * implementan internamente las reglas de negocio, el acceso a datos o la
 * comunicación con servicios externos.</p>
 *
 * <p>Desde una perspectiva arquitectónica, {@code PedidoService} actúa como una
 * frontera entre el Controller y las dependencias técnicas del sistema:</p>
 *
 * <pre>
 * PedidoController
 *        |
 *        | utiliza
 *        v
 * PedidoService                 &lt;- contrato / abstracción
 *        ^
 *        |
 *        | implementa
 *        |
 * PedidoServiceImpl             &lt;- implementación
 *        |
 *        +-----------------------------+
 *        |                             |
 *        v                             v
 * PedidoRepository               InventarioClient
 *        |                             |
 *        v                             v
 * MySQL / H2                  inventario-service
 * </pre>
 *
 * <p>En la ejecución normal de la aplicación, la persistencia se realiza sobre
 * MySQL. Durante las pruebas de integración se utiliza H2 mediante el perfil
 * {@code test}, evitando que las pruebas dependan de una instancia MySQL
 * disponible.</p>
 *
 * <p>El uso de una interfaz permite aplicar principios importantes de diseño:</p>
 *
 * <ul>
 *   <li>
 *     <strong>Abstracción:</strong> las clases consumidoras conocen qué
 *     operaciones están disponibles, pero no necesitan conocer su implementación.
 *   </li>
 *   <li>
 *     <strong>Inversión de dependencias:</strong> el Controller depende del
 *     contrato {@code PedidoService} y no directamente de
 *     {@link PedidoServiceImpl}.
 *   </li>
 *   <li>
 *     <strong>Bajo acoplamiento:</strong> la implementación puede evolucionar
 *     sin obligar a modificar las clases que consumen el servicio.
 *   </li>
 *   <li>
 *     <strong>Testabilidad:</strong> durante las pruebas unitarias es posible
 *     sustituir esta dependencia por mocks creados con Mockito.
 *   </li>
 * </ul>
 *
 * <p>Spring puede inyectar automáticamente una implementación de esta interfaz
 * cuando una clase la declara como dependencia. En este proyecto, la
 * implementación concreta corresponde a {@link PedidoServiceImpl}.</p>
 *
 * <p><strong>Nota pedagógica:</strong> la interfaz define el
 * <em>qué</em> puede hacer la capa de servicio; la implementación define el
 * <em>cómo</em> se ejecuta cada operación.</p>
 *
 * @author Carlos.Nole
 * @see PedidoServiceImpl
 * @see Pedido
 */
public interface PedidoService {

  /**
   * Registra un nuevo pedido aplicando las reglas de negocio definidas por
   * la aplicación.
   *
   * <p>Esta operación representa un caso de uso de negocio. El Controller
   * entrega los datos recibidos desde la solicitud y la capa de servicio se
   * encarga de coordinar las acciones necesarias para completar el registro.</p>
   *
   * <p>Conceptualmente, el flujo es el siguiente:</p>
   *
   * <pre>
   * validar datos de entrada
   *          |
   *          v
   * consultar / reservar stock
   *          |
   *          v
   * calcular importe del pedido
   *          |
   *          v
   * construir entidad Pedido
   *          |
   *          v
   * persistir el pedido
   * </pre>
   *
   * <p>La persistencia se realiza en MySQL durante la ejecución normal de la
   * aplicación. En las pruebas de integración, la misma operación puede
   * ejecutarse sobre H2 utilizando el perfil {@code test}.</p>
   *
   * <p>La lógica concreta de validación, integración con Inventario y
   * persistencia pertenece a la implementación del servicio. De esta manera,
   * el Controller no necesita conocer esos detalles técnicos.</p>
   *
   * @param cliente nombre o identificación del cliente que realiza el pedido
   * @param productoCodigo código del producto solicitado
   * @param cantidad número de unidades que se desean registrar; debe cumplir
   *                  las reglas de negocio definidas por la aplicación
   * @return pedido creado y persistido satisfactoriamente
   * @throws PedidoException si los datos incumplen una regla de negocio o la
   *                         operación no puede completarse conforme a las
   *                         condiciones definidas por el dominio
   */
  Pedido registrarPedido(String cliente, String productoCodigo, int cantidad);

  /**
   * Obtiene la relación de productos disponibles desde el servicio externo
   * de Inventario.
   *
   * <p>La capa de servicio abstrae al Controller de los detalles técnicos de
   * integración. Por ello, quien invoca este método no necesita conocer cómo
   * se construye la petición HTTP, qué URL utiliza el servicio remoto ni cómo
   * se transforma la respuesta JSON.</p>
   *
   * <p>Conceptualmente:</p>
   *
   * <pre>
   * PedidoController
   *        |
   *        v
   * PedidoService.listarProductos()
   *        |
   *        v
   * InventarioClient
   *        |
   *        | HTTP
   *        v
   * inventario-service
   * </pre>
   *
   * <p>El resultado se representa mediante
   * {@link ProductoInventarioResponse}, que funciona como DTO de integración y
   * evita exponer directamente detalles internos del servicio externo.</p>
   *
   * @return lista de productos obtenidos desde {@code inventario-service};
   *         el contenido concreto depende de la respuesta proporcionada por
   *         dicho servicio
   */
  List<ProductoInventarioResponse> listarProductos();

  /**
   * Recupera los pedidos registrados en la base de datos de la aplicación.
   *
   * <p>Esta operación delega el acceso a datos en la capa Repository. La capa
   * de servicio no debería construir directamente sentencias SQL ni administrar
   * conexiones JDBC para este caso de uso.</p>
   *
   * <p>El flujo conceptual es:</p>
   *
   * <pre>
   * PedidoController
   *        |
   *        v
   * PedidoService.listarPedidos()
   *        |
   *        v
   * PedidoRepository
   *        |
   *        v
   * Spring Data JPA / Hibernate
   *        |
   *        v
   * MySQL / H2
   * </pre>
   *
   * <p>En ejecución normal los datos se consultan desde MySQL. Durante las
   * pruebas de integración, el perfil {@code test} permite realizar la misma
   * operación utilizando H2.</p>
   *
   * <p>Esta separación mantiene a las capas superiores independientes de la
   * tecnología de persistencia utilizada.</p>
   *
   * @return lista de pedidos registrados en el sistema
   */
  List<Pedido> listarPedidos();

}
