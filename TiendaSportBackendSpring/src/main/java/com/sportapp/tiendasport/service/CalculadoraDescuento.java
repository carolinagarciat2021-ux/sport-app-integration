package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.model.EscalaDescuento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Cálculo del descuento mayorista (lógica pura, sin base de datos).
 * Se aplica el escalón MÁS ALTO cuyo monto mínimo alcance el subtotal.
 * El valor del descuento se redondea al peso (HALF_UP).
 */
public final class CalculadoraDescuento {

    private CalculadoraDescuento() {}

    public record Resultado(BigDecimal porcentaje, BigDecimal valor) {}

    public static Resultado calcular(List<EscalaDescuento> escalas, BigDecimal subtotal) {
        BigDecimal porcentaje = BigDecimal.ZERO;
        BigDecimal montoAlcanzado = null;
        if (escalas != null && subtotal != null) {
            for (EscalaDescuento e : escalas) {
                if (subtotal.compareTo(e.getMontoMinimo()) >= 0
                        && (montoAlcanzado == null || e.getMontoMinimo().compareTo(montoAlcanzado) > 0)) {
                    montoAlcanzado = e.getMontoMinimo();
                    porcentaje = e.getPorcentaje();
                }
            }
        }
        BigDecimal valor = subtotal == null ? BigDecimal.ZERO
                : subtotal.multiply(porcentaje).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return new Resultado(porcentaje, valor);
    }
}
