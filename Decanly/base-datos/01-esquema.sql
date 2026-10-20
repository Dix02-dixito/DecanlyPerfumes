-- Decanly · MySQL 8.0.16+ / 8.4. Importar una vez en una base nueva.
-- No elimina tablas ni datos existentes. No contiene procedimientos almacenados.
CREATE DATABASE IF NOT EXISTS decanly CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE decanly;

CREATE TABLE marca (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 nombre VARCHAR(80) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE imagen (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 nombre_archivo VARCHAR(150) NOT NULL,
 tipo_mime VARCHAR(40) NOT NULL,
 contenido LONGBLOB NOT NULL,
 sha256 CHAR(64) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE producto (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 marca_id BIGINT NOT NULL,
 nombre VARCHAR(100) NOT NULL,
 ocasion VARCHAR(10) NOT NULL,
 notas VARCHAR(400) NOT NULL DEFAULT '',
 imagen_id BIGINT NOT NULL,
 descuento INT NOT NULL DEFAULT 0,
 activo BOOLEAN NOT NULL DEFAULT TRUE,
 version INT NOT NULL DEFAULT 0,
 CONSTRAINT fk_producto_marca FOREIGN KEY (marca_id) REFERENCES marca(id),
 CONSTRAINT fk_producto_imagen FOREIGN KEY (imagen_id) REFERENCES imagen(id),
 CONSTRAINT chk_producto_ocasion CHECK (ocasion IN ('Día','Noche')),
 CONSTRAINT chk_producto_descuento CHECK (descuento BETWEEN 0 AND 70)
) ENGINE=InnoDB;

CREATE TABLE presentacion (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 producto_id BIGINT NOT NULL,
 ml INT NOT NULL,
 precio DECIMAL(10,2) NOT NULL,
 stock INT NOT NULL DEFAULT 0,
 UNIQUE KEY uq_presentacion(producto_id,ml),
 CONSTRAINT fk_presentacion_producto FOREIGN KEY (producto_id) REFERENCES producto(id),
 CONSTRAINT chk_presentacion_ml CHECK (ml IN (3,5,10)),
 CONSTRAINT chk_presentacion_precio CHECK (precio>0 AND precio<=10000),
 CONSTRAINT chk_presentacion_stock CHECK (stock>=0)
) ENGINE=InnoDB;

CREATE TABLE producto_imagen (
 producto_id BIGINT NOT NULL,
 imagen_id BIGINT NOT NULL,
 descripcion VARCHAR(120) NOT NULL DEFAULT '',
 orden INT NOT NULL,
 PRIMARY KEY(producto_id,imagen_id),
 UNIQUE KEY uq_galeria_orden(producto_id,orden),
 CONSTRAINT fk_galeria_producto FOREIGN KEY(producto_id) REFERENCES producto(id),
 CONSTRAINT fk_galeria_imagen FOREIGN KEY(imagen_id) REFERENCES imagen(id),
 CONSTRAINT chk_galeria_orden CHECK(orden BETWEEN 0 AND 5)
) ENGINE=InnoDB;

CREATE TABLE pack (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 titulo VARCHAR(60) NOT NULL,
 descripcion VARCHAR(180) NOT NULL,
 etiqueta VARCHAR(40) NOT NULL DEFAULT '',
 activo BOOLEAN NOT NULL DEFAULT TRUE,
 version INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE pack_detalle (
 pack_id BIGINT NOT NULL,
 producto_id BIGINT NOT NULL,
 ml INT NOT NULL,
 cantidad INT NOT NULL,
 PRIMARY KEY(pack_id,producto_id),
 CONSTRAINT fk_pack_detalle_pack FOREIGN KEY(pack_id) REFERENCES pack(id) ON DELETE CASCADE,
 CONSTRAINT fk_pack_detalle_presentacion FOREIGN KEY(producto_id,ml) REFERENCES presentacion(producto_id,ml),
 CONSTRAINT chk_pack_detalle_cantidad CHECK(cantidad BETWEEN 1 AND 10)
) ENGINE=InnoDB;

CREATE TABLE configuracion (
 id INT PRIMARY KEY,
 descuento_pack INT NOT NULL DEFAULT 10,
 CONSTRAINT chk_configuracion_unica CHECK(id=1),
 CONSTRAINT chk_configuracion_descuento CHECK(descuento_pack BETWEEN 0 AND 70)
) ENGINE=InnoDB;

CREATE TABLE usuario (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 email VARCHAR(120) NOT NULL UNIQUE,
 nombre VARCHAR(120) NOT NULL,
 password_hash VARCHAR(100) NOT NULL,
 rol VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
 activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE pedido (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 referencia VARCHAR(40) NOT NULL UNIQUE,
 clave_solicitud CHAR(36) NOT NULL UNIQUE,
 payload_hash CHAR(64) NOT NULL,
 nombre_cliente VARCHAR(120) NOT NULL,
 telefono VARCHAR(15) NOT NULL,
 direccion VARCHAR(250) NOT NULL,
 acepta_terminos BOOLEAN NOT NULL,
 subtotal DECIMAL(12,2) NOT NULL,
 total DECIMAL(12,2) NOT NULL,
 envio_gratis BOOLEAN NOT NULL DEFAULT FALSE,
 estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
 creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 vence_en TIMESTAMP NOT NULL,
 INDEX idx_pedido_vencimiento(estado,vence_en),
 CONSTRAINT chk_pedido_estado CHECK(estado IN ('PENDIENTE','CONFIRMADO','ENTREGADO','CANCELADO','VENCIDO')),
 CONSTRAINT chk_pedido_total CHECK(total>=0 AND subtotal>=total),
 CONSTRAINT chk_pedido_terminos CHECK(acepta_terminos=TRUE)
) ENGINE=InnoDB;

CREATE TABLE pedido_detalle (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 pedido_id BIGINT NOT NULL,
 producto_id BIGINT NOT NULL,
 ml INT NOT NULL,
 nombre_perfume VARCHAR(100) NOT NULL,
 marca VARCHAR(80) NOT NULL,
 cantidad INT NOT NULL,
 precio_base DECIMAL(10,2) NOT NULL,
 importe DECIMAL(12,2) NOT NULL,
 grupo VARCHAR(80),
 CONSTRAINT fk_pedido_detalle_pedido FOREIGN KEY(pedido_id) REFERENCES pedido(id),
 CONSTRAINT fk_pedido_detalle_presentacion FOREIGN KEY(producto_id,ml) REFERENCES presentacion(producto_id,ml),
 CONSTRAINT chk_pedido_detalle_cantidad CHECK(cantidad BETWEEN 1 AND 99),
 CONSTRAINT chk_pedido_detalle_importe CHECK(importe>=0 AND precio_base>0)
) ENGINE=InnoDB;
