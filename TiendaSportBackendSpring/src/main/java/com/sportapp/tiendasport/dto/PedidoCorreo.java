package com.sportapp.tiendasport.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * "Foto" de un pedido en el momento de avisar al cliente por correo. Se arma dentro de la transacción
 * y se envía después de que esta termina bien (así nunca se avisa de un pedido que no quedó guardado).
 */
public record PedidoCorreo(
        Integer idPedido,
        String estado,
        boolean compraNueva,          // true = correo de confirmación de la compra; false = cambio de estado
        String nombreCliente,
        String correoCliente,
        String telefonoCliente,
        List<Linea> productos,
        BigDecimal subtotal,
        BigDecimal descuentoPorcentaje,
        BigDecimal descuentoValor,
        BigDecimal total,
        String direccionEntrega,
        String transportadora,
        String numeroGuia) {

    public record Linea(String nombre, String talla, String color, Integer cantidad, BigDecimal precioUnitario) {
    }
}
