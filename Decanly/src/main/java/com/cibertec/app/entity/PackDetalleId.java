package com.cibertec.app.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class PackDetalleId implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Long packId;
    private Long productoId;
}
