package com.cibertec.app.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import jakarta.persistence.OneToMany;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "producto")
public class Producto {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_producto")
	private Long id;
	@Column(name = "nombre_producto", nullable = false, length = 100)
	private String nombre;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "marca_id_producto")
	private Marca marca;
	@Column(name = "ocasion_producto", length = 10)
	private String ocasion = "Día";
	@Column(name = "notas_producto", length = 400)
	private String notas = "";
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "imagen_id_producto")
	private Imagen imagen;
	@Column(name = "descuento_producto")
	private Integer descuento = 0;
	@Column(name = "activo_producto")
	private boolean activo = true;
	@Version
	@Column(name = "version_producto")
	private Integer version;
	@OneToMany(mappedBy = "producto")
	@OrderBy("ml ASC")
	private List<Presentacion> presentaciones = new ArrayList<>();

	public boolean isConStock() {
		for (Presentacion p : presentaciones)
			if (p.getStock() > 0)
				return true;
		return false;
	}

	public Presentacion buscarPresentacion(int ml) {
		for (Presentacion p : presentaciones)
			if (p.getMl() == ml)
				return p;
		return null;
	}
}
