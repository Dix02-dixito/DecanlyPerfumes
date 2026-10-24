package com.cibertec.app.service;

import org.springframework.web.multipart.MultipartFile;
import com.cibertec.app.entity.Producto;

import java.util.List;

// Integrante 2: perfumes. Firmas iniciales: completar parámetros según la guía.
public interface ProductoService {
	void guardar(Producto producto, MultipartFile foto, List<MultipartFile> fotosGaleria);

	void actualizar(Long id, Producto producto, MultipartFile foto, List<MultipartFile> fotosGaleria,
			List<Long> eliminarImagenes);

	void cambiarEstado(Long id, Integer version, boolean activo);
}
