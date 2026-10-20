package com.cibertec.app.service.impl;

import com.cibertec.app.entity.*;
import com.cibertec.app.repository.*;
import com.cibertec.app.service.*;

import jakarta.persistence.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ProductoServiceImpl implements ProductoService {
    @Autowired private ProductoRepository productoRepository;
    @Autowired private MarcaRepository marcaRepository;
    @Autowired private ProductoImagenRepository productoImagenRepository;
    @Autowired private ImagenService imagenService;
    @PersistenceContext private EntityManager entityManager;
    private static final int[] TAMANOS = {3, 5, 10};

    @Override
    public List<Producto> listarTodosProductos() {
        return productoRepository.findAllByOrderByIdDesc();
    }

    @Override
    public Producto buscarProductoById(Long id) {
        return productoRepository.findById(id).orElse(null);
    }

    @Override
    public Producto nuevoProducto() {
        Producto p = new Producto();
        p.setMarca(new Marca());
        for (int ml : TAMANOS) {
            Presentacion v = new Presentacion();
            v.setMl(ml);
            v.setProducto(p);
            p.getPresentaciones().add(v);
        }
        return p;
    }

    @Override
    @Transactional
    public Producto guardarProducto(
            Producto formulario, MultipartFile foto, List<MultipartFile> galeria) {
        validar(formulario);
        Producto nuevo = nuevoProducto();
        copiarCampos(nuevo, formulario);
        nuevo.setImagen(imagenService.guardarImagen(foto));
        Producto guardado = productoRepository.save(nuevo);
        guardarGaleria(guardado, galeria, List.of());
        return guardado;
    }

    @Override
    @Transactional
    public Producto actualizarProducto(
            Long id,
            Producto formulario,
            MultipartFile foto,
            List<MultipartFile> galeria,
            List<Long> quitar) {
        validar(formulario);
        Producto actual = exigirProducto(id, formulario.getVersion());
        copiarCampos(actual, formulario);
        if (foto != null && !foto.isEmpty()) actual.setImagen(imagenService.guardarImagen(foto));
        guardarGaleria(actual, galeria, quitar);
        entityManager.lock(actual, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        return productoRepository.save(actual);
    }

    private void copiarCampos(Producto destino, Producto origen) {
        destino.setNombre(origen.getNombre().trim());
        destino.setMarca(
                marcaRepository
                        .findById(origen.getMarca().getId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "La marca seleccionada no existe.")));
        destino.setOcasion(origen.getOcasion());
        destino.setNotas(origen.getNotas() == null ? "" : origen.getNotas().trim());
        destino.setDescuento(origen.getDescuento());
        for (int i = 0; i < TAMANOS.length; i++) {
            destino.getPresentaciones()
                    .get(i)
                    .setPrecio(origen.getPresentaciones().get(i).getPrecio());
            destino.getPresentaciones()
                    .get(i)
                    .setStock(origen.getPresentaciones().get(i).getStock());
        }
    }

    private void validar(Producto p) {
        if (p.getNombre() == null || p.getNombre().isBlank() || p.getNombre().length() > 100)
            throw new IllegalArgumentException("Ingresa un nombre de hasta 100 caracteres.");
        if (p.getMarca() == null || p.getMarca().getId() == null)
            throw new IllegalArgumentException("Selecciona una marca.");
        if (p.getOcasion() == null || !List.of("Día", "Noche").contains(p.getOcasion()))
            throw new IllegalArgumentException("Selecciona Día o Noche.");
        if (p.getNotas() != null && p.getNotas().length() > 400)
            throw new IllegalArgumentException("La descripción admite hasta 400 caracteres.");
        if (p.getDescuento() == null || p.getDescuento() < 0 || p.getDescuento() > 70)
            throw new IllegalArgumentException("El descuento debe estar entre 0 y 70.");
        if (p.getPresentaciones() == null || p.getPresentaciones().size() != 3)
            throw new IllegalArgumentException("Completa las tres presentaciones.");
        for (Presentacion v : p.getPresentaciones()) {
            if (v == null
                    || v.getPrecio() == null
                    || v.getPrecio().signum() <= 0
                    || v.getPrecio().compareTo(new BigDecimal("10000")) > 0
                    || v.getPrecio().scale() > 2)
                throw new IllegalArgumentException(
                        "Precios mayores que cero, hasta S/ 10000 y con máximo dos decimales.");
            if (v.getStock() == null || v.getStock() < 0 || v.getStock() > 9999)
                throw new IllegalArgumentException("Stock entero entre 0 y 9999.");
        }
    }

    private Producto exigirProducto(Long id, Integer version) {
        Producto p =
                productoRepository
                        .findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("El perfume no existe."));
        if (version == null || !Objects.equals(p.getVersion(), version))
            throw new IllegalArgumentException(
                    "Otro cambio modificó este perfume. Vuelve a abrir Editar.");
        return p;
    }

    private void guardarGaleria(
            Producto producto, List<MultipartFile> archivos, List<Long> quitar) {
        List<ProductoImagen> actuales =
                productoImagenRepository.findByProductoIdOrderByOrdenAsc(producto.getId());
        quitar = quitar == null ? List.of() : quitar;
        for (Long id : quitar)
            if (actuales.stream().noneMatch(f -> Objects.equals(f.getImagenId(), id)))
                throw new IllegalArgumentException("Una fotografía no pertenece a este perfume.");
        List<ProductoImagen> conservar = new ArrayList<>();
        for (ProductoImagen f : actuales) if (!quitar.contains(f.getImagenId())) conservar.add(f);
        List<MultipartFile> nuevas =
                archivos == null ? List.of() : archivos.stream().filter(f -> !f.isEmpty()).toList();
        if (conservar.size() + nuevas.size() > 6)
            throw new IllegalArgumentException("La galería admite hasta seis fotos adicionales.");
        productoImagenRepository.deleteByProductoId(producto.getId());
        productoImagenRepository.flush();
        List<ProductoImagen> resultado = new ArrayList<>();
        for (ProductoImagen anterior : conservar) {
            ProductoImagen f = new ProductoImagen();
            f.setProductoId(producto.getId());
            f.setImagenId(anterior.getImagenId());
            f.setDescripcion(anterior.getDescripcion());
            f.setOrden(resultado.size());
            resultado.add(f);
        }
        for (MultipartFile archivo : nuevas) {
            Imagen imagen = imagenService.guardarImagen(archivo);
            ProductoImagen f = new ProductoImagen();
            f.setProductoId(producto.getId());
            f.setImagenId(imagen.getId());
            f.setDescripcion(producto.getNombre());
            f.setOrden(resultado.size());
            resultado.add(f);
        }
        productoImagenRepository.saveAll(resultado);
    }

    @Override
    public List<ProductoImagen> listarGaleria(Long id) {
        return productoImagenRepository.findByProductoIdOrderByOrdenAsc(id);
    }

    @Override
    @Transactional
    public void eliminarProductoById(Long id, Integer version) {
        Producto p = exigirProducto(id, version);
        p.setActivo(false);
        productoRepository.save(p);
    }

    @Override
    @Transactional
    public void restaurarProductoById(Long id, Integer version) {
        Producto p = exigirProducto(id, version);
        p.setActivo(true);
        productoRepository.save(p);
    }

    @Override
    @Transactional
    public void agotarProductoById(Long id, Integer version) {
        Producto p = exigirProducto(id, version);
        for (Presentacion v : p.getPresentaciones()) v.setStock(0);
        entityManager.lock(p, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        productoRepository.save(p);
    }
}
