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

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

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

	@PersistenceContext
	private EntityManager entityManager;

	// TODO integrante 2: perfumes: completar; aún no cambia MySQL.
	@Override
	public void guardar(Producto producto) {
		throw new UnsupportedOperationException("Pendiente: Producto.guardar");
	}

	// metodo actualizar productos
	@Override
	@Transactional
	public void actualizar(Long id, Producto datos, MultipartFile foto) {
		{

			if (id == null) {
				throw new IllegalArgumentException("No se recibió el identificador del perfume.");
			}

			productoValidador.validar(datos);

			Producto actual = productoRepository.findById(id)
					.orElseThrow(() -> new IllegalArgumentException("El perfume no existe."));

			if (!Objects.equals(actual.getVersion(), datos.getVersion())) {
				throw new IllegalArgumentException("El perfume fue modificado. Recarga la página.");
			}

			Marca marca = marcaRepository.findById(datos.getMarca().getId())
					.orElseThrow(() -> new IllegalArgumentException("La marca seleccionada no existe."));

			actual.setNombre(datos.getNombre().trim());
			actual.setMarca(marca);
			actual.setOcasion(datos.getOcasion());

			actual.setNotas(datos.getNotas() == null ? "" : datos.getNotas().trim());

			actual.setDescuento(datos.getDescuento());

			if (foto != null && !foto.isEmpty()) {
				actual.setImagen(imagenService.guardar(foto));
			}

			entityManager.lock(actual, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

			productoRepository.save(actual);

			int[] tamanos = { 3, 5, 10 };

			for (int i = 0; i < tamanos.length; i++) {

				int ml = tamanos[i];

				Presentacion recibida = datos.getPresentaciones().get(i);

				Presentacion presentacion = presentacionRepository.findByProducto_IdAndMl(id, ml).orElse(null);

				if (presentacion == null) {
					presentacion = new Presentacion();
					presentacion.setProducto(actual);
					presentacion.setMl(ml);
				}

				presentacion.setPrecio(recibida.getPrecio());
				presentacion.setStock(recibida.getStock());

				presentacionRepository.save(presentacion);
			}
		}
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
}
