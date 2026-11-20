package com.sportapp.tiendasport.controller;

import com.sportapp.tiendasport.dto.MensajeResponse;
import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.Subcategoria;
import com.sportapp.tiendasport.service.SubcategoriaService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Tipos de prenda dentro de cada categoría. GET público; crear/editar/borrar solo Administrador. */
@RestController
@RequestMapping("/api/subcategorias")
@RequiredArgsConstructor
public class SubcategoriaController {

    private final SubcategoriaService servicio;

    @Data
    public static class SubcategoriaRequest {
        private String nombre;
        private Integer id_categoria;
        private String imagen_url;
    }

    @Data
    public static class AsignarRequest {
        private List<Integer> ids = new ArrayList<>();
    }

    @GetMapping
    public List<Subcategoria> listar() {
        return servicio.listar();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody SubcategoriaRequest r, @AuthenticationPrincipal Cliente actor) {
        Subcategoria s = servicio.crear(r.getNombre(), r.getId_categoria(), r.getImagen_url(), actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(s);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody SubcategoriaRequest r,
                                        @AuthenticationPrincipal Cliente actor) {
        return ResponseEntity.ok(servicio.actualizar(id, r.getNombre(), r.getImagen_url(), actor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id, @AuthenticationPrincipal Cliente actor) {
        servicio.eliminar(id, actor);
        return ResponseEntity.ok(MensajeResponse.ok("Tipo eliminado (los productos se conservan sin tipo)"));
    }

    @PutMapping("/{id}/productos")
    public ResponseEntity<?> asignarProductos(@PathVariable Integer id, @RequestBody AsignarRequest r,
                                              @AuthenticationPrincipal Cliente actor) {
        int asignados = servicio.asignarProductos(id, r.getIds(), actor);
        return ResponseEntity.ok(Map.of("mensaje", "Productos asignados al tipo", "asignados", asignados));
    }
}
