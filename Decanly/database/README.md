# Base completa de Decanly

`001-schema.sql` contiene ahora **las tablas y los datos reales**, y sustituye al esquema vacío anterior. Conserva los datos de `DecanlysqlNuevo-verificado.sql`, probado en MySQL 8.4, y añade los campos del registro manual de pedidos. Usa UTF-8 explícito desde el inicio.

Incluye 11 tablas, 21 marcas, 50 perfumes, 150 presentaciones, 50 imágenes, 9 promociones y los 2 usuarios originales. No contiene pedidos anteriores ni fotos adicionales de galería.

## Descargar y ejecutar

El archivo pesa aproximadamente 49 MiB; GitHub puede no mostrar su contenido. Abre `001-schema.sql` y usa **Download raw file / Descargar archivo sin procesar**. También puedes descargar el ZIP de esta rama del repositorio y abrir `Decanly/database/001-schema.sql`.

Ejecuta el archivo completo en MySQL 8.4 **solo para una base nueva y vacía**. Ya incluye `CREATE DATABASE`, `USE decanly`, las tablas y los INSERT. No ejecutes antes el esquema vacío anterior ni lo importes encima de tablas existentes. `CREATE DATABASE IF NOT EXISTS` no convierte el resto del archivo en una migración reutilizable.

Al terminar, comprueba:

```sql
USE decanly;
SELECT COUNT(*) AS productos FROM producto;       -- 50
SELECT COUNT(*) AS presentaciones FROM presentacion; -- 150
SELECT COUNT(*) AS imagenes FROM imagen;           -- 50
SELECT COUNT(*) AS promociones FROM pack;          -- 9
SELECT COUNT(*) AS usuarios FROM usuario;          -- 2
```

Para instalar con Docker, sigue [PUBLICAR.md](../PUBLICAR.md). Se conserva `DB_INIT_SQL=./database/001-schema.sql` y `BOOTSTRAP_ADMIN=false`. La importación solo se ejecuta al inicializar un volumen nuevo.

## Cuentas incluidas

La copia completa conserva los hashes BCrypt de las cuentas originales ADMIN y VENDEDOR; no cambia sus contraseñas ni crea `admin@decanly.local`. El login actual de la aplicación acepta ADMIN. Los hashes no permiten recuperar las contraseñas originales.

Este archivo se incorpora completo a GitHub a petición del propietario. Antes de publicar la tienda, cambia las contraseñas de las cuentas incluidas si el repositorio es accesible a terceros. Guarda las futuras copias con cuentas y pedidos fuera de Git, por ejemplo en la carpeta local `backups/` excluida del repositorio.

## Si la base ya está instalada

Ejecuta únicamente [002-pedidos-manuales.sql](./002-pedidos-manuales.sql) antes de arrancar la nueva versión. Añade los campos de subtotal, descuento, origen manual, fecha de registro, token contra duplicados y nombre del perfume en el detalle. No altera las existencias ni las ventas anteriores. Puede repetirse sin volver a añadir las columnas.

Los pedidos anteriores conservan sus importes finales: sus descuentos históricos no se pueden reconstruir con los precios actuales. Consulta [PUBLICAR.md](../PUBLICAR.md) para la actualización desde Windows o Docker.
