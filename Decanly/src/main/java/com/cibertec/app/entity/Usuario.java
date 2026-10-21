package com.cibertec.app.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "usuario")
public class Usuario {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario")
	private Integer id;
	@Column(name = "nombre_usuario", length = 100)
	private String nombre;
	@Column(name = "correo_usuario", length = 150)
	private String correo;
	@Column(name = "clave_hash_usuario", length = 255)
	private String claveHash;
	@Column(name = "rol_usuario", columnDefinition = "ENUM('ADMIN')")
	private String rol;
	@Column(name = "activo_usuario")
	private boolean activo = true;
}
