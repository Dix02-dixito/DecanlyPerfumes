package com.cibertec.app;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class VerificarHash {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder(12);

        String hashAdmin = "$2a$12$SBniYo//ov5HosYxLxGlNO7DU.2.1wicwaGMA71M3s9JSx3O6WspG";
        String hashVendedor = "$2a$12$OEqG8KMg/Qnn9GvuIjowcezXwHcaBD9cYOt7BdhWinw6uxLDM8Fli";

        boolean adminCorrecto =
            encoder.matches("Decanly2026", hashAdmin);

        boolean vendedorCorrecto =
            encoder.matches("Vendedor2026", hashVendedor);

        System.out.println(
            "Contraseña de ADMIN correcta: " + adminCorrecto
        );

        System.out.println(
            "Contraseña de VENDEDOR correcta: " + vendedorCorrecto
        );
    }
}
