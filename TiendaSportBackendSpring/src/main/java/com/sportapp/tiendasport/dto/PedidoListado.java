package com.sportapp.tiendasport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila del listado de pedidos para Administrador y Vendedor (incluye quién compró). */
public record PedidoListado(
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
        String numeroGuia) {
}
