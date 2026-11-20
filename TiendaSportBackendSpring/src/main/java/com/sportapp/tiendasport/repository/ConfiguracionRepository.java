package com.sportapp.tiendasport.repository;

import com.sportapp.tiendasport.model.ConfiguracionTienda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionRepository extends JpaRepository<ConfiguracionTienda, Integer> {
}
