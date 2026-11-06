package com.cibertec.app.entity;

import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pedido")
public class Pedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "idpedido")
	private Long id;

	@Column(name = "nombrecliente")
	private String nombreCliente;

	@Column(name = "telefono")
	private String telefono;

	@Column(name = "direccion")
	private String direccion;

	@Column(name = "fechapedido")
	private LocalDateTime fechaPedido;

	@Column(name = "estadopedido")
	private String estadoPedido;

	@Column(name = "totalpedido")
	private BigDecimal totalPedido;

    @Column(name = "subtotalpedido", precision = 10, scale = 2)
    private BigDecimal subtotalPedido;

    @Column(name = "descuentopedido", precision = 10, scale = 2)
    private BigDecimal descuentoPedido;

    @Column(name = "registro_manual", nullable = false)
    private boolean registroManual;

    @Column(name = "token_registro", length = 36, unique = true)
    private String tokenRegistro;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    public String getPerfumesComprados() {
        return detalles.stream().map(DetallePedido::getNombrePerfume)
                .distinct().collect(java.util.stream.Collectors.joining(", "));
    }

	@OneToMany(mappedBy = "pedido", cascade = CascadeType.PERSIST)
	@OrderBy("id ASC")
	private List<DetallePedido> detalles = new ArrayList<>();

	public void agregarDetalle(DetallePedido detalle) {
		detalle.setPedido(this);
		detalles.add(detalle);
	}
}
