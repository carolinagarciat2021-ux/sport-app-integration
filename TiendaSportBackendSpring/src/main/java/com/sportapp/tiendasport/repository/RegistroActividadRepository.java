package com.sportapp.tiendasport.repository;

import com.sportapp.tiendasport.model.RegistroActividad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroActividadRepository extends JpaRepository<RegistroActividad, Long> {

    @Query("select r from RegistroActividad r " +
           "where (:accion is null or r.accion = :accion) " +
           "and (:idUsuario is null or r.idUsuario = :idUsuario) " +
           "order by r.fecha desc, r.idRegistro desc")
    Page<RegistroActividad> buscar(@Param("accion") String accion,
                                   @Param("idUsuario") Integer idUsuario,
                                   Pageable pageable);
}
