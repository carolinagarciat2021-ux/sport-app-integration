package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.dto.PedidoConDetalle;
import com.sportapp.tiendasport.dto.PedidoListado;
import com.sportapp.tiendasport.model.DetallePedido;
import com.sportapp.tiendasport.model.Producto;
import com.sportapp.tiendasport.repository.DetallePedidoRepository;
import com.sportapp.tiendasport.repository.PedidoEstadoHistorialRepository;
import com.sportapp.tiendasport.repository.PedidoRepository;
import com.sportapp.tiendasport.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoConsultaServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private DetallePedidoRepository detallePedidoRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private PedidoEstadoHistorialRepository historialRepository;
    @InjectMocks private PedidoConsultaService servicio;

    private PedidoListado fila() {
        return new PedidoListado(26, LocalDateTime.of(2026, 10, 8, 18, 21), "Confirmado", new BigDecimal("350000"),
                new BigDecimal("5"), new BigDecimal("17500"), new BigDecimal("332500"), "Calle 1",
                21, "Lina", "Tabares", "lina_tabares@gmail.com", "3001234567", null, null);
    }

    @Test
    void devuelveQuienCompraConNombreYCorreoYSusProductosEnUnaSolaRespuesta() {
        when(pedidoRepository.buscarListado(any(), any(), any(), any(), anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fila()), PageRequest.of(0, 20), 1));

        DetallePedido d = new DetallePedido();
        d.setIdPedido(26);
        d.setIdProducto(5);
        d.setTalla("M");
        d.setColor("Negro");
        d.setCantidad(2);
        d.setPrecioUnitario(new BigDecimal("175000"));
        when(detallePedidoRepository.findByIdPedidoIn(List.of(26))).thenReturn(List.of(d));

        Producto prod = new Producto();
        prod.setIdProducto(5);
        prod.setNombre("Camiseta Pro");
        when(productoRepository.findAllById(any())).thenReturn(List.of(prod));

        PedidoConDetalle.Pagina pagina = servicio.buscar("lina", null, null, null, null, null, 0, 20);

        assertEquals(1, pagina.totalRegistros());
        PedidoConDetalle p = pagina.pedidos().get(0);
        assertEquals("Lina", p.nombreCliente());
        assertEquals("lina_tabares@gmail.com", p.correoCliente());
        assertEquals("Camiseta Pro", p.productos().get(0).nombreProducto());
        assertEquals(0, new BigDecimal("350000").compareTo(p.productos().get(0).subtotal()));
    }

    @Test
    void elTextoSeBuscaEnMinusculasSinComodinesYElTamanoSeLimita() {
        when(pedidoRepository.buscarListado(any(), any(), any(), any(), anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        servicio.buscar("  LiNa% ", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null, null, -3, 5000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(pedidoRepository).buscarListado(any(), hasta.capture(), any(), any(), eq(""), eq("lina"), eq("%lina%"), pageable.capture());
        assertEquals(100, pageable.getValue().getPageSize());
        assertEquals(0, pageable.getValue().getPageNumber());
        // el día 31 entra completo: el límite es el 1 de noviembre a las 00:00
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0), hasta.getValue());
    }

    @Test
    void rechazaUnEstadoInventado() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.buscar(null, null, null, null, null, "Borrado", 0, 20));
    }
}
