package com.sportapp.tiendasport.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstadoPedidoTest {

    @Test
    void aceptaLosCincoEstadosSinImportarMayusculas() {
        assertEquals(EstadoPedido.PENDIENTE, EstadoPedido.desdeTexto("Pendiente"));
        assertEquals(EstadoPedido.CONFIRMADO, EstadoPedido.desdeTexto("confirmado"));
        assertEquals(EstadoPedido.ENVIADO, EstadoPedido.desdeTexto(" ENVIADO "));
        assertEquals(EstadoPedido.ENTREGADO, EstadoPedido.desdeTexto("Entregado"));
        assertEquals(EstadoPedido.CANCELADO, EstadoPedido.desdeTexto("Cancelado"));
    }

    @Test
    void rechazaCualquierOtroEstado() {
        assertThrows(IllegalArgumentException.class, () -> EstadoPedido.desdeTexto("Eliminado"));
        assertThrows(IllegalArgumentException.class, () -> EstadoPedido.desdeTexto(null));
        assertThrows(IllegalArgumentException.class, () -> EstadoPedido.desdeTexto(""));
    }
}
