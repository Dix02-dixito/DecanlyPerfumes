package com.cibertec.app.entity;
import jakarta.persistence.*;

import lombok.*;
import java.util.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "pack")
public class Pack {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id_pack") private Long idPack;
    @Column(name="titulo_pack", length=60) private String nombre;
    @Column(name="descripcion_pack", length=180) private String descripcion;
    @Column(name="etiqueta_pack", length=40) private String etiqueta="";
    @Column(name="activo_pack") private Boolean activo=true;
    @Version @Column(name="version_pack") private Integer version;
    @OneToMany(mappedBy="pack") @OrderBy("productoId ASC")
    private List<PackDetalle> detalles=new ArrayList<>();

    public BigDecimal getSubtotal() {
        BigDecimal subtotal=BigDecimal.ZERO;
        for (PackDetalle d : detalles) {
            Presentacion p=d.getProducto().buscarPresentacion(d.getMl());
            if (p!=null) subtotal=subtotal.add(p.getPrecio().multiply(BigDecimal.valueOf(d.getCantidad())));
        }
        return subtotal;
    }
    public boolean isConStock() {
        if (detalles.isEmpty()) return false;
        for (PackDetalle d : detalles) {
            Presentacion p=d.getProducto().buscarPresentacion(d.getMl());
            if (!d.getProducto().isActivo() || p==null || p.getStock()<d.getCantidad()) return false;
        }
        return true;
    }
}
