package com.cibertec.app.service;

import com.cibertec.app.entity.*;

// Integrante 3: pedidos. Firmas iniciales: completar parámetros según la guía.
public interface PedidoService {
	void registrar();

	void cancelar(Long id);

	void cambiarEstado(Long id, String estado);
}
