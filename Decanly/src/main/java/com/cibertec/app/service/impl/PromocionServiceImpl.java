package com.cibertec.app.service.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.app.entity.Pack;
import com.cibertec.app.entity.PackDetalle;
import com.cibertec.app.entity.Producto;
import com.cibertec.app.repository.PackDetalleRepository;
import com.cibertec.app.repository.PackRepository;
import com.cibertec.app.repository.ProductoRepository;
import com.cibertec.app.service.PromocionService;
import com.cibertec.app.validation.PromocionValidador;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromocionServiceImpl implements PromocionService {

    private final PackRepository packRepository;
    private final PackDetalleRepository packDetalleRepository;
    private final ProductoRepository productoRepository;
    private final PromocionValidador promocionValidador;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Pack> listar() {
        return packRepository.findAllByOrderByIdPackAsc();
    }

    @Override
    @Transactional
    public void guardar(
            Long id,
            Pack datos,
            List<Long> productoIds,
            List<Integer> ml,
            List<Integer> cantidades) {

        promocionValidador.validar(
                datos, productoIds, ml, cantidades);

        Pack pack = id == null
                ? new Pack()
                : buscarParaModificar(id, datos.getVersion());

        pack.setNombre(datos.getNombre().trim());
        pack.setDescripcion(datos.getDescripcion().trim());
        pack.setEtiqueta(datos.getEtiqueta() == null
                ? "" : datos.getEtiqueta().trim());

        if (id != null) {
            entityManager.lock(
                    pack, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        }

        packRepository.save(pack);

        guardarDetalles(pack, productoIds, ml, cantidades);
    }

    @Override
    @Transactional
    public void cambiarEstado(
            Long id, Integer version, boolean activo) {

        Pack pack = buscarParaModificar(id, version);

        pack.setActivo(activo);
        packRepository.save(pack);
    }

    private Pack buscarParaModificar(Long id, Integer version) {

        if (id == null || version == null) {
            throw new IllegalArgumentException(
                    "Faltan datos de la promoción.");
        }

        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La promoción no existe."));

        if (!Objects.equals(pack.getVersion(), version)) {
            throw new IllegalArgumentException(
                    "La promoción fue modificada. Recarga la página.");
        }

        return pack;
    }

    private void guardarDetalles(
            Pack pack,
            List<Long> productoIds,
            List<Integer> ml,
            List<Integer> cantidades) {

        List<PackDetalle> existentes = packDetalleRepository
                .findByPackIdOrderByProductoIdAsc(pack.getIdPack());

        for (PackDetalle detalle : existentes) {
            if (!productoIds.contains(detalle.getProductoId())) {
                packDetalleRepository.delete(detalle);
            }
        }

        for (int i = 0; i < productoIds.size(); i++) {

            Long productoId = productoIds.get(i);
            Integer tamano = ml.get(i);

            Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Uno de los perfumes seleccionados no existe."));

            if (producto.buscarPresentacion(tamano) == null) {
                throw new IllegalArgumentException(
                        producto.getNombre()
                                + " no tiene presentación de "
                                + tamano + " ml.");
            }

            PackDetalle detalle = existentes.stream()
                    .filter(d -> d.getProductoId().equals(productoId))
                    .findFirst()
                    .orElseGet(PackDetalle::new);

            detalle.setPackId(pack.getIdPack());
            detalle.setProductoId(productoId);
            detalle.setPack(pack);
            detalle.setProducto(producto);
            detalle.setMl(tamano);
            detalle.setCantidad(cantidades.get(i));

            packDetalleRepository.save(detalle);
        }
    }
}