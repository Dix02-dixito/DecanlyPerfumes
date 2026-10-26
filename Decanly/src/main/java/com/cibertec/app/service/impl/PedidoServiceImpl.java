package com.cibertec.app.service.impl;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.cibertec.app.entity.CarritoItem;
import com.cibertec.app.entity.DetallePedido;
import com.cibertec.app.entity.Pack;
import com.cibertec.app.entity.PackDetalle;
import com.cibertec.app.entity.Presentacion;
import com.cibertec.app.entity.Producto;

import com.cibertec.app.repository.PackRepository;
import com.cibertec.app.repository.PresentacionRepository;
import com.cibertec.app.repository.ProductoRepository;

import com.cibertec.app.service.CatalogoService;
import com.cibertec.app.validation.PedidoValidador;

import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.cibertec.app.entity.Pedido;
import com.cibertec.app.repository.PedidoRepository;
import com.cibertec.app.service.PedidoService;
import com.cibertec.app.service.PoliticaEnvio;

@Service
@RequiredArgsConstructor
@Transactional
public class PedidoServiceImpl implements PedidoService {

	private final PedidoRepository pedidoRepository;
	private final ProductoRepository productoRepository;
	private final PresentacionRepository presentacionRepository;
	private final PackRepository packRepository;
	private final CatalogoService catalogo;
	private final PedidoValidador pedidoValidador;

	@Override
	public Long registrar(
	        String nombre,
	        String telefono,
	        String direccion,
	        boolean aceptaTerminos,
	        List<CarritoItem> carrito) {

	    pedidoValidador.validar(
	            nombre, telefono, direccion, aceptaTerminos, carrito);

	    validarPromociones(carrito);
	    validarPacksPersonalizados(carrito);

	    Map<Long, Producto> productos = bloquearProductos(
	            carrito.stream()
	                    .map(CarritoItem::getProductoId)
	                    .toList());

	    int descuentoPack = catalogo.descuentoPack();

	    Pedido pedido = new Pedido();
	    pedido.setNombreCliente(nombre.trim());
	    pedido.setTelefono(telefono.trim());
	    pedido.setDireccion(direccion.trim());
	    pedido.setFechaPedido(LocalDateTime.now());
	    pedido.setEstadoPedido("PENDIENTE");

	    BigDecimal subtotal = BigDecimal.ZERO;

	    for (CarritoItem item : carrito) {

	        Producto producto = productos.get(item.getProductoId());

	        if (!producto.isActivo()) {
	            throw new IllegalArgumentException(
	                    producto.getNombre() + " ya no está disponible.");
	        }

	        Presentacion presentacion = presentacionRepository
	                .bloquearPorProductoYml(producto.getId(), item.getMl())
	                .orElseThrow(() -> new IllegalArgumentException(
	                        "La presentación seleccionada ya no existe."));

	        if (presentacion.getStock() < item.getCantidad()) {
	            throw new IllegalArgumentException(
	                    "Stock insuficiente de " + producto.getNombre()
	                    + " de " + item.getMl() + " ml.");
	        }

	        boolean perteneceAPack = item.getPackId() != null
	                || item.isPackPersonalizado();

	        int descuento = Math.max(
	                producto.getDescuento(),
	                perteneceAPack ? descuentoPack : 0);

	        BigDecimal precio = presentacion.getPrecio()
	                .multiply(BigDecimal.valueOf(100 - descuento))
	                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

	        if (item.getPrecio() == null
	                || precio.compareTo(item.getPrecio()) != 0) {

	            throw new IllegalArgumentException(
	                    "El precio de " + producto.getNombre()
	                    + " cambió. Vuelve a agregarlo al carrito.");
	        }

	        DetallePedido detalle = new DetallePedido();
	        detalle.setProducto(producto);
	        detalle.setMl(item.getMl());
	        detalle.setCantidad(item.getCantidad());
	        detalle.setPrecio(precio);

	        pedido.agregarDetalle(detalle);
	        subtotal = subtotal.add(detalle.getSubtotal());

	        presentacion.setStock(
	                presentacion.getStock() - item.getCantidad());
	    }

	    BigDecimal envio = PoliticaEnvio.importe(subtotal);

	    BigDecimal total = subtotal.add(envio);

	    if (total.compareTo(new BigDecimal("99999999.99")) > 0) {
	        throw new IllegalArgumentException(
	                "El importe del pedido supera el máximo permitido.");
	    }

	    pedido.setTotalPedido(total);

	    return pedidoRepository.save(pedido).getId();
	}
	
	private Map<Long, Producto> bloquearProductos(List<Long> ids) {

	    Map<Long, Producto> productos = new HashMap<>();

	    for (Long id : ids.stream().distinct().sorted().toList()) {

	        Producto producto = productoRepository.bloquearPorId(id)
	                .orElseThrow(() -> new IllegalArgumentException(
	                        "Uno de los perfumes ya no existe."));

	        productos.put(id, producto);
	    }

	    return productos;
	}
	
