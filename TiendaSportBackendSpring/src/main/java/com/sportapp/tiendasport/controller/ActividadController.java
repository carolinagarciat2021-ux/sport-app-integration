package com.sportapp.tiendasport.controller;

import com.sportapp.tiendasport.model.RegistroActividad;
import com.sportapp.tiendasport.repository.RegistroActividadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta del historial de actividad. SOLO el Administrador (ver SecurityConfig).
 * Es de solo lectura: no existe ningún endpoint para editar ni borrar registros.
 */
@RestController
@RequestMapping("/api/actividad")
@RequiredArgsConstructor
public class ActividadController {

    private final RegistroActividadRepository repositorio;

    @GetMapping
    public Map<String, Object> listar(@RequestParam(defaultValue = "0") int pagina,
                                      @RequestParam(defaultValue = "50") int tamano,
                                      @RequestParam(required = false) String accion,
                                      @RequestParam(name = "id_usuario", required = false) Integer idUsuario) {
        int p = Math.max(pagina, 0);
        int t = Math.min(Math.max(tamano, 1), 200);
        String filtroAccion = (accion == null || accion.isBlank()) ? null : accion.trim();

        Page<RegistroActividad> resultado = repositorio.buscar(filtroAccion, idUsuario, PageRequest.of(p, t));

        Map<String, Object> respuesta = new LinkedHashMap<>();
        List<RegistroActividad> contenido = resultado.getContent();
        respuesta.put("registros", contenido);
        respuesta.put("pagina", resultado.getNumber());
        respuesta.put("total_paginas", resultado.getTotalPages());
        respuesta.put("total_registros", resultado.getTotalElements());
        return respuesta;
    }
}
