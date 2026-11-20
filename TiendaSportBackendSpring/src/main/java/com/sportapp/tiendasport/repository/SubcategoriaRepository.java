package com.sportapp.tiendasport.repository;

import com.sportapp.tiendasport.model.Subcategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubcategoriaRepository extends JpaRepository<Subcategoria, Integer> {
    List<Subcategoria> findAllByOrderByIdCategoriaAscNombreAsc();
    boolean existsByIdCategoriaAndNombreIgnoreCase(Integer idCategoria, String nombre);
    boolean existsByIdCategoriaAndNombreIgnoreCaseAndIdSubcategoriaNot(Integer idCategoria, String nombre, Integer idSubcategoria);
}
