package com.sportapp.tiendasport.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer idPedido;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(nullable = false, length = 20)
    private String estado = "Pendiente";

    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;

    // Total calculado por el servidor al confirmar la compra (nunca viene del cliente).
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // Subtotal (suma de precios del catálogo), descuento mayorista aplicado y total = subtotal - descuento.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "descuento_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;

    @Column(name = "descuento_valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuentoValor = BigDecimal.ZERO;

    // Se llenan al pasar el pedido a "Enviado" (opcionales): empresa que lo reparte y número de guía
    @Column(length = 80)
    private String transportadora;

    @Column(name = "numero_guia", length = 60)
    private String numeroGuia;

    // Copia de la dirección del cliente al momento de la compra.
    @Column(name = "direccion_entrega", length = 150)
    private String direccionEntrega;
}
