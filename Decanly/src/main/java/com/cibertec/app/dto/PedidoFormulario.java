package com.cibertec.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

/** Solo datos editables por el administrador; ID, subtotal y total los calcula el servidor. */
@Getter
@Setter
public class PedidoFormulario {
    private String token;
    private String nombreCliente;
    private String telefono;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaPedido;
    private BigDecimal descuento = BigDecimal.ZERO;
    private List<Linea> lineas = new ArrayList<>();

    @Getter
    @Setter
    public static class Linea {
        private Long productoId;
        private Integer ml = 3;
        private Integer cantidad = 1;
        private BigDecimal precioUnitario;
    }
}
