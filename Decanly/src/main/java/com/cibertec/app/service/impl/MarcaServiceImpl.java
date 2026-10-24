package com.cibertec.app.service.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.app.entity.Marca;
import com.cibertec.app.repository.MarcaRepository;
import com.cibertec.app.repository.ProductoRepository;
import com.cibertec.app.service.MarcaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarcaServiceImpl implements MarcaService {

    private final MarcaRepository marcaRepository;
    private final ProductoRepository productoRepository;

    @Override
    public List<Marca> listar() {
        return marcaRepository.findAllByOrderByNombreAsc();
    }

    @Override
    public Marca buscar(Long id) {
        return marcaRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void guardar(Long id, String nombre) {

        String limpio = nombre == null ? "" : nombre.trim();

        if (limpio.isEmpty() || limpio.length() > 80) {
            throw new IllegalArgumentException(
                    "El nombre debe tener entre 1 y 80 caracteres.");
        }

        Marca repetida = marcaRepository
                .findByNombreIgnoreCase(limpio)
                .orElse(null);

        if (repetida != null
                && !Objects.equals(repetida.getId(), id)) {
            throw new IllegalArgumentException(
                    "Ya existe una marca con ese nombre.");
        }

        Marca marca = id == null
                ? new Marca()
                : marcaRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "La marca no existe."));

        marca.setNombre(limpio);
        marcaRepository.saveAndFlush(marca);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {

        Marca marca = marcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La marca no existe."));

        if (productoRepository.existsByMarca_Id(id)) {
            throw new IllegalArgumentException(
                    "La marca está asociada a perfumes "
                    + "y no puede eliminarse.");
        }

        marcaRepository.delete(marca);
        marcaRepository.flush();
    }
}