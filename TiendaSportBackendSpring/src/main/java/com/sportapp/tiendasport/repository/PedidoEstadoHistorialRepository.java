package com.sportapp.tiendasport.repository;

import com.sportapp.tiendasport.model.PedidoEstadoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PedidoEstadoHistorialRepository extends JpaRepository<PedidoEstadoHistorial, Long> {

    List<PedidoEstadoHistorial> findByIdPedidoInOrderByFechaAscIdHistorialAsc(Collection<Integer> idsPedido);

    @Modifying(clearAutomatically = true)
    @Query("delete from PedidoEstadoHistorial h where h.idPedido = :id")
    int borrarDePedido(@Param("id") Integer idPedido);
}
