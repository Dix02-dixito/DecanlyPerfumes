package com.cibertec.app.service.impl;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Comparator;
import com.cibertec.app.dto.PedidoFormulario;
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
	@Transactional(readOnly = true)
	public Pedido prepararSolicitud(
	        String nombre,
	        String telefono,
	        String direccion,
	        boolean aceptaTerminos,
	        List<CarritoItem> carrito) {

	    pedidoValidador.validar(
	            nombre, telefono, direccion, aceptaTerminos, carrito);

	    validarPromociones(carrito);
	    validarPacksPersonalizados(carrito);

	    Map<Long, Producto> productos = consultarProductos(
	            carrito.stream()
	                    .map(CarritoItem::getProductoId)
	                    .toList());

	    int descuentoPack = catalogo.descuentoPack();
        Map<String, Integer> cantidades = new HashMap<>();

	    Pedido pedido = new Pedido();
	    pedido.setNombreCliente(nombre.trim());
	    pedido.setTelefono(telefono.trim());
	    pedido.setDireccion(direccion.trim());
	    pedido.setFechaPedido(LocalDateTime.now());
	    // Una solicitud de WhatsApp no es una venta registrada.

	    BigDecimal subtotal = BigDecimal.ZERO;

	    for (CarritoItem item : carrito) {

	        Producto producto = productos.get(item.getProductoId());

	        if (!producto.isActivo()) {
	            throw new IllegalArgumentException(
	                    producto.getNombre() + " ya no está disponible.");
	        }

	        Presentacion presentacion = presentacionRepository
	                .findByProducto_IdAndMl(producto.getId(), item.getMl())
	                .orElseThrow(() -> new IllegalArgumentException(
	                        "La presentación seleccionada ya no existe."));

	        int cantidadTotal = cantidades.merge(producto.getId() + ":" + item.getMl(), item.getCantidad(), Math::addExact);
            if (presentacion.getStock() < cantidadTotal) {
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
            detalle.setNombreProducto(producto.getNombre());
	        detalle.setMl(item.getMl());
	        detalle.setCantidad(item.getCantidad());
	        detalle.setPrecio(precio);

	        pedido.agregarDetalle(detalle);
	        subtotal = subtotal.add(detalle.getSubtotal());


	    }

	    // La solicitud incluye solo productos; la entrega se coordina con el vendedor.
	    BigDecimal total = subtotal;

	    if (total.compareTo(new BigDecimal("99999999.99")) > 0) {
	        throw new IllegalArgumentException(
	                "El importe del pedido supera el máximo permitido.");
	    }

	    pedido.setTotalPedido(total);

	    return pedido;
	}
	

    @Override
    public Long registrarManual(PedidoFormulario formulario) {
        pedidoValidador.validarManual(formulario);
        var existente = pedidoRepository.findByTokenRegistro(formulario.getToken());
        if (existente.isPresent()) return existente.get().getId();

        BigDecimal subtotal = formulario.getLineas().stream()
                .map(l -> l.getPrecioUnitario().multiply(BigDecimal.valueOf(l.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        BigDecimal descuento = formulario.getDescuento().setScale(2);
        if (subtotal.compareTo(PedidoValidador.MAXIMO) > 0 || descuento.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("El descuento no puede superar el subtotal ni el pedido exceder S/ 99 999 999.99.");
        }

        // Siempre bloquea en el mismo orden. La transacción revierte tanto stock como pedido al fallar.
        Map<Long, Producto> productos = new HashMap<>();
        for (Long id : formulario.getLineas().stream().map(l -> l.getProductoId()).distinct().sorted().toList()) {
            Producto producto = productoRepository.bloquearPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los perfumes ya no existe."));
            if (!producto.isActivo()) throw new IllegalArgumentException(producto.getNombre() + " no está activo.");
            productos.put(id, producto);
        }
        Map<String, Presentacion> presentaciones = new HashMap<>();
        Map<String, Integer> cantidades = new HashMap<>();
        for (var linea : formulario.getLineas().stream()
                .sorted(Comparator.comparing(PedidoFormulario.Linea::getProductoId)
                        .thenComparing(PedidoFormulario.Linea::getMl)).toList()) {
            String clave = linea.getProductoId() + ":" + linea.getMl();
            Presentacion presentacion = presentaciones.computeIfAbsent(clave, k -> presentacionRepository
                    .bloquearPorProductoYml(linea.getProductoId(), linea.getMl())
                    .orElseThrow(() -> new IllegalArgumentException("La presentación seleccionada ya no existe.")));
            int cantidad = cantidades.merge(clave, linea.getCantidad(), Math::addExact);
            if (presentacion.getStock() < cantidad) {
                throw new IllegalArgumentException("Stock insuficiente de " + productos.get(linea.getProductoId()).getNombre()
                        + " de " + linea.getMl() + " ml.");
            }
        }

        Pedido pedido = new Pedido();
        pedido.setNombreCliente(formulario.getNombreCliente().trim());
        pedido.setTelefono(formulario.getTelefono().trim());
        pedido.setDireccion(""); // Campo anterior conservado para no alterar el historial.
        pedido.setFechaPedido(formulario.getFechaPedido().atStartOfDay());
        pedido.setFechaRegistro(LocalDateTime.now(PedidoValidador.ZONA_VENTA));
        pedido.setEstadoPedido("ENTREGADO"); // Compatibilidad con la columna anterior, sin flujo de estados.
        pedido.setRegistroManual(true); // Solo se registran ventas ya completadas y pagadas.
        pedido.setTokenRegistro(formulario.getToken());
        pedido.setSubtotalPedido(subtotal);
        pedido.setDescuentoPedido(descuento);
        pedido.setTotalPedido(subtotal.subtract(descuento));
        for (var linea : formulario.getLineas()) {
            DetallePedido detalle = new DetallePedido();
            Producto producto = productos.get(linea.getProductoId());
            detalle.setProducto(producto);
            detalle.setNombreProducto(producto.getNombre());
            detalle.setMl(linea.getMl());
            detalle.setCantidad(linea.getCantidad());
            detalle.setPrecio(linea.getPrecioUnitario().setScale(2));
            pedido.agregarDetalle(detalle);
        }
        cantidades.forEach((clave, cantidad) -> {
            Presentacion presentacion = presentaciones.get(clave);
            presentacion.setStock(presentacion.getStock() - cantidad);
        });
        return pedidoRepository.saveAndFlush(pedido).getId();
    }

	private Map<Long, Producto> consultarProductos(List<Long> ids) {

	    Map<Long, Producto> productos = new HashMap<>();

	    for (Long id : ids.stream().distinct().sorted().toList()) {

	        Producto producto = productoRepository.findById(id)
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

	        Pack pack = packRepository.findById(id)
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