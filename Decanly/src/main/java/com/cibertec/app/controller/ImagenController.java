package com.cibertec.app.controller;

import com.cibertec.app.entity.Imagen;
import com.cibertec.app.repository.ProductoRepository;
import com.cibertec.app.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class ImagenController {
    @Autowired private ImagenService imagenService;
    @Autowired private ProductoRepository productoRepository;

    @GetMapping("/imagenes/{id}")
    public ResponseEntity<byte[]> imagen(@PathVariable Long id, Authentication auth) {
        boolean admin =
                auth != null
                        && auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && productoRepository.contarImagenPublica(id) == 0)
            return ResponseEntity.notFound().build();
        Imagen imagen = imagenService.buscarImagenById(id);
        if (imagen == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getTipoMime()))
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; img-src data:; sandbox")
                .cacheControl(CacheControl.noStore())
                .body(imagen.getContenido());
    }
}
