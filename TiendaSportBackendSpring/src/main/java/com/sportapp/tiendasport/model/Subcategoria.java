package com.sportapp.tiendasport.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * "Tipo" dentro de una categoría (ej.: categoría "Faldas Deportivas" -> tipos "Falda short", "Falda larga").
 * El catálogo muestra primero los tipos (con su imagen) y luego los productos de cada tipo.
 */
@Entity
@Table(name = "subcategoria")
@Data
@NoArgsConstructor
public class Subcategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_subcategoria")
    private Integer idSubcategoria;

    @Column(name = "id_categoria", nullable = false)
    private Integer idCategoria;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;
}
