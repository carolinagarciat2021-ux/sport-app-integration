package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.Subcategoria;
import com.sportapp.tiendasport.repository.CategoriaRepository;
import com.sportapp.tiendasport.repository.ProductoRepository;
import com.sportapp.tiendasport.repository.SubcategoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubcategoriaServiceTest {

    @Mock private SubcategoriaRepository subcategoriaRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ActividadService actividadService;
    @InjectMocks private SubcategoriaService servicio;

    @Test
    void creaUnTipoValidoYLoRegistraEnElHistorial() {
        when(categoriaRepository.existsById(3)).thenReturn(true);
        when(subcategoriaRepository.save(any(Subcategoria.class))).thenAnswer(i -> i.getArgument(0));

        Subcategoria s = servicio.crear("  Falda short  ", 3, "https://img/x.jpg", new Cliente());

        assertEquals("Falda short", s.getNombre());
        assertEquals(3, s.getIdCategoria());
        verify(actividadService).registrar(any(), eq("TIPO_CREADO"), eq("subcategoria"), any(), anyString());
    }

    @Test
    void rechazaNombreVacioYCategoriaInexistenteYRepetidos() {
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(" ", 3, null, new Cliente()));

        when(categoriaRepository.existsById(99)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> servicio.crear("Falda", 99, null, new Cliente()));

        when(categoriaRepository.existsById(3)).thenReturn(true);
        when(subcategoriaRepository.existsByIdCategoriaAndNombreIgnoreCase(3, "Falda")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> servicio.crear("Falda", 3, null, new Cliente()));

        verify(subcategoriaRepository, never()).save(any(Subcategoria.class));
    }

    @Test
    void alEliminarUnTipoLosProductosQuedanSinTipoPeroNoSeBorran() {
        Subcategoria s = new Subcategoria();
        s.setIdSubcategoria(7);
        s.setNombre("Falda larga");
        when(subcategoriaRepository.findById(7)).thenReturn(Optional.of(s));

        servicio.eliminar(7, new Cliente());

        verify(productoRepository).limpiarSubcategoria(7);
        verify(subcategoriaRepository).delete(s);
        verify(productoRepository, never()).deleteById(any());
    }

    @Test
    void asignarProductosReemplazaLaListaCompleta() {
        Subcategoria s = new Subcategoria();
        s.setIdSubcategoria(7);
        s.setIdCategoria(3);
        s.setNombre("Falda larga");
        when(subcategoriaRepository.findById(7)).thenReturn(Optional.of(s));
        when(productoRepository.asignarSubcategoria(7, List.of(1, 2), 3)).thenReturn(2);

        int asignados = servicio.asignarProductos(7, List.of(1, 2), new Cliente());

        assertEquals(2, asignados);
        verify(productoRepository).limpiarSubcategoria(7);
    }
}
