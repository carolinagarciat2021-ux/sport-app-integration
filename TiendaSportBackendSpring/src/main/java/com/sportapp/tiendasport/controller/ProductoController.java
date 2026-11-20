package com.sportapp.tiendasport.controller;

import com.sportapp.tiendasport.dto.MensajeResponse;
import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.Producto;
import com.sportapp.tiendasport.repository.ProductoRepository;
import com.sportapp.tiendasport.service.ActividadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/producto")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoRepository productoRepository;
    private final ActividadService actividadService;

    // Pública: el catálogo lo puede ver cualquier invitado
    @GetMapping
    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    // Administrador o Vendedor
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody Producto producto, @AuthenticationPrincipal Cliente actor) {
        producto.setIdProducto(null);
        if (producto.getImagenUrl() == null || producto.getImagenUrl().isBlank()) {
            producto.setImagenUrl("/imagenes/productos/default.png");
        }
        productoRepository.save(producto);
        actividadService.registrar(actor, "PRODUCTO_CREADO", "producto", producto.getIdProducto(),
                "Producto \"" + producto.getNombre() + "\", precio $" + producto.getPrecioMayorista() + ", stock " + producto.getStock());
        return ResponseEntity.status(HttpStatus.CREATED).body(MensajeResponse.ok("Producto guardado exitosamente"));
    }

    // Administrador o Vendedor
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @Valid @RequestBody Producto datos,
                                         @AuthenticationPrincipal Cliente actor) {
        return productoRepository.findById(id).map(producto -> {
            String antes = "precio $" + producto.getPrecioMayorista() + ", costo $" + producto.getCostoProducto() + ", stock " + producto.getStock();
            producto.setNombre(datos.getNombre());
            producto.setDescripcion(datos.getDescripcion());
            producto.setTalla(datos.getTalla());
            producto.setColor(datos.getColor());
            producto.setGenero(datos.getGenero() != null ? datos.getGenero() : "Unisex");
            producto.setPrecioMayorista(datos.getPrecioMayorista());
            producto.setCostoProducto(datos.getCostoProducto());
            producto.setStock(datos.getStock());
            producto.setImagenUrl(datos.getImagenUrl());
            producto.setImagenesPorColor(datos.getImagenesPorColor());
            producto.setIdCategoria(datos.getIdCategoria());
            productoRepository.save(producto);
            actividadService.registrar(actor, "PRODUCTO_EDITADO", "producto", id,
                    "Producto \"" + producto.getNombre() + "\". Antes: " + antes + ". Ahora: precio $"
                            + producto.getPrecioMayorista() + ", costo $" + producto.getCostoProducto() + ", stock " + producto.getStock());
            return ResponseEntity.ok(MensajeResponse.ok("Producto actualizado correctamente"));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(MensajeResponse.error("No se encontró el producto")));
    }

    // Solo Administrador
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id, @AuthenticationPrincipal Cliente actor) {
        Producto existente = productoRepository.findById(id).orElse(null);
        if (existente == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(MensajeResponse.error("No existe ese producto"));
        }
        actividadService.registrar(actor, "PRODUCTO_ELIMINADO", "producto", id,
                "Producto eliminado: \"" + existente.getNombre() + "\" (" + existente.getTalla() + ", " + existente.getColor()
                        + "), precio $" + existente.getPrecioMayorista() + ", stock " + existente.getStock());
        productoRepository.deleteById(id);
        return ResponseEntity.ok(MensajeResponse.ok("Producto eliminado"));
    }
}
