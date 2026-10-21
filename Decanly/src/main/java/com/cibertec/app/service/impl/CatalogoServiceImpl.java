package com.cibertec.app.service.impl;

import com.cibertec.app.entity.*;
import com.cibertec.app.repository.*;
import com.cibertec.app.service.CatalogoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.text.Normalizer;

// Infraestructura de CONSULTA para que Thymeleaf muestre los valores reales del SQL.
// Las operaciones de negocio están pendientes en los otros cuatro servicios.
@Service
@Transactional(readOnly = true)
public class CatalogoServiceImpl implements CatalogoService {
	@Autowired
	private ProductoRepository productos;
	@Autowired
	private MarcaRepository marcas;
	@Autowired
	private PackRepository packs;
	@Autowired
	private ConfiguracionRepository configuracion;
	@Autowired
	private ProductoImagenRepository fotos;

	public List<Producto> listar(String q, String marca, String ocasion, String estilo, String orden,
			boolean soloActivos) {
		List<Producto> resultado = new ArrayList<>();
		for (Producto p : productos.findAllByOrderByIdAsc()) {
			String texto = normalizar(p.getNombre() + " " + p.getMarca().getNombre() + " " + p.getNotas());
			if (soloActivos && !p.isActivo())
				continue;
			if (!texto.contains(normalizar(q)))
				continue;
			if (!marca.isBlank() && !p.getMarca().getNombre().equalsIgnoreCase(marca))
				continue;
			if (!ocasion.isBlank() && !p.getOcasion().equals(ocasion))
				continue;
			if (!coincideEstilo(p, estilo))
				continue;
			resultado.add(p);
		}
		if (orden.equals("name"))
			resultado.sort(Comparator.comparing(Producto::getNombre));
		if (orden.equals("price-up"))
			resultado.sort(Comparator.comparing(p -> p.buscarPresentacion(3).getPrecioFinal()));
		if (orden.equals("price-down"))
			resultado.sort((a, b) -> b.buscarPresentacion(3).getPrecioFinal()
					.compareTo(a.buscarPresentacion(3).getPrecioFinal()));
		return resultado;
	}

	private String normalizar(String texto) {
		return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
				.trim();
	}

	private boolean coincideEstilo(Producto p, String estilo) {
		if (estilo.isBlank())
			return true;
		if (estilo.equals("Verano"))
			return p.getOcasion().equals("Día");
		if (estilo.equals("Invierno"))
			return p.getOcasion().equals("Noche");
		String palabras;
		if (estilo.equals("Dulce"))
			palabras = "vainilla|caramelo|miel|haba tonka|toffee";
		else if (estilo.equals("Amaderado"))
			palabras = "amaderado|sandalo|cedro|vetiver|pachuli";
		else if (estilo.equals("Floral"))
			palabras = "floral|jazmin|iris|lavanda|geranio|rosa";
		else if (estilo.equals("Limpio"))
			palabras = "almizcle|marino|bergamota|lima|limon";
		else
			return false;
		for (String palabra : palabras.split("\\|"))
			if (normalizar(p.getNotas()).contains(palabra))
				return true;
		return false;
	}

	public Producto buscar(Long id) {
		return productos.findById(id).orElse(null);
	}

	public Pack buscarPack(Long id) {
		return packs.findById(id).orElse(null);
	}

	public List<Marca> marcas() {
		return marcas.findAllByOrderByNombreAsc();
	}

	public List<Pack> packs() {
		return packs.findByActivoTrueOrderByIdPackAsc();
	}

	public List<ProductoImagen> galeria(Long id) {
		return fotos.findByProductoIdOrderByOrdenAsc(id);
	}

	public int descuentoPack() {
		Configuracion c = configuracion.findById(1).orElse(null);
		return c == null ? 0 : c.getDescuentoPack();
	}

	public Producto nuevo() {
		Producto p = new Producto();
		p.setMarca(new Marca());
		for (int ml : new int[] { 3, 5, 10 }) {
			Presentacion v = new Presentacion();
			v.setMl(ml);
			v.setProducto(p);
			p.getPresentaciones().add(v);
		}
		return p;
	}
}
