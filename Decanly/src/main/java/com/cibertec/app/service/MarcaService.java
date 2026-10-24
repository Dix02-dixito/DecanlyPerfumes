package com.cibertec.app.service;
import com.cibertec.app.entity.Marca;

import java.util.List;

public interface MarcaService {
	
	List<Marca> listar();
	Marca buscar(Long id);
	void guardar(Long id, String nombre);
	void eliminar(Long id);

}
