package com.sportapp.tiendasport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Un pedido del cliente con sus productos y su línea de tiempo (para la pantalla "Mis pedidos"). */
public record MiPedido(
        Integer idPedido,
        LocalDateTime fecha,
        String estado,
        BigDecimal subtotal,
        BigDecimal descuentoPorcentaje,
        BigDecimal descuentoValor,
        BigDecimal total,
        String direccionEntrega,
        String transportadora,
        String numeroGuia,
        List<PedidoConDetalle.Linea> productos,
        List<Hito> historial) {

    public record Hito(String estado, LocalDateTime fecha) {
    }
}
