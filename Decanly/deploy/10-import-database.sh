#!/bin/bash
# MySQL ejecuta este archivo solo al inicializar un volumen nuevo.
set -Eeuo pipefail

# El SQL original contiene textos como Día; no depender del charset del cliente.
# --binary-mode impide interpretar comandos del cliente dentro del archivo.
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket -uroot \
    --binary-mode=1 --local-infile=0 --default-character-set=utf8mb4 \
    --max-allowed-packet=256M --database=decanly < /opt/decanly/initial.sql
