package com.cibertec.app.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@IdClass(ProductoImagenId.class)
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "producto_imagen")
public class ProductoImagen {

	@Id
	@Column(name = "producto_id_producto_imagen")

	private Long productoId;

	@Id
	@Column(name = "imagen_id_producto_imagen")

	private Long imagenId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "producto_id_producto_imagen", insertable = false, updatable = false)

	private Producto producto;
	@Column(name = "descripcion_producto_imagen", length = 120)

	private String descripcion;

	@Column(name = "orden_producto_imagen")

	private Integer orden;
}
