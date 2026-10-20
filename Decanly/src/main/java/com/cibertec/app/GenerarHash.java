package com.cibertec.app;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerarHash {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder(12);

        String hashAdmin = encoder.encode("Decanly2026");
        String hashVendedor = encoder.encode("Vendedor2026");

        System.out.println("HASH_ADMIN=" + hashAdmin);
        System.out.println("HASH_VENDEDOR=" + hashVendedor);
    }
}
