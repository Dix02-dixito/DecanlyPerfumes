package com.cibertec.app.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Lob;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "imagen")
public class Imagen {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_imagen")
	private Long id;
	@Column(name = "nombre_archivo_imagen", length = 150)
	private String nombreArchivo;
	@Column(name = "tipo_mime_imagen", length = 40)
	private String tipoMime;
	@Lob
	@Column(name = "contenido_imagen", columnDefinition = "LONGBLOB")
	private byte[] contenido;
	@Column(name = "sha256_imagen", columnDefinition = "CHAR(64)", length = 64)
	private String sha256;
}
