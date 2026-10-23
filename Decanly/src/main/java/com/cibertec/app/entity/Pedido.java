package com.cibertec.app.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}