package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.Subcategoria;
import com.sportapp.tiendasport.repository.CategoriaRepository;
import com.sportapp.tiendasport.repository.ProductoRepository;
import com.sportapp.tiendasport.repository.SubcategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class SubcategoriaService {

    private final SubcategoriaRepository subcategoriaRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final ActividadService actividadService;

    public List<Subcategoria> listar() {
        return subcategoriaRepository.findAllByOrderByIdCategoriaAscNombreAsc();
    }

    @Transactional
    public Subcategoria crear(String nombre, Integer idCategoria, String imagenUrl, Cliente actor) {
        String limpio = validarNombre(nombre);
        if (idCategoria == null || !categoriaRepository.existsById(idCategoria)) {
            throw new IllegalArgumentException("La categoría seleccionada no existe.");
        }
        if (subcategoriaRepository.existsByIdCategoriaAndNombreIgnoreCase(idCategoria, limpio)) {
            throw new IllegalArgumentException("Ya existe un tipo con ese nombre en esta categoría.");
        }
        Subcategoria s = new Subcategoria();
        s.setNombre(limpio);
        s.setIdCategoria(idCategoria);
        s.setImagenUrl(limpiarUrl(imagenUrl));
        Subcategoria guardada = subcategoriaRepository.save(s);
        actividadService.registrar(actor, "TIPO_CREADO", "subcategoria", guardada.getIdSubcategoria(),
                "Tipo \"" + limpio + "\" en la categoría " + idCategoria);
        return guardada;
    }

    @Transactional
    public Subcategoria actualizar(Integer id, String nombre, String imagenUrl, Cliente actor) {
        Subcategoria s = subcategoriaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el tipo."));
        String limpio = validarNombre(nombre);
        if (subcategoriaRepository.existsByIdCategoriaAndNombreIgnoreCaseAndIdSubcategoriaNot(s.getIdCategoria(), limpio, id)) {
            throw new IllegalArgumentException("Ya existe un tipo con ese nombre en esta categoría.");
        }
        String antes = s.getNombre();
        s.setNombre(limpio);
        s.setImagenUrl(limpiarUrl(imagenUrl));
        Subcategoria guardada = subcategoriaRepository.save(s);
        actividadService.registrar(actor, "TIPO_EDITADO", "subcategoria", id, "Tipo \"" + antes + "\" -> \"" + limpio + "\"");
        return guardada;
    }

    /** Elimina el tipo; los productos NO se borran, solo quedan sin tipo. */
    @Transactional
    public void eliminar(Integer id, Cliente actor) {
        Subcategoria s = subcategoriaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el tipo."));
        productoRepository.limpiarSubcategoria(id);
        subcategoriaRepository.delete(s);
        actividadService.registrar(actor, "TIPO_ELIMINADO", "subcategoria", id, "Tipo eliminado: \"" + s.getNombre() + "\"");
    }

    /** Deja asignados al tipo exactamente los productos indicados (solo los de la misma categoría). */
    @Transactional
    public int asignarProductos(Integer id, List<Integer> idsProductos, Cliente actor) {
        Subcategoria s = subcategoriaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el tipo."));
        productoRepository.limpiarSubcategoria(id);
        int asignados = 0;
        if (idsProductos != null && !idsProductos.isEmpty()) {
            asignados = productoRepository.asignarSubcategoria(id, idsProductos, s.getIdCategoria());
        }
        actividadService.registrar(actor, "TIPO_PRODUCTOS", "subcategoria", id,
                "Tipo \"" + s.getNombre() + "\": " + asignados + " producto(s) asignado(s)");
        return asignados;
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del tipo es obligatorio.");
        }
        String limpio = nombre.trim();
        if (limpio.length() > 80) {
            throw new IllegalArgumentException("El nombre del tipo no puede pasar de 80 caracteres.");
        }
        return limpio;
    }

    private static String limpiarUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String limpia = url.trim();
        if (limpia.length() > 500) {
            throw new IllegalArgumentException("La dirección de la imagen es demasiado larga (máximo 500 caracteres).");
        }
        return limpia;
    }
}
