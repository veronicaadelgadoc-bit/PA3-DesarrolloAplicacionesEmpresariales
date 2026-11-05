package com.isil.edu.pe.spedidos.repository;

import com.isil.edu.pe.spedidos.entity.Pedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Contrato de persistencia para consultar y administrar entidades {@link Pedido}.
 *
 * <p>Esta interfaz pertenece a la <strong>capa de acceso a datos</strong> del Sistema
 * de Pedidos. Su propósito es aislar a la capa de servicio de los detalles de
 * persistencia, de modo que el negocio trabaje con operaciones del repositorio
 * sin construir directamente consultas SQL o gestionar un {@code EntityManager}.</p>
 *
 * <p>Extender {@link JpaRepository} permite aprovechar las operaciones CRUD
 * proporcionadas por Spring Data JPA, como {@code save}, {@code findById},
 * {@code findAll} y {@code deleteById}. Los parámetros genéricos indican:</p>
 * <ul>
 *   <li>{@code Pedido}: tipo de entidad persistida.</li>
 *   <li>{@code Long}: tipo de dato del identificador de la entidad.</li>
 * </ul>
 *
 * <p>Spring Data JPA crea la implementación del repositorio durante la
 * inicialización del contexto, por lo que no es necesario escribir una clase
 * {@code PedidoRepositoryImpl} para las operaciones declaradas aquí. La anotación
 * {@link Repository @Repository} expresa su función en la capa de persistencia;
 * en interfaces Spring Data JPA normalmente no es obligatoria.</p>
 *
 * <p><strong>Flujo arquitectónico:</strong></p>
 * <pre>
 * PedidoController
 *        |
 *        v
 * PedidoService
 *        |
 *        v
 * PedidoRepository
 *        |
 *        v
 * Spring Data JPA / Hibernate
 *        |
 *        +-- MySQL (ejecución normal)
 *        +-- H2 (perfil de pruebas de integración)
 * </pre>
 *
 * <p>La base de datos efectiva depende de la configuración del entorno: MySQL
 * para la aplicación y H2 en memoria cuando se activa el perfil de pruebas.
 * Este repositorio no selecciona ni configura el motor de base de datos.</p>
 *
 * <p><strong>Nota didáctica:</strong> Spring Data JPA y Spring JDBC son dos
 * estrategias distintas de acceso a datos. Esta interfaz utiliza JPA; un
 * repositorio basado en {@code JdbcTemplate} escribe explícitamente sus
 * consultas SQL y el mapeo de filas.</p>
 *
 * @author Carlos.Nole
 * @see Pedido
 * @see JpaRepository
 */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

  /**
   * Recupera todos los pedidos en orden descendente según su identificador.
   *
   * <p>Es un ejemplo de <strong>consulta derivada del nombre del método</strong>
   * (query method). Spring Data JPA interpreta sus componentes:</p>
   * <ul>
   *   <li>{@code findAllBy}: solicita todos los registros, sin filtro adicional.</li>
   *   <li>{@code OrderById}: utiliza el atributo {@code id} como criterio de orden.</li>
   *   <li>{@code Desc}: establece el orden descendente.</li>
   * </ul>
   *
   * <p>Su equivalente conceptual en JPQL sería:</p>
   * <pre>
   * SELECT p FROM Pedido p ORDER BY p.id DESC
   * </pre>
   *
   * <p>El resultado es útil para mostrar primero los identificadores más altos.
   * Sin embargo, un identificador mayor no demuestra necesariamente que el
   * pedido sea más reciente; para un orden cronológico estricto se debe usar
   * un atributo de fecha de creación.</p>
   *
   * <p><strong>Consideración de rendimiento:</strong> al retornar una lista
   * completa, esta operación no aplica paginación. Si el volumen de pedidos
   * aumenta, conviene evaluar {@code Pageable} y un resultado paginado.</p>
   *
   * @return lista de entidades {@link Pedido} ordenadas por {@code id} en orden
   *         descendente; si no existen registros, devuelve una lista vacía
   */
  List<Pedido> findAllByOrderByIdDesc();

}
