-- MySQL 8.4: esquema inicial para una base NUEVA y vacía.
-- No ejecutarlo sobre tu base existente; no contiene productos, contraseñas ni DROP TABLE.

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `configuracion` (
  `id_configuracion` int NOT NULL,
  `descuento_pack_configuracion` int DEFAULT NULL,
  PRIMARY KEY (`id_configuracion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `detallepedido` (
  `iddetalle` bigint NOT NULL AUTO_INCREMENT,
  `cantidad` int NOT NULL,
  `ml` int NOT NULL,
  `precio` decimal(10,2) NOT NULL,
  `pedidoid` bigint NOT NULL,
  `productoid` bigint NOT NULL,
  PRIMARY KEY (`iddetalle`),
  KEY `FK6g6o9l64lk5i6prcd3yqfh91r` (`pedidoid`),
  KEY `FKmvasbl3q74tqn8wj76y1dyxum` (`productoid`),
  CONSTRAINT `FK6g6o9l64lk5i6prcd3yqfh91r` FOREIGN KEY (`pedidoid`) REFERENCES `pedido` (`idpedido`),
  CONSTRAINT `FKmvasbl3q74tqn8wj76y1dyxum` FOREIGN KEY (`productoid`) REFERENCES `producto` (`id_producto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `imagen` (
  `id_imagen` bigint NOT NULL AUTO_INCREMENT,
  `contenido_imagen` longblob,
  `nombre_archivo_imagen` varchar(150) DEFAULT NULL,
  `sha256_imagen` char(64) DEFAULT NULL,
  `tipo_mime_imagen` varchar(40) DEFAULT NULL,
  PRIMARY KEY (`id_imagen`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `marca` (
  `id_marca` bigint NOT NULL AUTO_INCREMENT,
  `nombre_marca` varchar(80) NOT NULL,
  PRIMARY KEY (`id_marca`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack` (
  `id_pack` bigint NOT NULL AUTO_INCREMENT,
  `activo_pack` bit(1) DEFAULT NULL,
  `descripcion_pack` varchar(180) DEFAULT NULL,
  `etiqueta_pack` varchar(40) DEFAULT NULL,
  `titulo_pack` varchar(60) DEFAULT NULL,
  `version_pack` int DEFAULT NULL,
  PRIMARY KEY (`id_pack`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pack_detalle` (
  `pack_id_pack_detalle` bigint NOT NULL,
  `producto_id_pack_detalle` bigint NOT NULL,
  `cantidad_pack_detalle` int DEFAULT NULL,
  `ml_pack_detalle` int DEFAULT NULL,
  PRIMARY KEY (`pack_id_pack_detalle`,`producto_id_pack_detalle`),
  KEY `FK1hrfk9e9wba6w2y8r3ajg0xal` (`producto_id_pack_detalle`),
  CONSTRAINT `FK1hrfk9e9wba6w2y8r3ajg0xal` FOREIGN KEY (`producto_id_pack_detalle`) REFERENCES `producto` (`id_producto`),
  CONSTRAINT `FKenhttplakuc23lo676jt7mwwq` FOREIGN KEY (`pack_id_pack_detalle`) REFERENCES `pack` (`id_pack`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pedido` (
  `idpedido` bigint NOT NULL AUTO_INCREMENT,
  `direccion` varchar(255) DEFAULT NULL,
  `estadopedido` varchar(255) DEFAULT NULL,
  `fechapedido` datetime(6) DEFAULT NULL,
  `nombrecliente` varchar(255) DEFAULT NULL,
  `telefono` varchar(255) DEFAULT NULL,
  `totalpedido` decimal(38,2) DEFAULT NULL,
  PRIMARY KEY (`idpedido`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `presentacion` (
  `id_presentacion` bigint NOT NULL AUTO_INCREMENT,
  `ml_presentacion` int DEFAULT NULL,
  `precio_presentacion` decimal(10,2) DEFAULT NULL,
  `stock_presentacion` int DEFAULT NULL,
  `producto_id_presentacion` bigint DEFAULT NULL,
  PRIMARY KEY (`id_presentacion`),
  KEY `FKlu7aby98lbtv92a6vs5e2ihfn` (`producto_id_presentacion`),
  CONSTRAINT `FKlu7aby98lbtv92a6vs5e2ihfn` FOREIGN KEY (`producto_id_presentacion`) REFERENCES `producto` (`id_producto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `producto` (
  `id_producto` bigint NOT NULL AUTO_INCREMENT,
  `activo_producto` bit(1) DEFAULT NULL,
  `descuento_producto` int DEFAULT NULL,
  `nombre_producto` varchar(100) NOT NULL,
  `notas_producto` varchar(400) DEFAULT NULL,
  `ocasion_producto` varchar(10) DEFAULT NULL,
  `version_producto` int DEFAULT NULL,
  `imagen_id_producto` bigint DEFAULT NULL,
  `marca_id_producto` bigint DEFAULT NULL,
  PRIMARY KEY (`id_producto`),
  KEY `FKsl7nx13qsynl29u9vq8f9opl3` (`imagen_id_producto`),
  KEY `FKe3gl9odxv1m1deg5v04176cn8` (`marca_id_producto`),
  CONSTRAINT `FKe3gl9odxv1m1deg5v04176cn8` FOREIGN KEY (`marca_id_producto`) REFERENCES `marca` (`id_marca`),
  CONSTRAINT `FKsl7nx13qsynl29u9vq8f9opl3` FOREIGN KEY (`imagen_id_producto`) REFERENCES `imagen` (`id_imagen`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `producto_imagen` (
  `imagen_id_producto_imagen` bigint NOT NULL,
  `producto_id_producto_imagen` bigint NOT NULL,
  `descripcion_producto_imagen` varchar(120) DEFAULT NULL,
  `orden_producto_imagen` int DEFAULT NULL,
  PRIMARY KEY (`imagen_id_producto_imagen`,`producto_id_producto_imagen`),
  KEY `FKpsf0mgaldd7fa1nvn08vbnq2x` (`producto_id_producto_imagen`),
  CONSTRAINT `FKpsf0mgaldd7fa1nvn08vbnq2x` FOREIGN KEY (`producto_id_producto_imagen`) REFERENCES `producto` (`id_producto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `activo_usuario` bit(1) NOT NULL,
  `clave_hash_usuario` varchar(255) NOT NULL,
  `correo_usuario` varchar(150) NOT NULL,
  `nombre_usuario` varchar(100) NOT NULL,
  `rol_usuario` enum('ADMIN','VENDEDOR') NOT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `UKdo5kjb0haqlicn34eexyuy1e3` (`correo_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

