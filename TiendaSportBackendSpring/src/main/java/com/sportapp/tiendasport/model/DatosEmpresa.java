package com.sportapp.tiendasport.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Datos de la empresa que se muestran en la tienda (franja de promociones, pie de página,
 * botón de WhatsApp). Hay una sola fila (id 1) y la edita el Administrador desde su cuenta.
 */
@Entity
@Table(name = "datos_empresa")
@Data
@NoArgsConstructor
public class DatosEmpresa {

    @Id
    @Column(name = "id_empresa")
    private Integer idEmpresa = 1;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String lema;

    // Solo dígitos, con código de país (ej.: 573127622295)
    @Column(nullable = false, length = 20)
    private String whatsapp;

    @Column(name = "mensaje_whatsapp", length = 200)
    private String mensajeWhatsapp;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(length = 200)
    private String direccion;

    @Column(length = 200)
    private String horario;

    @Column(name = "tienda_virtual", length = 200)
    private String tiendaVirtual;

    @Column(length = 300)
    private String instagram;

    @Column(length = 300)
    private String facebook;

    // A este correo le llega un aviso cada vez que un cliente hace un pedido (opcional)
    @Column(name = "correo_avisos", length = 150)
    private String correoAvisos;

    // Compras desde este valor tienen envío gratis (0 = no se anuncia envío gratis)
    @Column(name = "envio_gratis_desde", nullable = false, precision = 12, scale = 2)
    private BigDecimal envioGratisDesde = BigDecimal.ZERO;
}
