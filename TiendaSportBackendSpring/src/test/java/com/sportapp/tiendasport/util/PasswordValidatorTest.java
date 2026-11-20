package com.sportapp.tiendasport.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Pruebas unitarias de la regla de contraseñas (CP-004 / RNF-07). */
class PasswordValidatorTest {

    @Test
    void contrasenaValidaDevuelveNull() {
        assertNull(PasswordValidator.validar("Clave123*"));
    }

    @Test
    void contrasenaNulaEsRechazada() {
        assertNotNull(PasswordValidator.validar(null));
    }

    @Test
    void contrasenaCortaEsRechazada() {
        assertEquals("La contraseña debe tener mínimo 8 caracteres.",
                PasswordValidator.validar("Ab1*"));
    }

    @Test
    void contrasenaSinSimboloEsRechazada() {
        assertNotNull(PasswordValidator.validar("Clave12345"));
    }

    @Test
    void contrasenaSinNumeroEsRechazada() {
        assertNotNull(PasswordValidator.validar("Clave****"));
    }

    @Test
    void contrasenaSinLetraEsRechazada() {
        assertNotNull(PasswordValidator.validar("12345678*"));
    }
}
