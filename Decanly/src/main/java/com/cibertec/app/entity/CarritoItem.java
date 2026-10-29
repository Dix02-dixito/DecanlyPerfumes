package com.cibertec.app.entity;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CarritoItem {

    private String lineaId = UUID.randomUUID().toString();

    private Long productoId;
    private Long imagenId;
    private Long packId;

    private String nombre;

    private Integer ml;
    private Integer cantidad;
    
    private boolean packPersonalizado;
    private String grupoPack;

    private BigDecimal precio;
    

    public BigDecimal getSubtotal() {
        return precio.multiply(BigDecimal.valueOf(cantidad));
    }
}
