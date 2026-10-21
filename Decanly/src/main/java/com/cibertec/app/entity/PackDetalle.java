package com.cibertec.app.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.IdClass;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@IdClass(PackDetalleId.class)
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pack_detalle")
public class PackDetalle {
	@Id
	@Column(name = "pack_id_pack_detalle")
	private Long packId;
	@Id
	@Column(name = "producto_id_pack_detalle")
	private Long productoId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pack_id_pack_detalle", insertable = false, updatable = false)
	private Pack pack;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "producto_id_pack_detalle", insertable = false, updatable = false)
	private Producto producto;
	@Column(name = "ml_pack_detalle")
	private Integer ml;
	@Column(name = "cantidad_pack_detalle")
	private Integer cantidad;
}
