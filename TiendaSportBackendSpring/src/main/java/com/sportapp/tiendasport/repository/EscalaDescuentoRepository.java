package com.sportapp.tiendasport.repository;

import com.sportapp.tiendasport.model.EscalaDescuento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscalaDescuentoRepository extends JpaRepository<EscalaDescuento, Integer> {
    List<EscalaDescuento> findAllByOrderByMontoMinimoAsc();
}
