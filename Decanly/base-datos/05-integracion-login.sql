-- SOLO para una base anterior cuya tabla usuario todavía NO tiene rol.
-- Una instalación nueva ya incluye rol en 01-esquema.sql: NO ejecutar este archivo.
-- Comprobar antes: SHOW COLUMNS FROM decanly.usuario LIKE 'rol';
USE decanly;
ALTER TABLE usuario ADD COLUMN rol VARCHAR(20) NOT NULL DEFAULT 'ADMIN' AFTER password_hash;
-- Conserva usuarios, correos, hashes BCrypt e identificadores.
