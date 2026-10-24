package com.cibertec.app.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenPack(
        List<LineaPack> items,
        BigDecimal subtotal,
        BigDecimal ahorro,
        BigDecimal total,
        int perfumes,
        int unidades,
        int faltantes,
        boolean listo) {
}