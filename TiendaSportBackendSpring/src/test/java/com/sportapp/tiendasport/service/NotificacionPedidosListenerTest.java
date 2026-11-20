package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.dto.PedidoCorreo;
import com.sportapp.tiendasport.model.DatosEmpresa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionPedidosListenerTest {

    @Mock private CorreoService correoService;
    @Mock private EmpresaService empresaService;
    @InjectMocks private NotificacionPedidosListener listener;

    private PedidoCorreo pedido(boolean nueva) {
        return new PedidoCorreo(12, "Confirmado", nueva, "Lina", "lina@gmail.com", "300",
                List.of(new PedidoCorreo.Linea("Camiseta", "M", "Negro", 1, new BigDecimal("250000"))),
                new BigDecimal("250000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("250000"), "Calle 1", null, null);
    }

    private DatosEmpresa empresa() {
        DatosEmpresa e = new DatosEmpresa();
        e.setNombre("Tienda Sport");
        e.setLema("Lema");
        e.setWhatsapp("573127622295");
        e.setCorreo("tiendasport@gmail.com");
        e.setCorreoAvisos("carolinagarciat2021@gmail.com");
        return e;
    }

    @Test
    void unaCompraNuevaAvisaAlClienteYAlAdministrador() {
        when(correoService.habilitado()).thenReturn(true);
        when(empresaService.obtener()).thenReturn(empresa());

        listener.enviarAvisos(pedido(true));

        ArgumentCaptor<CorreoService.Mensaje> envios = ArgumentCaptor.forClass(CorreoService.Mensaje.class);
        verify(correoService, times(2)).enviar(envios.capture());
        assertEquals("lina@gmail.com", envios.getAllValues().get(0).paraCorreo());
        assertEquals("carolinagarciat2021@gmail.com", envios.getAllValues().get(1).paraCorreo());
    }

    @Test
    void unCambioDeEstadoSoloAvisaAlCliente() {
        when(correoService.habilitado()).thenReturn(true);
        when(empresaService.obtener()).thenReturn(empresa());

        listener.enviarAvisos(pedido(false));

        verify(correoService, times(1)).enviar(any());
    }

    @Test
    void sinCorreoConfiguradoNoHaceNadaYUnFalloNoSePropaga() {
        when(correoService.habilitado()).thenReturn(false);
        listener.enviarAvisos(pedido(true));
        verify(correoService, never()).enviar(any());

        when(correoService.habilitado()).thenReturn(true);
        when(empresaService.obtener()).thenThrow(new RuntimeException("base de datos caída"));
        assertDoesNotThrow(() -> listener.enviarAvisos(pedido(true)));
    }
}
