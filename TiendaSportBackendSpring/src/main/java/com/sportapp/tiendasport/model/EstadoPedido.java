package com.sportapp.tiendasport.model;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Estados válidos de un pedido. En la base de datos se guarda la etiqueta
 * ("Confirmado", "Enviado"...) para no cambiar los datos que ya existen.
 * Cualquier otro texto se rechaza.
 *
 * "Confirmado" equivale a "pagado" mientras no exista una pasarela de pago.
 */
public enum EstadoPedido {
    PENDIENTE("Pendiente"),
    CONFIRMADO("Confirmado"),
    ENVIADO("Enviado"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoPedido(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }

    /** Convierte el texto recibido (sin importar mayúsculas) o lanza IllegalArgumentException. */
    public static EstadoPedido desdeTexto(String texto) {
        if (texto != null) {
            for (EstadoPedido e : values()) {
                if (e.etiqueta.equalsIgnoreCase(texto.trim())) return e;
            }
        }
        String validos = Arrays.stream(values()).map(EstadoPedido::getEtiqueta).collect(Collectors.joining(", "));
        throw new IllegalArgumentException("Estado de pedido no válido. Los estados permitidos son: " + validos + ".");
    }
}
