package com.cibertec.app.service;

import com.cibertec.app.entity.Producto;


// Integrante 2: perfumes. Firmas iniciales: completar parámetros según la guía.
public interface ProductoService {
	void guardar(Producto producto);

	void actualizar(Long id, Producto producto);

	void retirar(Long id);
}
