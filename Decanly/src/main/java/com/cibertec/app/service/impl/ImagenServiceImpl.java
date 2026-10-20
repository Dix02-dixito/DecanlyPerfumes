package com.cibertec.app.service.impl;

import com.cibertec.app.entity.*;
import com.cibertec.app.repository.*;
import com.cibertec.app.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

import javax.imageio.ImageIO;

@Service
public class ImagenServiceImpl implements ImagenService {
    @Autowired private ImagenRepository imagenRepository;

    @Override
    public Imagen guardarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > 2 * 1024 * 1024)
            throw new IllegalArgumentException("Selecciona una imagen de hasta 2 MB.");
        try {
            byte[] bytes = archivo.getBytes();
            String tipo = detectarTipo(bytes);
            if (!tipo.equals("image/webp")) {
                try (var entrada =
                        ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                    var lectores = ImageIO.getImageReaders(entrada);
                    if (!lectores.hasNext())
                        throw new IllegalArgumentException("No se reconoce la imagen.");
                    var lector = lectores.next();
                    try {
                        lector.setInput(entrada);
                        int ancho = lector.getWidth(0), alto = lector.getHeight(0);
                        if (ancho > 8000 || alto > 8000 || (long) ancho * alto > 16000000)
                            throw new IllegalArgumentException(
                                    "La imagen admite hasta 8000 píxeles por lado y 16"
                                        + " megapíxeles.");
                        lector.read(0);
                    } finally {
                        lector.dispose();
                    }
                }
            }
            String nombre = archivo.getOriginalFilename();
            nombre = nombre == null ? "perfume" : nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
            Imagen imagen = new Imagen();
            imagen.setNombreArchivo(nombre.substring(0, Math.min(nombre.length(), 150)));
            imagen.setTipoMime(tipo);
            imagen.setContenido(bytes);
            imagen.setSha256(
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            return imagenRepository.save(imagen);
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer la imagen.");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String detectarTipo(byte[] b) {
        if (b.length >= 8
                && b[0] == (byte) 137
                && b[1] == 80
                && b[2] == 78
                && b[3] == 71
                && b[4] == 13
                && b[5] == 10
                && b[6] == 26
                && b[7] == 10) return "image/png";
        if (b.length >= 3 && b[0] == (byte) 255 && b[1] == (byte) 216 && b[2] == (byte) 255)
            return "image/jpeg";
        if (b.length >= 30
                && new String(b, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(b, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")
                && new String(b, 12, 3, StandardCharsets.US_ASCII).equals("VP8"))
            return "image/webp";
        throw new IllegalArgumentException("El archivo debe ser una imagen PNG, JPG o WebP.");
    }

    @Override
    public Imagen buscarImagenById(Long id) {
        return imagenRepository.findById(id).orElse(null);
    }
}
