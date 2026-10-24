package com.cibertec.app.service.impl;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cibertec.app.entity.Imagen;
import com.cibertec.app.repository.ImagenRepository;
import com.cibertec.app.service.ImagenService;

@Service
public class ImagenServiceImpl implements ImagenService {

    @Autowired
    private ImagenRepository imagenRepository;

    @Override
    @Transactional
    public Imagen guardar(MultipartFile archivo) {

        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Selecciona una fotografía.");
        }

        if (archivo.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "La fotografía permite hasta 2 MB.");
        }

        try {
            byte[] contenido = archivo.getBytes();
            String tipo = detectarTipo(contenido);

            String extension = switch (tipo) {
                case "image/png" -> "png";
                case "image/jpeg" -> "jpg";
                default -> "webp";
            };

            Imagen imagen = new Imagen();

            imagen.setNombreArchivo(
                    "perfume-" + UUID.randomUUID() + "." + extension);

            imagen.setTipoMime(tipo);
            imagen.setContenido(contenido);

            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(contenido);

            imagen.setSha256(HexFormat.of().formatHex(hash));

            return imagenRepository.save(imagen);

        } catch (IOException ex) {
            throw new IllegalArgumentException(
                    "No se pudo leer la fotografía. Selecciónala nuevamente.",
                    ex);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "No se pudo calcular SHA-256.", ex);
        }
    }

    private String detectarTipo(byte[] contenido) {

        String firma = HexFormat.of().formatHex(
                contenido, 0, Math.min(contenido.length, 12));

        if (firma.startsWith("89504e470d0a1a0a")) {
            return "image/png";
        }

        if (firma.startsWith("ffd8ff")) {
            return "image/jpeg";
        }

        if (firma.length() == 24
                && firma.startsWith("52494646")
                && firma.endsWith("57454250")) {
            return "image/webp";
        }

        throw new IllegalArgumentException(
                "Selecciona una imagen PNG, JPG o WebP.");
    }
}