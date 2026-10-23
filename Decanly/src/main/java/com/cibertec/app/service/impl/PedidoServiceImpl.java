package com.cibertec.app.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.cibertec.app.entity.Pedido;
import com.cibertec.app.repository.PedidoRepository;
import com.cibertec.app.service.PedidoService;

@Service
public class PedidoServiceImpl
        implements PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoServiceImpl(
            PedidoRepository pedidoRepository) {

        this.pedidoRepository = pedidoRepository;
    }

    @Override
    public void registrar(
            String nombre,
            String telefono,
            String direccion,
            BigDecimal total) {

        Pedido pedido = new Pedido();

        pedido.setNombreCliente(nombre);
        pedido.setTelefono(telefono);
        pedido.setDireccion(direccion);

        pedido.setFechaPedido(
                LocalDateTime.now());

        pedido.setEstadoPedido(
                "PENDIENTE");

        pedido.setTotalPedido(total);

        pedidoRepository.save(pedido);
    }

    @Override
    public void cancelar(Long id) {

        Pedido pedido =
                pedidoRepository
                        .findById(id)
                        .orElse(null);

        if (pedido != null) {

            pedido.setEstadoPedido(
                    "CANCELADO");

            pedidoRepository.save(
                    pedido);
        }
    }

    @Override
    public void cambiarEstado(
            Long id,
            String estado) {

        Pedido pedido =
                pedidoRepository
                        .findById(id)
                        .orElse(null);

        if (pedido != null) {

            pedido.setEstadoPedido(
                    estado);

            pedidoRepository.save(
                    pedido);
        }
    }
}