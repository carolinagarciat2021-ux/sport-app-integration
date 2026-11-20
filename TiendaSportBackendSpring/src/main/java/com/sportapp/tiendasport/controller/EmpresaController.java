package com.sportapp.tiendasport.controller;

import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.DatosEmpresa;
import com.sportapp.tiendasport.service.EmpresaService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/** Datos de la empresa. GET público (los ve toda la tienda); PUT solo Administrador. */
@RestController
@RequestMapping("/api/empresa")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService servicio;

    @GetMapping
    public DatosEmpresa obtener(@AuthenticationPrincipal Cliente solicitante) {
        DatosEmpresa datos = servicio.obtener();
        // Solo el Administrador ve el correo privado de avisos; los visitantes no.
        boolean esAdmin = solicitante != null && solicitante.getIdRol() != null && solicitante.getIdRol() == 1;
        return esAdmin ? datos : servicio.paraPublico(datos);
    }

    @Data
    public static class EnvioGratisRequest {
        private BigDecimal envio_gratis_desde;
    }

    /** Solo el valor del envío gratis (se edita desde "Descuentos y compra mínima"). */
    @PutMapping("/envio-gratis")
    public ResponseEntity<DatosEmpresa> actualizarEnvioGratis(@RequestBody EnvioGratisRequest r, @AuthenticationPrincipal Cliente actor) {
        return ResponseEntity.ok(servicio.actualizarEnvioGratis(r.getEnvio_gratis_desde(), actor));
    }

    @PutMapping
    public ResponseEntity<DatosEmpresa> actualizar(@RequestBody DatosEmpresa datos, @AuthenticationPrincipal Cliente actor) {
        return ResponseEntity.ok(servicio.actualizar(datos, actor));
    }
}
