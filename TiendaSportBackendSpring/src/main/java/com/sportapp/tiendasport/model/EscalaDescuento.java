package com.sportapp.tiendasport.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Un escalón de descuento mayorista: "desde $monto_minimo de compra, porcentaje % de descuento".
 * El Administrador define los escalones; el servidor aplica el más alto que la compra alcance.
 */
@Entity
@Table(name = "descuento_escala")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscalaDescuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_escala")
    private Integer idEscala;

    @Column(name = "monto_minimo", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoMinimo;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;
}
