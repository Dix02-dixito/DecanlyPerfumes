# Base de datos de Decanly

[Descargar el SQL completo](./Decanly-base-completa.sql?raw=true).

Si GitHub indica que el archivo es demasiado grande para mostrarlo, pulsa **Download raw file / Descargar archivo sin procesar**. El archivo pesa aproximadamente 49 MiB e incluye las tablas y los datos del catálogo.

Importa el archivo completo en MySQL 8.4 únicamente para una base nueva y vacía. Ya contiene `CREATE DATABASE` y `USE decanly`.

Esta descarga es una copia idéntica de `Decanly/database/001-schema.sql`, que sigue siendo el archivo utilizado por la instalación con Docker.

## Actualizar una base que ya existe

Descarga y ejecuta [002-pedidos-manuales.sql](./002-pedidos-manuales.sql?raw=true) antes de arrancar la versión de pedidos manuales. Conserva los productos, fotos, usuarios, pedidos y stock existentes. **No importes nuevamente la base completa sobre tus datos.**

La base completa de esta carpeta ya incluye los campos nuevos para una instalación desde cero. Las instrucciones de actualización están en [PUBLICAR.md](../Decanly/PUBLICAR.md).
