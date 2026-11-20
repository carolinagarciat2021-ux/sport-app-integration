package com.sportapp.tiendasport.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Guarda los parámetros configurables del negocio. Por ahora solo tiene el
 * monto mínimo de compra mayorista, pero queda lista para agregar más
 * parámetros en el futuro sin tener que crear otra tabla. Siempre existe
 * UNA sola fila, con id = 1.
 */
@Entity
@Table(name = "configuracion_tienda")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionTienda {

    @Id
    private Integer id = 1;

    @Column(name = "monto_minimo_compra", nullable = false)
    private BigDecimal montoMinimoCompra = new BigDecimal("200000");
}
