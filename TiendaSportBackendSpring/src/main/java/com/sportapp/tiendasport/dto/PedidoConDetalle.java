package com.sportapp.tiendasport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Pedido con quién lo hizo y todos sus productos, en una sola respuesta (sin llamadas extra). */
public record PedidoConDetalle(
        Integer idPedido,
        LocalDateTime fecha,
        String estado,
        BigDecimal subtotal,
        BigDecimal descuentoPorcentaje,
        BigDecimal descuentoValor,
        BigDecimal total,
        String direccionEntrega,
        Integer idCliente,
        String nombreCliente,
        String apellidoCliente,
        String correoCliente,
        String telefonoCliente,
        String transportadora,
        String numeroGuia,
        List<Linea> productos) {

    public record Linea(
            Integer idProducto,
            String nombreProducto,
            String talla,
            String color,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal,
            String imagenUrl) {
    }

    public record Pagina(List<PedidoConDetalle> pedidos, int pagina, int totalPaginas, long totalRegistros) {
    }
}
