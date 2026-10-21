package com.cibertec.app.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoImagenId implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private Long productoId;
	private Long imagenId;
}
