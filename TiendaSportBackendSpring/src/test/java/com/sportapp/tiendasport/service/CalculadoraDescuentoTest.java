package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.model.EscalaDescuento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraDescuentoTest {

    private final List<EscalaDescuento> escala = List.of(
            new EscalaDescuento(1, new BigDecimal("200000"), new BigDecimal("5")),
            new EscalaDescuento(2, new BigDecimal("500000"), new BigDecimal("10")),
            new EscalaDescuento(3, new BigDecimal("1000000"), new BigDecimal("15")));

    private void verificar(String subtotal, String pctEsperado, String valorEsperado) {
        CalculadoraDescuento.Resultado r = CalculadoraDescuento.calcular(escala, new BigDecimal(subtotal));
        assertEquals(0, new BigDecimal(pctEsperado).compareTo(r.porcentaje()), "porcentaje para " + subtotal);
        assertEquals(0, new BigDecimal(valorEsperado).compareTo(r.valor()), "valor para " + subtotal);
    }

    @Test
    void sinAlcanzarElPrimerEscalonNoHayDescuento() { verificar("199999", "0", "0"); }

    @Test
    void enElMontoExactoDelEscalonYaAplica() {
        verificar("200000", "5", "10000");
        verificar("500000", "10", "50000");
        verificar("1000000", "15", "150000");
    }

    @Test
    void seAplicaElEscalonMasAltoAlcanzado() {
        verificar("499999", "5", "25000");
        verificar("750000", "10", "75000");
        verificar("2500000", "15", "375000");
    }

    @Test
    void sinEscalasNoHayDescuento() {
        CalculadoraDescuento.Resultado r = CalculadoraDescuento.calcular(List.of(), new BigDecimal("900000"));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.valor()));
    }

    @Test
    void elValorSeRedondeaAlPeso() {
        // 5 % de 333.333 = 16.666,65 -> 16.667
        verificar("333333", "5", "16667");
    }
}
