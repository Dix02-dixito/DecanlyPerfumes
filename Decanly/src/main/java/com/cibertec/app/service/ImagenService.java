package com.cibertec.app.service;

import com.cibertec.app.entity.*;

import org.springframework.web.multipart.MultipartFile;

import java.util.*;

public interface ImagenService {
    Imagen guardarImagen(MultipartFile archivo);

    Imagen buscarImagenById(Long id);
}
