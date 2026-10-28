#!/bin/bash
# Ejecutado solo al inicializar un volumen NUEVO por la imagen oficial de MySQL.
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket -uroot <<'SQL'
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'decanly'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON decanly.* TO 'decanly'@'%';
SQL
