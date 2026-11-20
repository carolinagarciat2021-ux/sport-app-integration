package com.sportapp.tiendasport.controller;

import com.sportapp.tiendasport.dto.MensajeResponse;
import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.EscalaDescuento;
import com.sportapp.tiendasport.service.DescuentoService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Escala de descuentos mayoristas. GET público (la tienda la muestra en las fichas de producto y en el
 * carrito, incluso a invitados). PUT solo Administrador (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/descuentos")
@RequiredArgsConstructor
public class DescuentoController {

    private final DescuentoService descuentoService;

    @Data
    public static class EscalaDto {
        private BigDecimal monto_minimo;
        private BigDecimal porcentaje;
    }

    @Data
    public static class EscalasRequest {
        @jakarta.validation.constraints.Size(max = 10, message = "máximo 10 escalones")
        private List<EscalaDto> escalas = new ArrayList<>();
    }

    @GetMapping
    public List<EscalaDto> listar() {
        return aDtos(descuentoService.listar());
    }

    @PutMapping
    public ResponseEntity<?> guardar(@Valid @RequestBody EscalasRequest request, @AuthenticationPrincipal Cliente actor) {
        List<EscalaDescuento> nuevas = new ArrayList<>();
        for (EscalaDto d : request.getEscalas()) {
            nuevas.add(new EscalaDescuento(null, d.getMonto_minimo(), d.getPorcentaje()));
        }
        descuentoService.reemplazar(nuevas, actor);
        return ResponseEntity.ok(MensajeResponse.ok("Escala de descuentos actualizada"));
    }

    private static List<EscalaDto> aDtos(List<EscalaDescuento> escalas) {
        List<EscalaDto> lista = new ArrayList<>();
        for (EscalaDescuento e : escalas) {
            EscalaDto d = new EscalaDto();
            d.setMonto_minimo(e.getMontoMinimo());
            d.setPorcentaje(e.getPorcentaje());
            lista.add(d);
        }
        return lista;
    }
}
