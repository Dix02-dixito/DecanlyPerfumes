package com.cibertec.app.security;

import org.springframework.security.authentication.LockedException;

public class LoginBloqueadoException extends LockedException {
    private final long segundosRestantes;

    public LoginBloqueadoException(long segundosRestantes) {
        super("Demasiados intentos fallidos. El acceso está bloqueado temporalmente. Inténtalo más tarde.");
        this.segundosRestantes = Math.max(1, segundosRestantes);
    }

    public long getSegundosRestantes() {
        return segundosRestantes;
    }
}
