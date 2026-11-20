package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.EscalaDescuento;
import com.sportapp.tiendasport.repository.EscalaDescuentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DescuentoServiceTest {

    @Mock private EscalaDescuentoRepository repositorio;
    @Mock private ActividadService actividadService;
    @InjectMocks private DescuentoService servicio;

    private EscalaDescuento e(String monto, String pct) {
        return new EscalaDescuento(null, new BigDecimal(monto), new BigDecimal(pct));
    }

    @Test
    void guardaUnaEscalaValidaYQuedaEnElHistorial() {
        when(repositorio.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        servicio.reemplazar(List.of(e("500000", "10"), e("200000", "5")), new Cliente());
        verify(repositorio).deleteAllInBatch();
        verify(actividadService).registrar(any(), eq("DESCUENTOS_ACTUALIZADOS"), eq("descuento"), any(), anyString());
    }

    @Test
    void rechazaMontosRepetidos() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.reemplazar(List.of(e("200000", "5"), e("200000", "8")), new Cliente()));
        verify(repositorio, never()).deleteAllInBatch();
    }

    @Test
    void rechazaQueAMayorMontoHayaMenorOIgualDescuento() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.reemplazar(List.of(e("200000", "10"), e("500000", "5")), new Cliente()));
    }

    @Test
    void rechazaPorcentajesYMontosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> servicio.reemplazar(List.of(e("200000", "0")), new Cliente()));
        assertThrows(IllegalArgumentException.class, () -> servicio.reemplazar(List.of(e("200000", "95")), new Cliente()));
        assertThrows(IllegalArgumentException.class, () -> servicio.reemplazar(List.of(e("-5", "10")), new Cliente()));
    }
}
