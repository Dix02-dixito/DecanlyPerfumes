-- MySQL 8.4. Ejecutar sobre la base EXISTENTE antes de arrancar la aplicación actualizada.
-- Añade columnas; conserva pedidos, detalles, precios, usuarios, imágenes y existencias.
-- Los importes históricos desconocidos quedan NULL, sin inventar descuentos ni pagos.
USE decanly;

SET @decanly_ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'pedido' AND column_name = 'registro_manual') = 0,
    'ALTER TABLE pedido ADD COLUMN subtotalpedido DECIMAL(10,2) NULL, ADD COLUMN descuentopedido DECIMAL(10,2) NULL, ADD COLUMN registro_manual BOOLEAN NOT NULL DEFAULT FALSE, ADD COLUMN token_registro VARCHAR(36) NULL, ADD COLUMN fecha_registro DATETIME NULL, ADD CONSTRAINT uq_pedido_token UNIQUE (token_registro), ADD CONSTRAINT chk_pedido_manual CHECK (registro_manual = FALSE OR         (subtotalpedido IS NOT NULL AND descuentopedido IS NOT NULL AND          subtotalpedido >= 0 AND descuentopedido >= 0 AND descuentopedido <= subtotalpedido AND          totalpedido IS NOT NULL AND totalpedido = subtotalpedido - descuentopedido AND          token_registro IS NOT NULL AND fecha_registro IS NOT NULL))',
    'SELECT "Campos de pedido ya actualizados" AS resultado');
PREPARE decanly_migracion FROM @decanly_ddl;
EXECUTE decanly_migracion;
DEALLOCATE PREPARE decanly_migracion;

SET @decanly_ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'detallepedido' AND column_name = 'nombreproducto') = 0,
    'ALTER TABLE detallepedido ADD COLUMN nombreproducto VARCHAR(100) NULL',
    'SELECT "Detalles ya actualizados" AS resultado');
PREPARE decanly_migracion FROM @decanly_ddl;
EXECUTE decanly_migracion;
DEALLOCATE PREPARE decanly_migracion;
SET @decanly_ddl = NULL;
