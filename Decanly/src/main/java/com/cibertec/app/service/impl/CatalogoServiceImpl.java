package com.cibertec.app.service.impl;

import com.cibertec.app.entity.*;
import com.cibertec.app.repository.*;
import com.cibertec.app.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class CatalogoServiceImpl implements CatalogoService {
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProductoImagenRepository productoImagenRepository;
    @Autowired private ConfiguracionRepository configuracionRepository;

    @Override
    public Map<String, Object> obtenerDatosTienda() {
        List<Map<String, Object>> productos = new ArrayList<>();
        for (Producto p : productoRepository.findAllByOrderByIdDesc()) {
            if (!p.isActivo()) continue;
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", p.getId());
            fila.put("name", p.getNombre());
            fila.put("brand", p.getMarca().getNombre());
            fila.put("occasion", p.getOcasion());
            fila.put("notes", p.getNotas());
            fila.put("discount", p.getDescuento());
            fila.put("version", p.getVersion());
            fila.put("image", "imagenes/" + p.getImagen().getId());
            Map<String, Object> precios = new LinkedHashMap<>(), stock = new LinkedHashMap<>();
            for (Presentacion v : p.getPresentaciones()) {
                precios.put(v.getMl().toString(), v.getPrecio());
                stock.put(v.getMl().toString(), v.getStock());
            }
            fila.put("prices", precios);
            fila.put("stock", stock);
            List<Map<String, Object>> galeria = new ArrayList<>();
            galeria.add(
                    Map.of(
                            "id",
                            "perfume",
                            "src",
                            "imagenes/" + p.getImagen().getId(),
                            "alt",
                            p.getNombre()));
            for (ProductoImagen foto :
                    productoImagenRepository.findByProductoIdOrderByOrdenAsc(p.getId()))
                galeria.add(
                        Map.of(
                                "id",
                                foto.getImagenId(),
                                "src",
                                "imagenes/" + foto.getImagenId(),
                                "alt",
                                foto.getDescripcion()));
            fila.put("gallery", galeria);
            productos.add(fila);
        }
        return Map.of(
                "products",
                productos,
                "config",
                Map.of(
                        "packDiscount",
                        configuracionRepository
                                .findById(1)
                                .map(Configuracion::getDescuentoPack)
                                .orElse(0)));
    }
}
