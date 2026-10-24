package com.cibertec.app.service;

import org.springframework.web.multipart.MultipartFile;
import com.cibertec.app.entity.Producto;


// Integrante 2: perfumes. Firmas iniciales: completar parámetros según la guía.
public interface ProductoService {
	void guardar(Producto producto);

	void actualizar(Long id, Producto producto, MultipartFile foto);

	void cambiarEstado(Long id, Integer version, boolean activo);
}
