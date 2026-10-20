package com.cibertec.app.entity;

import jakarta.persistence.*;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "producto")
public class Producto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marca_id", nullable = false)
    private Marca marca;

    @Column(nullable = false, length = 10)
    private String ocasion = "Día";

    @Column(nullable = false, length = 400)
    private String notas = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imagen_id", nullable = false)
    private Imagen imagen;

    private Integer descuento = 0;
    private boolean activo = true;
    @Version private Integer version;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL)
    @OrderBy("ml ASC")
    private java.util.List<Presentacion> presentaciones = new java.util.ArrayList<>();

    public boolean isConStock() {
        return presentaciones.stream().anyMatch(v -> v.getStock() != null && v.getStock() > 0);
    }
}
