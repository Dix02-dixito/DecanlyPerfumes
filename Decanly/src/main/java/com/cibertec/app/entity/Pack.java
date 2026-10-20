package com.cibertec.app.entity;

import com.fasterxml.jackson.annotation.*;

import jakarta.persistence.*;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pack")
public class Pack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @JsonProperty("id")
    private Long idPack;

    @Column(name = "titulo", nullable = false, length = 60)
    @JsonProperty("title")
    private String nombre;

    @Column(nullable = false, length = 180)
    @JsonProperty("text")
    private String descripcion;

    @Column(length = 40)
    @JsonProperty("badge")
    private String etiqueta = "";

    @JsonProperty("active")
    private Boolean activo = true;

    @Version @JsonIgnore private Integer version;

    @OneToMany(mappedBy = "pack", cascade = CascadeType.ALL)
    @OrderBy("productoId ASC")
    @JsonProperty("items")
    private java.util.List<PackDetalle> detalles;
}
