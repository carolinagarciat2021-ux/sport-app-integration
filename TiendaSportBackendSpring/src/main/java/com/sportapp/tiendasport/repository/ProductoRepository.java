package com.sportapp.tiendasport.repository;

import com.sportapp.tiendasport.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    // Bloquea la fila del producto mientras dura la transacción de compra,
    // para que dos compras al mismo tiempo no descuenten el mismo stock dos veces
    // (el mismo cuidado que ya teníamos en el backend anterior, ahora con JPA).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Producto> findWithLockByIdProducto(Integer idProducto);

    // Tipos (subcategorías): quita el tipo a todos los productos que lo tenían
    @Modifying(clearAutomatically = true)
    @Query("update Producto p set p.idSubcategoria = null where p.idSubcategoria = :id")
    int limpiarSubcategoria(@Param("id") Integer id);

    // Asigna el tipo solo a productos de la misma categoría del tipo
    @Modifying(clearAutomatically = true)
    @Query("update Producto p set p.idSubcategoria = :id where p.idProducto in :ids and p.idCategoria = :cat")
    int asignarSubcategoria(@Param("id") Integer id, @Param("ids") List<Integer> ids, @Param("cat") Integer idCategoria);
}
