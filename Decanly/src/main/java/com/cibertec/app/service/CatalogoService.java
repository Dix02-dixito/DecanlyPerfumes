package com.cibertec.app.service;

import com.cibertec.app.entity.Producto;
import com.cibertec.app.entity.Marca;
import com.cibertec.app.entity.Pack;
import com.cibertec.app.entity.ProductoImagen;

import java.util.List;

public interface CatalogoService {
	List<Producto> listar(String q, String marca, String ocasion, String estilo, String orden, boolean soloActivos);

	Producto buscar(Long id);

	Producto nuevo();

	List<Marca> marcas();

	List<Pack> packs();

	Pack buscarPack(Long id);

	int descuentoPack();

	List<ProductoImagen> galeria(Long id);
}
