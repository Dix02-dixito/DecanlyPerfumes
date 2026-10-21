package com.cibertec.app.entity;

import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackDetalleId implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private Long packId;
	private Long productoId;
}
