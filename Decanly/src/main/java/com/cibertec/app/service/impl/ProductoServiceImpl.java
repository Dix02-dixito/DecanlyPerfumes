package com.cibertec.app.service.impl;

import com.cibertec.app.entity.Producto;
import com.cibertec.app.service.ProductoService;
import org.springframework.stereotype.Service;

@Service
public class ProductoServiceImpl implements ProductoService {
	// TODO integrante 2: perfumes: completar; aún no cambia MySQL.
	@Override
	public void guardar(Producto producto) {
		throw new UnsupportedOperationException("Pendiente: Producto.guardar");
	}

	// TODO integrante 2: perfumes: completar; aún no cambia MySQL.
	@Override
	public void actualizar(Long id, Producto producto) {
		throw new UnsupportedOperationException("Pendiente: Producto.actualizar");
	}

	// TODO integrante 2: perfumes: completar; aún no cambia MySQL.
	@Override
	public void retirar(Long id) {
		throw new UnsupportedOperationException("Pendiente: Producto.retirar");
	}
}
