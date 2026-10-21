package com.cibertec.app.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "presentacion")
public class Presentacion {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_presentacion")
	private Long id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "producto_id_presentacion")
	private Producto producto;
	@Column(name = "ml_presentacion")
	private Integer ml;
	@Column(name = "precio_presentacion", precision = 10, scale = 2)
	private BigDecimal precio;
	@Column(name = "stock_presentacion")
	private Integer stock = 0;

	// Solo muestra el precio. La venta deberá releerlo y validarlo dentro de su
	// transacción.
	public BigDecimal getPrecioFinal() {
		return precio.multiply(BigDecimal.valueOf(100 - producto.getDescuento())).divide(new BigDecimal("100"), 2,
				java.math.RoundingMode.HALF_UP);
	}
}
