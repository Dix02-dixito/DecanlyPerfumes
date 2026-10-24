package com.cibertec.app.service.impl;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cibertec.app.entity.Marca;
import com.cibertec.app.entity.Presentacion;
import com.cibertec.app.entity.Producto;
import com.cibertec.app.repository.MarcaRepository;
import com.cibertec.app.repository.PresentacionRepository;
import com.cibertec.app.repository.ProductoRepository;
import com.cibertec.app.service.ImagenService;
import com.cibertec.app.service.ProductoService;
import com.cibertec.app.validation.ProductoValidador;
import com.cibertec.app.entity.Imagen;
import com.cibertec.app.entity.ProductoImagen;
import com.cibertec.app.repository.ProductoImagenRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@Service
public class ProductoServiceImpl implements ProductoService {

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private MarcaRepository marcaRepository;

	@Autowired
	private PresentacionRepository presentacionRepository;

	@Autowired
	private ProductoValidador productoValidador;

	@Autowired
	private ImagenService imagenService;

	@Autowired
	private ProductoImagenRepository galeriaRepository;

	private static final int MAX_FOTOS_GALERIA = 6;

	@PersistenceContext
	private EntityManager entityManager;

	// metodo guardar
	@Override
	@Transactional
	public void guardar(Producto datos, MultipartFile foto, List<MultipartFile> fotosGaleria) {

		productoValidador.validar(datos);

		Producto nuevo = new Producto();

		copiarDatos(nuevo, datos);
		nuevo.setImagen(imagenService.guardar(foto));

		productoRepository.save(nuevo);

		guardarPresentaciones(nuevo, datos.getPresentaciones());
		guardarGaleria(nuevo, fotosGaleria, List.of());
	}

	// metodo copiar datos ayuda al metodo guardar
	private void copiarDatos(Producto destino, Producto datos) {

		Marca marca = marcaRepository.findById(datos.getMarca().getId())
				.orElseThrow(() -> new IllegalArgumentException("La marca seleccionada no existe."));

		destino.setNombre(datos.getNombre().trim());
		destino.setMarca(marca);
		destino.setOcasion(datos.getOcasion());
		destino.setDescuento(datos.getDescuento());

		destino.setNotas(datos.getNotas() == null ? "" : datos.getNotas().trim());
	}
	// metodo guardar presentacion para el metodo guardar

	private void guardarPresentaciones(Producto producto, List<Presentacion> recibidas) {

		int[] tamanos = { 3, 5, 10 };

		for (int i = 0; i < tamanos.length; i++) {

			int ml = tamanos[i];
			Presentacion presentacion = producto.buscarPresentacion(ml);

			if (presentacion == null) {
				presentacion = new Presentacion();
				presentacion.setProducto(producto);
				presentacion.setMl(ml);
				producto.getPresentaciones().add(presentacion);
			}

			presentacion.setPrecio(recibidas.get(i).getPrecio());
			presentacion.setStock(recibidas.get(i).getStock());

			presentacionRepository.save(presentacion);
		}
	}

	// metodo actualizar productos
	@Override
	@Transactional
	public void actualizar(Long id, Producto datos, MultipartFile foto, List<MultipartFile> fotosGaleria,
			List<Long> eliminarImagenes) {

		if (id == null) {
			throw new IllegalArgumentException("Falta el identificador del perfume.");
		}

		productoValidador.validar(datos);

		Producto actual = productoRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("El perfume no existe."));

		if (!Objects.equals(actual.getVersion(), datos.getVersion())) {
			throw new IllegalArgumentException("El perfume fue modificado. Recarga la página.");
		}

		copiarDatos(actual, datos);

		if (foto != null && !foto.isEmpty()) {
			actual.setImagen(imagenService.guardar(foto));
		}

		entityManager.lock(actual, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

		productoRepository.save(actual);

		guardarPresentaciones(actual, datos.getPresentaciones());
		guardarGaleria(actual, fotosGaleria, eliminarImagenes);
	}

	// METODO PARA CAMBIAR ESTADO DEL PRODUCTO(PERFUME)
	@Override
	@Transactional
	public void cambiarEstado(Long id, Integer version, boolean activo) {

		if (id == null || version == null) {
			throw new IllegalArgumentException("Faltan datos para cambiar el estado.");
		}

		Producto producto = productoRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("El perfume no existe."));

		if (!Objects.equals(producto.getVersion(), version)) {
			throw new IllegalArgumentException("El perfume fue modificado. Recarga la página.");
		}

		producto.setActivo(activo);
		productoRepository.save(producto);
	}

	// METODO GUARDAR PARA LAS FOTOS GALERIA PUES

	private void guardarGaleria(Producto producto, List<MultipartFile> archivos, List<Long> eliminarImagenes) {

		List<MultipartFile> nuevas = archivos == null ? List.of()
				: archivos.stream().filter(archivo -> archivo != null && !archivo.isEmpty()).toList();

		List<Long> quitar = eliminarImagenes == null ? List.of() : eliminarImagenes;

		if (nuevas.isEmpty() && quitar.isEmpty()) {
			return;
		}

		List<ProductoImagen> actuales = galeriaRepository.findByProductoIdOrderByOrdenAsc(producto.getId());

		// Comprueba que las fotos solicitadas pertenezcan al perfume.
		for (Long imagenId : quitar) {
			boolean pertenece = actuales.stream().anyMatch(foto -> Objects.equals(foto.getImagenId(), imagenId));

			if (!pertenece) {
				throw new IllegalArgumentException("Una fotografía seleccionada no pertenece al perfume.");
			}
		}

		long conservadas = actuales.stream().filter(foto -> !quitar.contains(foto.getImagenId())).count();

		if (conservadas + nuevas.size() > MAX_FOTOS_GALERIA) {
			throw new IllegalArgumentException(
					"La galería permite hasta " + MAX_FOTOS_GALERIA + " fotografías adicionales.");
		}

		boolean[] ocupados = new boolean[MAX_FOTOS_GALERIA];

		for (ProductoImagen foto : actuales) {
			if (quitar.contains(foto.getImagenId())) {
				galeriaRepository.delete(foto);
			} else {
				ocupados[foto.getOrden()] = true;
			}
		}

		// Libera los órdenes eliminados antes de insertar fotos nuevas.
		galeriaRepository.flush();

		for (MultipartFile archivo : nuevas) {

			int orden = 0;

			while (ocupados[orden]) {
				orden++;
			}

			Imagen imagen = imagenService.guardar(archivo);

			ProductoImagen relacion = new ProductoImagen();
			relacion.setProductoId(producto.getId());
			relacion.setImagenId(imagen.getId());
			relacion.setDescripcion(producto.getNombre());
			relacion.setOrden(orden);

			galeriaRepository.save(relacion);
			ocupados[orden] = true;
		}
	}
}
