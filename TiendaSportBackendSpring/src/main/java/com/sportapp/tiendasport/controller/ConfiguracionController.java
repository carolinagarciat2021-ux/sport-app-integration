// NUEVO ARCHIVO — guárdalo en:
// src/main/java/com/sportapp/tiendasport/controller/ConfiguracionController.java

package com.sportapp.tiendasport.controller;

import com.sportapp.tiendasport.dto.MensajeResponse;
import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.ConfiguracionTienda;
import com.sportapp.tiendasport.repository.ConfiguracionRepository;
import com.sportapp.tiendasport.service.ActividadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionRepository configuracionRepository;
    private final ActividadService actividadService;

    // Público (ver SecurityConfig): la web y la app Android necesitan leer el
    // monto mínimo vigente ANTES de dejar confirmar una compra, incluso si
    // quien compra todavía no inició sesión.
    // Si la fila todavía no existe (primera vez que corre el backend), la crea
    // sola con el valor por defecto de $200.000 — no hace falta tocar la base
    // de datos a mano.
    @GetMapping
    public ResponseEntity<ConfiguracionTienda> obtener() {
        ConfiguracionTienda config = configuracionRepository.findById(1)
                .orElseGet(() -> configuracionRepository.save(new ConfiguracionTienda(1, new BigDecimal("200000"))));
        return ResponseEntity.ok(config);
    }

    // Protegido en SecurityConfig: solo Administrador puede cambiar el monto mínimo.
    @PutMapping
    public ResponseEntity<?> actualizar(@RequestBody ConfiguracionTienda nuevaConfig, @AuthenticationPrincipal Cliente actor) {
        if (nuevaConfig.getMontoMinimoCompra() == null || nuevaConfig.getMontoMinimoCompra().compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().body(MensajeResponse.error("El monto mínimo no puede ser negativo."));
        }
        ConfiguracionTienda config = configuracionRepository.findById(1)
                .orElse(new ConfiguracionTienda(1, nuevaConfig.getMontoMinimoCompra()));
        BigDecimal anterior = config.getMontoMinimoCompra();
        config.setMontoMinimoCompra(nuevaConfig.getMontoMinimoCompra());
        configuracionRepository.save(config);
        actividadService.registrar(actor, "MINIMO_COMPRA_ACTUALIZADO", "configuracion", 1,
                "Monto mínimo de compra: $" + anterior.toPlainString() + " -> $" + nuevaConfig.getMontoMinimoCompra().toPlainString());
        return ResponseEntity.ok(MensajeResponse.ok(
            "Monto mínimo de compra actualizado a $" + nuevaConfig.getMontoMinimoCompra().toPlainString()));
    }
}
