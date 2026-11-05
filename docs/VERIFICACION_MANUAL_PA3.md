# PA3 – Procedimiento de verificación manual (cinco casos)

Procedimiento reproducible para ejecutar los cinco casos funcionales sobre la misma instancia de la aplicación. Base URL: `http://localhost:8081/sistema-pedidos`.

**Herramientas:** navegador con DevTools (pestaña **Network** con *Preserve log* activado, y **Console**), MySQL Workbench y la consola donde corre `spring-boot:run`.

> Los logs de `org.springframework.jdbc.core` están en `DEBUG`: en la consola de la aplicación se ve el SQL ejecutado con marcadores `?`, lo que demuestra que los valores viajan como parámetros y no concatenados.

---

## 0. Preparación

1. MySQL encendido; ejecutar `database/Script_BD_usuario_MySql.sql` (una sola vez).
2. Iniciar inventario-service (WildFly :8082) y luego `.\mvnw.cmd spring-boot:run`.
3. (Opcional, para que los números coincidan exactamente) ejecutar `database/pa3_limpiar_pedidos.sql` antes de registrar.
4. Iniciar sesión como **operador** y registrar en `/pedidos` estos seis pedidos:

| # | Cliente | Producto | Cantidad | Total esperado |
|---|---|---|---|---|
| 1 | Ana Torres | MON-001 Monitor | 2 | 1700.00 |
| 2 | Ana Torres | TEC-001 Teclado | 1 | 120.00 |
| 3 | Luis Ramos | LAP-001 Laptop | 1 | 2500.00 |
| 4 | Ana Torres | LAP-001 Laptop | 1 | 2500.00 |
| 5 | Luis Ramos | TEC-001 Teclado | 3 | 360.00 |
| 6 | Carla Diaz | MON-001 Monitor | 1 | 850.00 |

5. En MySQL Workbench ejecutar la consulta 0 de `database/pa3_consultas_verificacion.sql` para ver los seis registros.

---

## Caso 1 – Búsqueda por cliente y monto mínimo

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1.1 | Abrir `/reportes/pedidos`, escribir **Cliente:** `Ana`, **Total mínimo:** `1000` y pulsar **Buscar** | Tabla con 2 pedidos: #4 (2500.00) y #1 (1700.00), en ese orden. El pedido #2 de Ana (120.00) no aparece |
| 1.2 | Abrir `/api/v1/pedidos/busqueda-avanzada?cliente=Ana&totalMinimo=1000` (o el enlace "JSON equivalente") | HTTP 200, arreglo JSON con los mismos 2 pedidos |
| 1.3 | Ejecutar la consulta 1 de `pa3_consultas_verificacion.sql` en MySQL | Mismas 2 filas |
| 1.4 | Consola de la aplicación | `Executing prepared SQL statement [... LIKE LOWER(?) ESCAPE '!' AND total >= ? ORDER BY id DESC]` |

**Recorrido:** `PedidoReporteWebController` / `PedidoReporteController` → `PedidoReporteServiceImpl.buscarPedidosPorClienteYTotalMinimo` (valida) → `PedidoReporteJdbcRepository` → `NamedParameterJdbcTemplate` + `PedidoRowMapper` → MySQL.

## Caso 2 – Clientes frecuentes y consolidado

| Paso | Acción | Resultado esperado |
|---|---|---|
| 2.1 | En `/reportes/pedidos`, sección Clientes frecuentes, **Mínimo de pedidos:** `2` → **Consultar** | Ana Torres (3 pedidos, 4 uds, S/ 4,320.00, S/ 1,440.00) y Luis Ramos (2, 4, S/ 2,860.00, S/ 1,430.00). Carla Diaz (1 pedido) no aparece |
| 2.2 | `/api/v1/pedidos/clientes-frecuentes?minPedidos=2` | HTTP 200 con los mismos valores en JSON |
| 2.3 | Consulta 2 de `pa3_consultas_verificacion.sql` | Mismos totales |

Comprobación manual: Ana = 1700 + 120 + 2500 = 4320 → 4320 / 3 = 1440; unidades 2 + 1 + 1 = 4. Luis = 2500 + 360 = 2860 → 1430; unidades 1 + 3 = 4.

