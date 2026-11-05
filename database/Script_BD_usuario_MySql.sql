-- =====================================================================
-- Sistema de Pedidos - Base de datos y usuario de laboratorio (MySQL 8.x)
-- Ejecutar con un usuario administrador (por ejemplo root) en MySQL Workbench
-- o con:  mysql -u root -p < database/Script_BD_usuario_MySql.sql
--
-- NOTA: si el docente entregó su propio Script_BD_usuario_MySql.sql,
-- usar ese archivo. Este script reproduce los mismos valores por defecto
-- de application.properties (pedidosdb / spedidos_app).
-- La contraseña es SOLO de laboratorio: en otro entorno cámbiela aquí y
-- defina DB_PASSWORD con el mismo valor (no la suba al repositorio).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS pedidosdb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'spedidos_app'@'localhost' IDENTIFIED BY 'userapp_2026!';

-- Permisos mínimos para la aplicación (ddl-auto=update crea/actualiza la tabla pedido).
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON pedidosdb.* TO 'spedidos_app'@'localhost';

FLUSH PRIVILEGES;

-- La tabla pedido la crea Hibernate al iniciar la aplicación
-- (spring.jpa.hibernate.ddl-auto=update) a partir de la entidad Pedido.
