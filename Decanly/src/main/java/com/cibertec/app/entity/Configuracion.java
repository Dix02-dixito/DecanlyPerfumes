package com.cibertec.app.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "configuracion")
public class Configuracion {
	@Id
	@Column(name = "id_configuracion")
	private Integer id;
	@Column(name = "descuento_pack_configuracion")
	private Integer descuentoPack;
}
