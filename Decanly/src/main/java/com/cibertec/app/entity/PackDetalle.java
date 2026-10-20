package com.cibertec.app.entity;

import com.fasterxml.jackson.annotation.*;

import jakarta.persistence.*;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pack_detalle")
@IdClass(PackDetalleId.class)
public class PackDetalle {
    @Id
    @Column(name = "pack_id")
    @JsonIgnore
    private Long packId;

    @Id
    @Column(name = "producto_id")
    @JsonProperty("id")
    private Long productoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id", insertable = false, updatable = false)
    @JsonIgnore
    private Pack pack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", insertable = false, updatable = false)
    @JsonIgnore
    private Producto producto;

    @JsonIgnore private Integer ml;

    @JsonProperty("qty")
    private Integer cantidad;

    @JsonProperty("size")
    public String getSize() {
        return ml.toString();
    }
}
