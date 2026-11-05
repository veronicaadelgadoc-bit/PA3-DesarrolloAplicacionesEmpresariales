<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  [PA3] Vista de consulta y supervisión de pedidos.
  Datos recibidos desde PedidoReporteWebController mediante Model:
    resumen, pedidosEncontrados, clientesFrecuentes,
    errorBusqueda, errorClientes, errorGeneral y los valores ingresados.
  La vista solo presenta información: no contiene SQL ni reglas de negocio.
--%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Reportes de pedidos - Sistema de Pedidos ISIL</title>
    <c:url var="cssUrl" value="/assets/css/app.css"/>
    <link rel="stylesheet" href="${cssUrl}">
</head>
<body>
<c:url var="reportesUrl" value="/reportes/pedidos"/>
<c:url var="pedidosUrl" value="/pedidos"/>
<c:url var="logoutUrl" value="/logout"/>

<div class="contenedor">
    <header class="cabecera">
        <div>
            <p class="eyebrow">PA3 · Spring MVC · Spring Security · Spring JDBC</p>
            <h1>Reportes de pedidos</h1>
            <p class="subtitulo">
                Consulta y supervisión sobre los pedidos almacenados en MySQL.
                Usuario: <strong><c:out value="${pageContext.request.userPrincipal.name}"/></strong>
            </p>
            <nav class="navegacion">
                <a href="${pedidosUrl}">← Registro de pedidos</a>
            </nav>
        </div>
        <form method="post" action="${logoutUrl}">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <button type="submit">Cerrar sesión</button>
        </form>
        <div class="flujo">
            PedidoReporteWebController → PedidoReporteService → PedidoReporteJdbcRepository → MySQL
        </div>
    </header>

    <c:if test="${not empty errorGeneral}">
        <div class="alerta error"><c:out value="${errorGeneral}"/></div>
    </c:if>

    <%-- ===================== 1. Resumen existente ===================== --%>
    <section class="tarjeta">
        <h2>Resumen general</h2>
        <div class="indicadores">
            <div class="indicador">
                <span>Total de pedidos</span>
                <strong><c:out value="${resumen.totalPedidos}"/></strong>
            </div>
            <div class="indicador">
                <span>Total de unidades</span>
                <strong><c:out value="${resumen.totalUnidades}"/></strong>
            </div>
            <div class="indicador">
                <span>Importe acumulado</span>
                <strong>S/ <fmt:formatNumber value="${resumen.importeAcumulado}"
                                             minFractionDigits="2" maxFractionDigits="2"/></strong>
            </div>
            <div class="indicador">
                <span>Ticket promedio</span>
                <strong>S/ <fmt:formatNumber value="${resumen.ticketPromedio}"
                                             minFractionDigits="2" maxFractionDigits="2"/></strong>
            </div>
        </div>
    </section>

    <%-- ===================== 2. Búsqueda avanzada ===================== --%>
    <section class="tarjeta">
        <h2>Búsqueda avanzada: cliente y total mínimo</h2>

        <c:if test="${not empty errorBusqueda}">
            <div class="alerta error"><c:out value="${errorBusqueda}"/></div>
        </c:if>

        <form method="get" action="${reportesUrl}" class="form-grid form-reporte">
            <label>
                Cliente (contiene)
                <input name="cliente" maxlength="120" placeholder="Ej. Ana"
                       value="${fn:escapeXml(clienteIngresado)}">
            </label>
            <label>
                Total mínimo (S/)
                <input name="totalMinimo" type="number" step="0.01" placeholder="Ej. 1000"
                       value="${fn:escapeXml(totalMinimoIngresado)}">
            </label>
            <input type="hidden" name="minPedidos" value="${fn:escapeXml(minPedidosIngresado)}">
            <button type="submit">Buscar</button>
        </form>

        <c:if test="${busquedaSolicitada and empty errorBusqueda}">
            <c:url var="apiBusquedaUrl" value="/api/v1/pedidos/busqueda-avanzada">
                <c:param name="cliente" value="${clienteIngresado}"/>
                <c:param name="totalMinimo" value="${totalMinimoIngresado}"/>
            </c:url>
            <p class="nota">
                <c:out value="${fn:length(pedidosEncontrados)}"/> pedido(s) encontrado(s).
                JSON equivalente: <a href="${apiBusquedaUrl}" target="_blank"><code>GET <c:out value="${apiBusquedaUrl}"/></code></a>
            </p>

            <div class="tabla-scroll">
                <table>
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Cliente</th>
                        <th>Código</th>
                        <th>Producto</th>
                        <th>Cantidad</th>
                        <th>Total</th>
                        <th>Fecha</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="pedido" items="${pedidosEncontrados}">
                        <tr>
                            <td><c:out value="${pedido.id}"/></td>
                            <td><c:out value="${pedido.cliente}"/></td>
                            <td><c:out value="${pedido.productoCodigo}"/></td>
                            <td><c:out value="${pedido.productoNombre}"/></td>
                            <td><c:out value="${pedido.cantidad}"/></td>
                            <td>S/ <fmt:formatNumber value="${pedido.total}"
                                                     minFractionDigits="2" maxFractionDigits="2"/></td>
                            <td><c:out value="${pedido.fecha}"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty pedidosEncontrados}">
                        <tr>
                            <td colspan="7" class="sin-datos">Ningún pedido cumple ambos filtros.</td>
                        </tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </c:if>
    </section>

    <%-- ===================== 3. Clientes frecuentes ===================== --%>
    <section class="tarjeta">
        <h2>Clientes frecuentes</h2>

        <c:if test="${not empty errorClientes}">
            <div class="alerta error"><c:out value="${errorClientes}"/></div>
        </c:if>

        <form method="get" action="${reportesUrl}" class="form-grid form-reporte">
            <label>
                Mínimo de pedidos por cliente
                <input name="minPedidos" type="number" step="1"
                       value="${fn:escapeXml(minPedidosIngresado)}">
            </label>
            <c:if test="${busquedaSolicitada}">
                <input type="hidden" name="cliente" value="${fn:escapeXml(clienteIngresado)}">
                <input type="hidden" name="totalMinimo" value="${fn:escapeXml(totalMinimoIngresado)}">
            </c:if>
            <button type="submit">Consultar</button>
        </form>

        <c:if test="${empty errorClientes and not empty minPedidosIngresado}">
            <c:url var="apiClientesUrl" value="/api/v1/pedidos/clientes-frecuentes">
                <c:param name="minPedidos" value="${minPedidosIngresado}"/>
            </c:url>
            <p class="nota">
                Clientes con <c:out value="${minPedidosIngresado}"/> pedido(s) o más.
                JSON equivalente: <a href="${apiClientesUrl}" target="_blank"><code>GET <c:out value="${apiClientesUrl}"/></code></a>
            </p>

            <div class="tabla-scroll">
                <table>
                    <thead>
                    <tr>
                        <th>Cliente</th>
                        <th>Total pedidos</th>
                        <th>Total unidades</th>
                        <th>Importe acumulado</th>
                        <th>Ticket promedio</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="cf" items="${clientesFrecuentes}">
                        <tr>
                            <td><c:out value="${cf.cliente}"/></td>
                            <td><c:out value="${cf.totalPedidos}"/></td>
                            <td><c:out value="${cf.totalUnidades}"/></td>
                            <td>S/ <fmt:formatNumber value="${cf.importeAcumulado}"
                                                     minFractionDigits="2" maxFractionDigits="2"/></td>
                            <td>S/ <fmt:formatNumber value="${cf.ticketPromedio}"
                                                     minFractionDigits="2" maxFractionDigits="2"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty clientesFrecuentes}">
                        <tr>
                            <td colspan="5" class="sin-datos">Ningún cliente alcanza el umbral indicado.</td>
                        </tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </c:if>
    </section>
</div>
</body>
</html>
