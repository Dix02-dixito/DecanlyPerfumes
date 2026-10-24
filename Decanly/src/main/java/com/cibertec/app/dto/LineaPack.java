package com.cibertec.app.dto;

import java.math.BigDecimal;

public record LineaPack(
        Long productoId,
        Long imagenId,
        String nombre,
        Integer ml,
        Integer cantidad,
        BigDecimal precio,
        BigDecimal subtotal,
        String error) {
}