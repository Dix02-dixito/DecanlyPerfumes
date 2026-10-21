package com.cibertec.app.controller;

import com.cibertec.app.entity.Imagen;

import com.cibertec.app.repository.ImagenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ImagenController {
	@Autowired
	private ImagenRepository imagenes;

	@GetMapping("/imagenes/{id}")
	public ResponseEntity<byte[]> imagen(@PathVariable Long id) {
		Imagen i = imagenes.findById(id).orElse(null);
		if (i == null)
			return ResponseEntity.notFound().build();
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(i.getTipoMime()))
				.header("X-Content-Type-Options", "nosniff")
				.header("Content-Security-Policy", "default-src 'none'; sandbox")
				.cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1))).body(i.getContenido());
	}
}
