package com.cibertec.app.entity;

import jakarta.persistence.*;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "imagen")
public class Imagen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_archivo", length = 150, nullable = false)
    private String nombreArchivo;

    @Column(name = "tipo_mime", length = 40, nullable = false)
    private String tipoMime;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] contenido;

    @Column(nullable = false, columnDefinition = "CHAR(64)")
    private String sha256;
}
