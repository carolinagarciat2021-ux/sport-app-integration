package com.sportapp.tiendasport.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas unitarias de los tokens JWT (RNF-03). */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void preparar() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "clave-solo-para-pruebas-unitarias-0123456789");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 60000L);
    }

    @Test
    void tokenGuardaCorreoIdYRol() {
        String token = jwtUtil.generarToken(5, "cliente@correo.com", 2);

        assertEquals("cliente@correo.com", jwtUtil.extraerCorreo(token));
        assertEquals(5, jwtUtil.extraerIdClientes(token));
        assertEquals(2, jwtUtil.extraerIdRol(token));
    }

    @Test
    void tokenVigenteConElCorreoCorrectoEsValido() {
        String token = jwtUtil.generarToken(5, "cliente@correo.com", 2);
        assertTrue(jwtUtil.tokenValido(token, "cliente@correo.com"));
    }

    @Test
    void tokenConOtroCorreoNoEsValido() {
        String token = jwtUtil.generarToken(5, "cliente@correo.com", 2);
        assertFalse(jwtUtil.tokenValido(token, "otro@correo.com"));
    }

    @Test
    void tokenVencidoNoEsValido() {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", -1000L);
        String token = jwtUtil.generarToken(5, "cliente@correo.com", 2);
        assertFalse(jwtUtil.tokenValido(token, "cliente@correo.com"));
    }

    @Test
    void tokenAlteradoNoEsValido() {
        String token = jwtUtil.generarToken(5, "cliente@correo.com", 2);
        String alterado = token.substring(0, token.length() - 3) + "abc";
        assertFalse(jwtUtil.tokenValido(alterado, "cliente@correo.com"));
    }
}
