package com.cibertec.app.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class IntentosLoginTest {
    private static final String CORREO = "admin@decanly.test";
    private static final String IP = "192.0.2.10";
    private final Reloj reloj = new Reloj();
    private final IntentosLogin intentos = new IntentosLogin(reloj, Duration.ofMinutes(15), 10);
    private final AtomicInteger verificaciones = new AtomicInteger();
    private final Supplier<Authentication> incorrecto = () -> {
        verificaciones.incrementAndGet();
        throw new BadCredentialsException("Credenciales incorrectas");
    };
    private final Supplier<Authentication> correcto = () -> {
        verificaciones.incrementAndGet();
        return UsernamePasswordAuthenticationToken.authenticated(CORREO, null, List.of());
    };

    @Test
    void bloqueaAlQuintoFalloYNoCompruebaMasContrasenasAunqueSeanCorrectas() {
        cuatroFallos(CORREO, IP);
        LoginBloqueadoException bloqueo = assertThrows(LoginBloqueadoException.class,
                () -> intentos.autenticar(CORREO, IP, incorrecto));
        assertEquals(900, bloqueo.getSegundosRestantes());
        assertThrows(LoginBloqueadoException.class, () -> intentos.autenticar(CORREO, IP, correcto));
        assertEquals(5, verificaciones.get());
    }

    @Test
    void reintentosNoExtiendenElBloqueoYSePuedeEntrarAlVencer() {
        bloquear();
        reloj.avanzar(Duration.ofMinutes(14));
        assertEquals(60, assertThrows(LoginBloqueadoException.class,
                () -> intentos.autenticar(CORREO, IP, incorrecto)).getSegundosRestantes());
        reloj.avanzar(Duration.ofSeconds(59));
        assertEquals(1, assertThrows(LoginBloqueadoException.class,
                () -> intentos.autenticar(CORREO, IP, correcto)).getSegundosRestantes());
        reloj.avanzar(Duration.ofSeconds(1));
        assertTrue(intentos.autenticar(CORREO, IP, correcto).isAuthenticated());
        assertEquals(6, verificaciones.get());
    }

    @Test
    void unAccesoCorrectoAntesDelBloqueoReiniciaElContador() {
        cuatroFallos(CORREO, IP);
        assertTrue(intentos.autenticar(CORREO, IP, correcto).isAuthenticated());
        cuatroFallos(CORREO, IP);
        assertThrows(LoginBloqueadoException.class, () -> intentos.autenticar(CORREO, IP, incorrecto));
    }

    @Test
    void fallosAntiguosFueraDeLaVentanaNoBloquean() {
        cuatroFallos(CORREO, IP);
        reloj.avanzar(Duration.ofMinutes(15));
        cuatroFallos(CORREO, IP);
        assertTrue(intentos.autenticar(CORREO, IP, correcto).isAuthenticated());
    }

    @Test
    void mayusculasYEspaciosNoEludenElBloqueo() {
        cuatroFallos(CORREO, IP);
        assertThrows(LoginBloqueadoException.class,
                () -> intentos.autenticar("  ADMIN@Decanly.test  ", IP, incorrecto));
        assertThrows(LoginBloqueadoException.class, () -> intentos.autenticar(CORREO, IP, correcto));
    }

    @Test
    void otrosCorreosYOtrasConexionesNoQuedanBloqueados() {
        bloquear();
        assertTrue(intentos.autenticar("otro@decanly.test", IP, correcto).isAuthenticated());
        assertTrue(intentos.autenticar(CORREO, "192.0.2.11", correcto).isAuthenticated());
        assertThrows(LoginBloqueadoException.class, () -> intentos.autenticar(CORREO, IP, correcto));
    }

    @Test
    void fallosDeInfraestructuraNoSeCuentanComoClavesIncorrectas() {
        cuatroFallos(CORREO, IP);
        assertThrows(InternalAuthenticationServiceException.class,
                () -> intentos.autenticar(CORREO, IP, () -> { throw new InternalAuthenticationServiceException("Base no disponible"); }));
        assertTrue(intentos.autenticar(CORREO, IP, correcto).isAuthenticated());
    }

    @Test
    void intentosSimultaneosNoPermitenMasDeCincoVerificaciones() throws Exception {
        ExecutorService ejecutor = Executors.newFixedThreadPool(10);
        try {
            CountDownLatch inicio = new CountDownLatch(1);
            List<Future<Boolean>> tareas = new ArrayList<>();
            for (int i = 0; i < 30; i++) tareas.add(ejecutor.submit(() -> {
                inicio.await();
                try { intentos.autenticar(CORREO, IP, incorrecto); return false; }
                catch (LoginBloqueadoException ex) { return true; }
                catch (BadCredentialsException ex) { return false; }
            }));
            inicio.countDown();
            int bloqueados = 0;
            for (Future<Boolean> tarea : tareas) if (tarea.get(10, TimeUnit.SECONDS)) bloqueados++;
            assertEquals(5, verificaciones.get());
            assertEquals(26, bloqueados);
        } finally { ejecutor.shutdownNow(); }
    }

    @Test
    void unCorreoLentoNoDetieneOtrosAccesos() throws Exception {
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        CountDownLatch dentro = new CountDownLatch(1), terminar = new CountDownLatch(1);
        try {
            Future<Authentication> lento = ejecutor.submit(() -> intentos.autenticar(CORREO, IP, () -> {
                dentro.countDown();
                try { terminar.await(5, TimeUnit.SECONDS); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                return correcto.get();
            }));
            assertTrue(dentro.await(5, TimeUnit.SECONDS));
            assertTrue(ejecutor.submit(() -> intentos.autenticar("otro@decanly.test", IP, correcto))
                    .get(2, TimeUnit.SECONDS).isAuthenticated());
            terminar.countDown();
            assertTrue(lento.get(5, TimeUnit.SECONDS).isAuthenticated());
        } finally { terminar.countDown(); ejecutor.shutdownNow(); }
    }

    @Test
    void limiteDeMemoriaNoExpulsaUnBloqueoVigenteYLiberaEntradasVencidas() {
        IntentosLogin limitado = new IntentosLogin(reloj, Duration.ofMinutes(15), 1);
        for (int i = 0; i < 4; i++) assertThrows(BadCredentialsException.class, () -> limitado.autenticar(CORREO, IP, incorrecto));
        assertThrows(LoginBloqueadoException.class, () -> limitado.autenticar(CORREO, IP, incorrecto));
        assertThrows(LoginBloqueadoException.class, () -> limitado.autenticar("otro@decanly.test", IP, correcto));
        assertThrows(LoginBloqueadoException.class, () -> limitado.autenticar(CORREO, IP, correcto));
        assertEquals(5, verificaciones.get());
        reloj.avanzar(Duration.ofMinutes(15));
        assertTrue(limitado.autenticar("otro@decanly.test", IP, correcto).isAuthenticated());
    }

    private void cuatroFallos(String correo, String ip) {
        for (int i = 0; i < 4; i++) assertThrows(BadCredentialsException.class, () -> intentos.autenticar(correo, ip, incorrecto));
    }

    private void bloquear() {
        cuatroFallos(CORREO, IP);
        assertThrows(LoginBloqueadoException.class, () -> intentos.autenticar(CORREO, IP, incorrecto));
    }

    private static class Reloj extends Clock {
        private Instant ahora = Instant.parse("2026-01-01T00:00:00Z");
        void avanzar(Duration duracion) { ahora = ahora.plus(duracion); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zona) { return this; }
        @Override public Instant instant() { return ahora; }
    }
}