**Recorrido:** Controller → `PedidoReporteServiceImpl.listarClientesFrecuentes` (valida `minPedidos >= 1`) → `PedidoReporteJdbcRepository.listarClientesFrecuentes` (`GROUP BY cliente HAVING COUNT(*) >= :minPedidos`) → `ClienteFrecuenteProjection` → `ClienteFrecuente` → DTO/JSP.

## Caso 3 – Parámetros inválidos

| Paso | URL | Resultado esperado |
|---|---|---|
| 3.1 | `/api/v1/pedidos/busqueda-avanzada?cliente=&totalMinimo=100` | **400**, `"parametro": "cliente"` |
| 3.2 | `/api/v1/pedidos/busqueda-avanzada?cliente=Ana&totalMinimo=-5` | **400**, `"parametro": "totalMinimo"` |
| 3.3 | `/api/v1/pedidos/clientes-frecuentes?minPedidos=0` | **400**, `"parametro": "minPedidos"` |
| 3.4 | `/reportes/pedidos?minPedidos=0` (Network muestra el estado) | **400** y mensaje en la vista |
| 3.5 | `/api/v1/pedidos/busqueda-avanzada?cliente=' OR '1'='1&totalMinimo=0` | **200** y `[]`: el texto se trata como dato, no como SQL |

En 3.1–3.4 la consola **no** muestra ninguna línea `Executing prepared SQL statement` para esas solicitudes: el Service rechaza el parámetro antes de llegar al repositorio.

## Caso 4 – Continuidad con PA2: inventario indisponible

| Paso | Acción | Resultado esperado |
|---|---|---|
| 4.1 | Como **operador**, abrir `/pedidos` (inventario encendido) y llenar el formulario sin enviarlo | Formulario cargado con productos |
| 4.2 | **Detener WildFly** (Ctrl+C en la consola de inventario-service) | `http://localhost:8082/...` deja de responder |
| 4.3 | Enviar el formulario | **503** y página de error: "El servicio de inventario no se encuentra disponible o excedió el tiempo de espera." No se registra el pedido |
| 4.4 | Abrir `/reportes/pedidos` y `/api/v1/pedidos/clientes-frecuentes?minPedidos=2` | **200**: los reportes siguen funcionando porque consultan MySQL, no inventario |
| 4.5 | Consulta 0 en MySQL | Siguen siendo 6 pedidos |

**Recorrido:** `PedidoController.registrar` → `PedidoServiceImpl.registrarPedido` → `InventarioRestClient.reservarStock` → `ResourceAccessException` → `InventarioNoDisponibleException` → 503. Los reportes usan `PedidoReporteService` → JDBC → MySQL, sin `InventarioClient`.

## Caso 5 – Seguridad por roles y CSRF

| Paso | Acción | Resultado esperado |
|---|---|---|
| 5.1 | Ventana de incógnito → `/reportes/pedidos` | **302** → `/login` (Network) |
| 5.2 | Iniciar sesión como **consulta** | La vista de reportes carga (200) |
| 5.3 | Como **consulta**, en DevTools → Console ejecutar el **script A** | `HTTP 403` (rol sin permiso, aunque el token CSRF es válido) |
| 5.4 | Cerrar sesión, entrar como **operador**, abrir `/reportes/pedidos` y ejecutar el **script B** | `HTTP 403` (falta el token CSRF) |
| 5.5 | Encender inventario-service de nuevo y, como **operador**, registrar un pedido desde `/pedidos` | Pedido registrado (302 → `/pedidos?creado=7`) y visible en reportes |

**Script A** – CONSULTA con CSRF válido intenta registrar:

```js
const token = document.querySelector('input[name="_csrf"]').value;
fetch('/sistema-pedidos/pedidos', {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body: new URLSearchParams({ _csrf: token, cliente: 'Prueba CONSULTA', productoCodigo: 'TEC-001', cantidad: '1' })
}).then(r => console.log('HTTP', r.status));
```

**Script B** – OPERADOR sin token CSRF:

```js
fetch('/sistema-pedidos/pedidos', {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body: new URLSearchParams({ cliente: 'Prueba sin CSRF', productoCodigo: 'TEC-001', cantidad: '1' })
}).then(r => console.log('HTTP', r.status));
```

**Recorrido:** el filtro `CsrfFilter` rechaza el POST sin token antes del controlador; con token válido, `AuthorizationFilter` aplica `POST /pedidos → hasRole(OPERADOR)` y rechaza a CONSULTA. En ningún caso se invoca `PedidoController`.
