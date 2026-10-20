package com.cibertec.app.service;

import com.cibertec.app.entity.*;

import org.springframework.web.multipart.MultipartFile;

import java.util.*;

public interface ProductoService {
    List<Producto> listarTodosProductos();

    Producto buscarProductoById(Long id);

    Producto nuevoProducto();

    Producto guardarProducto(Producto formulario, MultipartFile foto, List<MultipartFile> galeria);

    Producto actualizarProducto(
            Long id,
            Producto formulario,
            MultipartFile foto,
            List<MultipartFile> galeria,
            List<Long> eliminarImagenes);

    List<ProductoImagen> listarGaleria(Long id);

    void eliminarProductoById(Long id, Integer version);

    void restaurarProductoById(Long id, Integer version);

    void agotarProductoById(Long id, Integer version);
}
