package com.cibertec.app.service;

import java.math.BigDecimal;

/** Regla compartida por la vista del carrito y el registro del pedido. */
public final class PoliticaEnvio {
    private static final BigDecimal MINIMO_GRATIS = new BigDecimal("149");
    private static final BigDecimal TARIFA = new BigDecimal("15");

    private PoliticaEnvio() {}

    public static BigDecimal importe(BigDecimal subtotal) {
        return subtotal.compareTo(MINIMO_GRATIS) >= 0 ? BigDecimal.ZERO : TARIFA;
    }

    public static BigDecimal faltante(BigDecimal subtotal) {
        return MINIMO_GRATIS.subtract(subtotal).max(BigDecimal.ZERO);
    }
}
