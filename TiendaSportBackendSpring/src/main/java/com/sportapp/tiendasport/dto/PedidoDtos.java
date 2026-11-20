package com.sportapp.tiendasport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class PedidoDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PedidoRequest {
        private String estado = "Pendiente";
        private BigDecimal monto_total = BigDecimal.ZERO;
        // Opcionales, solo al marcar el pedido como "Enviado"
        private String transportadora;
        private String numero_guia;
    }

    @Data
    @AllArgsConstructor
    public static class PedidoResponse {
        private String mensaje;
        private Integer id_pedido;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleRequest {
        @NotNull
        private Integer id_pedido;
        @NotNull
        private Integer id_producto;
        private String talla;
        private String color;
        @NotNull @Min(1)
        private Integer cantidad;
        @NotNull
        private BigDecimal precio_unitario;
    }

    /**
     * Línea de la compra. NO tiene precio a propósito: si el cliente envía
     * "precio_unitario" o cualquier otro campo extra, se ignora. El precio
     * siempre se lee de la tabla producto.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItemCompra {
        private Integer id_producto;
        private String talla;
        private String color;
        private Integer cantidad;
    }

    @Data
    @AllArgsConstructor
    public static class PedidoCompletoResponse {
        private String mensaje;
        private Integer id_pedido;
        private BigDecimal subtotal;
        private BigDecimal descuento_porcentaje;
        private BigDecimal descuento_valor;
        private BigDecimal total;
        private String estado;
    }
}
