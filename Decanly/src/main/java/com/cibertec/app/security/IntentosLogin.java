package com.cibertec.app.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/** Contador compartido por el formulario y la API, independiente de la sesión HTTP. */
@Component
public class IntentosLogin {
    private static final int LIMITE = 5;
    private static final Duration VENTANA = Duration.ofMinutes(15);
    private final Clock reloj;
    private final Duration bloqueo;
    private final int capacidad;
    private final Map<String, Estado> intentos = new HashMap<>();

    @Autowired
    public IntentosLogin(@Value("${decanly.seguridad.login.bloqueo-segundos:900}") long segundos) {
        this(Clock.systemUTC(), Duration.ofSeconds(segundos), 10_000);
    }

    IntentosLogin(Clock reloj, Duration bloqueo, int capacidad) {
        if (bloqueo.compareTo(Duration.ofSeconds(1)) < 0 || capacidad < 1) {
            throw new IllegalArgumentException("La duración y capacidad del bloqueo deben ser positivas.");
        }
        this.reloj = reloj;
        this.bloqueo = bloqueo;
        this.capacidad = capacidad;
    }

    public Authentication autenticar(String correo, String direccion, Supplier<Authentication> autenticar) {
        String clave = clave(correo, direccion);
        Estado estado = adquirir(clave);
        try {
            // Serializa únicamente intentos del mismo correo e IP, incluidos los simultáneos.
            synchronized (estado) {
                long ahora = reloj.millis();
                if (estado.bloqueadoHasta > ahora) {
                    throw bloqueado(estado.bloqueadoHasta - ahora);
                }
                if (ahora >= estado.vence) {
                    estado.fallos = 0;
                    estado.bloqueadoHasta = 0;
                    estado.vence = ahora + VENTANA.toMillis();
                }
                try {
                    Authentication resultado = autenticar.get();
                    estado.fallos = 0;
                    estado.bloqueadoHasta = 0;
                    estado.vence = 0;
                    return resultado;
                } catch (AuthenticationServiceException ex) {
                    // Un fallo de la base de datos o del servicio no penaliza al usuario.
                    throw ex;
                } catch (AuthenticationException ex) {
                    estado.fallos++;
                    if (estado.fallos >= LIMITE) {
                        estado.bloqueadoHasta = reloj.millis() + bloqueo.toMillis();
                        estado.vence = estado.bloqueadoHasta;
                        throw bloqueado(bloqueo.toMillis());
                    }
                    throw ex;
                }
            }
        } finally {
            synchronized (intentos) {
                estado.enUso--;
                if (estado.enUso == 0 && estado.vence <= reloj.millis()) {
                    intentos.remove(clave, estado);
                }
            }
        }
    }

    private Estado adquirir(String clave) {
        synchronized (intentos) {
            Estado estado = intentos.get(clave);
            if (estado == null) {
                long ahora = reloj.millis();
                intentos.values().removeIf(e -> e.enUso == 0 && e.vence <= ahora);
                if (intentos.size() >= capacidad) {
                    // No expulsa bloqueos vigentes: variar correos no permite reiniciar un contador.
                    long espera = intentos.values().stream().mapToLong(e -> e.vence).min().orElse(ahora + 1000);
                    throw bloqueado(Math.max(1000, espera - ahora));
                }
                estado = new Estado();
                estado.vence = ahora + VENTANA.toMillis();
                intentos.put(clave, estado);
            }
            estado.enUso++;
            return estado;
        }
    }

    private LoginBloqueadoException bloqueado(long milisegundos) {
        return new LoginBloqueadoException((milisegundos + 999) / 1000);
    }

    private String clave(String correo, String direccion) {
        String identidad = (direccion == null ? "sin-direccion" : direccion)
                + "\n" + (correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identidad.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible.", ex);
        }
    }

    private static final class Estado {
        private int fallos;
        private long bloqueadoHasta;
        private volatile long vence;
        private int enUso;
    }
}
