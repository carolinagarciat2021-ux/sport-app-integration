package com.sportapp.tiendasport.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "producto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @NotBlank(message = "es obligatorio")
    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 100)
    private String descripcion;

    @Column(nullable = false, length = 30)
    private String talla;

    @Column(nullable = false, length = 20)
    private String color;

    @Column(nullable = false, length = 20)
    private String genero = "Unisex";

    @NotNull(message = "es obligatorio")
    @DecimalMin(value = "0.0", message = "no puede ser negativo")
    @Column(name = "precio_mayorista", nullable = false)
    private java.math.BigDecimal precioMayorista;

    @NotNull(message = "es obligatorio")
    @DecimalMin(value = "0.0", message = "no puede ser negativo")
    @Column(name = "costo_producto", nullable = false)
    private java.math.BigDecimal costoProducto;

    @NotNull(message = "es obligatorio")
    @Min(value = 0, message = "no puede ser negativo")
    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;

    @Column(name = "imagenes_por_color", columnDefinition = "TEXT")
    private String imagenesPorColor;

    @Column(name = "id_categoria")
    private Integer idCategoria;

    // Tipo dentro de la categoría (opcional). Se asigna desde la pantalla "Tipos de prenda".
    @Column(name = "id_subcategoria")
    private Integer idSubcategoria;
}
