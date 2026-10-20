package com.cibertec.app.service.impl;

import com.cibertec.app.entity.*;
import com.cibertec.app.repository.*;
import com.cibertec.app.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PackServiceImpl implements PackService {
    @Autowired private PackRepository packRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Pack> listarTodos() {
        return packRepository.findByActivoTrueOrderByIdPackAsc().stream()
                .filter(
                        p ->
                                !p.getDetalles().isEmpty()
                                        && p.getDetalles().stream()
                                                .allMatch(d -> d.getProducto().isActivo()))
                .toList();
    }
}
