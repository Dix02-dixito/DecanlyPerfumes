package com.cibertec.app.service.impl;

import com.cibertec.app.entity.*;
import com.cibertec.app.repository.*;
import com.cibertec.app.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MarcaServiceImpl implements MarcaService {
    @Autowired private MarcaRepository marcaRepository;

    @Override
    public List<Marca> listarTodosMarca() {
        return marcaRepository.findAllByOrderByNombreAsc();
    }
}
