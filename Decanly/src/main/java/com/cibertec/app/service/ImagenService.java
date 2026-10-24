package com.cibertec.app.service;

import org.springframework.web.multipart.MultipartFile;
import com.cibertec.app.entity.Imagen;

public interface ImagenService {

    Imagen guardar(MultipartFile archivo);
}