package com.cibertec.app.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ProductoImagenId implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Long productoId;
    private Long imagenId;
}
