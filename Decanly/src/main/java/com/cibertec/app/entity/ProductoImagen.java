package com.cibertec.app.entity;

import jakarta.persistence.*;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "producto_imagen")
@IdClass(ProductoImagenId.class)
public class ProductoImagen {
    @Id
    @Column(name = "producto_id")
    private Long productoId;

    @Id
    @Column(name = "imagen_id")
    private Long imagenId;

    @Column(nullable = false, length = 120)
    private String descripcion = "";

    private Integer orden;
}