	private void validarPromociones(List<CarritoItem> carrito) {

	    List<Long> ids = carrito.stream()
	            .map(CarritoItem::getPackId)
	            .filter(Objects::nonNull)
	            .distinct()
	            .sorted()
	            .toList();

	    for (Long id : ids) {

	        Pack pack = packRepository.bloquearPorId(id)
	                .orElseThrow(() -> new IllegalArgumentException(
	                        "Una promoción del carrito ya no existe."));

	        if (!Boolean.TRUE.equals(pack.getActivo())
	                || pack.getDetalles().size() < 3) {

	            throw new IllegalArgumentException(
	                    "Una promoción del carrito ya no está disponible.");
	        }

	        List<CarritoItem> seleccion = carrito.stream()
	                .filter(item -> id.equals(item.getPackId()))
	                .toList();

	        int tamanio = pack.getDetalles().size();

	        if (seleccion.size() % tamanio != 0) {
	            throw new IllegalArgumentException(
	                    "La promoción cambió. Vuelve a agregarla al carrito.");
	        }

	        int copias = seleccion.size() / tamanio;

	        for (PackDetalle detalle : pack.getDetalles()) {

	            long coincidencias = seleccion.stream()
	                    .filter(item ->
	                            Objects.equals(
	                                    item.getProductoId(), detalle.getProductoId())
	                            && Objects.equals(item.getMl(), detalle.getMl())
	                            && Objects.equals(
	                                    item.getCantidad(), detalle.getCantidad()))
	                    .count();

	            if (coincidencias != copias) {
	                throw new IllegalArgumentException(
	                        "La promoción cambió. Vuelve a agregarla al carrito.");
	            }
	        }
	    }
	}

	@Override
	public void cancelar(Long id) {
	    cambiarEstado(id, "CANCELADO");
	}

	@Override
	public void cambiarEstado(Long id, String estado) {

	    if (estado == null
	            || !List.of("CONFIRMADO", "ENTREGADO", "CANCELADO")
	                    .contains(estado)) {

	        throw new IllegalArgumentException(
	                "Selecciona un estado válido.");
	    }

	    Pedido pedido = pedidoRepository.bloquearPorId(id)
	            .orElseThrow(() -> new IllegalArgumentException(
	                    "El pedido no existe."));

	    String actual = pedido.getEstadoPedido();

	    if (estado.equals(actual)) {
	        return;
	    }

	    boolean permitido =
	            ("PENDIENTE".equals(actual)
	                    && List.of("CONFIRMADO", "CANCELADO").contains(estado))
	            || ("CONFIRMADO".equals(actual)
	                    && List.of("ENTREGADO", "CANCELADO").contains(estado));

	    if (!permitido) {
	        throw new IllegalArgumentException(
	                "No se puede pasar de " + actual + " a " + estado + ".");
	    }

	    if ("CANCELADO".equals(estado)) {

	        bloquearProductos(
	                pedido.getDetalles().stream()
	                        .map(detalle -> detalle.getProducto().getId())
	                        .toList());

	        for (DetallePedido detalle : pedido.getDetalles()) {

	            Presentacion presentacion = presentacionRepository
	                    .bloquearPorProductoYml(
	                            detalle.getProducto().getId(), detalle.getMl())
	                    .orElseThrow(() -> new IllegalArgumentException(
	                            "No se encontró una presentación del pedido."));

	            presentacion.setStock(Math.addExact(
	                    presentacion.getStock(), detalle.getCantidad()));
	        }
	    }

	    pedido.setEstadoPedido(estado);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Pedido buscar(Long id) {
	    return pedidoRepository.buscarConDetalles(id).orElse(null);
	}
	
	private void validarPacksPersonalizados(List<CarritoItem> carrito) {

	    List<CarritoItem> personalizados = carrito.stream()
	            .filter(CarritoItem::isPackPersonalizado)
	            .toList();

	    boolean incompletos = personalizados.stream().anyMatch(item ->
	            item.getPackId() != null
	            || item.getGrupoPack() == null
	            || item.getGrupoPack().isBlank());

	    if (incompletos) {
	        throw new IllegalArgumentException(
	                "Un pack personalizado no tiene una identificación válida.");
	    }

	    List<String> grupos = personalizados.stream()
	            .map(CarritoItem::getGrupoPack)
	            .distinct()
	            .toList();

	    for (String grupo : grupos) {

	        List<CarritoItem> items = carrito.stream()
	                .filter(item -> grupo.equals(item.getGrupoPack()))
	                .toList();

	        long diferentes = items.stream()
	                .map(CarritoItem::getProductoId)
	                .distinct()
	                .count();

	        if (diferentes < 3
	                || diferentes != items.size()
	                || items.stream().anyMatch(
	                        item -> !item.isPackPersonalizado())) {

	            throw new IllegalArgumentException(
	                    "Cada pack personalizado debe conservar "
	                    + "al menos tres perfumes diferentes.");
	        }
	    }
	}
}