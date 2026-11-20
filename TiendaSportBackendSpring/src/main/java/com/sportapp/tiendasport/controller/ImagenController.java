package com.sportapp.tiendasport.controller;

import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.service.ImagenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Fotos de productos. POST (subir) solo Administrador; GET (ver) público, porque las ven
 * también los visitantes de la tienda. Una imagen nunca cambia (cada subida tiene su propio id),
 * por eso el navegador puede guardarla un año en caché.
 */
@RestController
@RequestMapping("/api/imagenes")
@RequiredArgsConstructor
public class ImagenController {

    private final ImagenService servicio;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subir(@RequestParam("archivo") MultipartFile archivo,
                                   @AuthenticationPrincipal Cliente actor) throws IOException {
        long id = servicio.guardar(archivo.getBytes(), actor);
        // Se devuelve una ruta relativa; la web y la app le anteponen la dirección del servidor.
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", "/api/imagenes/" + id, "id_imagen", id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> ver(@PathVariable long id) {
        return servicio.buscar(id)
                .map(img -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(img.tipo()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .body(img.datos()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
