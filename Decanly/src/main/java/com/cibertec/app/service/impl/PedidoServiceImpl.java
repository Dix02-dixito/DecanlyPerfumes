package com.cibertec.app.service.impl;

import com.cibertec.app.entity.*;
import com.cibertec.app.service.PedidoService;
import org.springframework.stereotype.Service;

@Service
public class PedidoServiceImpl implements PedidoService {
	// TODO integrante 3: pedidos: completar; aún no cambia MySQL.
	@Override
	public void registrar() {
		throw new UnsupportedOperationException("Pendiente: Pedido.registrar");
	}

	// TODO integrante 3: pedidos: completar; aún no cambia MySQL.
	@Override
	public void cancelar(Long id) {
		throw new UnsupportedOperationException("Pendiente: Pedido.cancelar");
	}

	// TODO integrante 3: pedidos: completar; aún no cambia MySQL.
	@Override
	public void cambiarEstado(Long id, String estado) {
		throw new UnsupportedOperationException("Pendiente: Pedido.cambiarEstado");
	}
}
