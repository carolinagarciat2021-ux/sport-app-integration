package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.dto.PedidoCorreo;
import com.sportapp.tiendasport.model.DatosEmpresa;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlantillasCorreoTest {

    private DatosEmpresa empresa() {
        DatosEmpresa e = new DatosEmpresa();
        e.setNombre("Tienda Sport");
        e.setLema("Ropa deportiva para toda la familia");
        e.setWhatsapp("573127622295");
        e.setCorreo("tiendasport@gmail.com");
        return e;
    }

    private PedidoCorreo pedido(String estado, boolean nueva, String nombre, String transportadora, String guia) {
        return new PedidoCorreo(12, estado, nueva, nombre, "lina@gmail.com", "3001234567",
                List.of(new PedidoCorreo.Linea("Camiseta Pro", "M", "Negro", 2, new BigDecimal("250000"))),
                new BigDecimal("500000"), new BigDecimal("8"), new BigDecimal("40000"), new BigDecimal("460000"),
                "Calle 1 # 2-3", transportadora, guia);
    }

    @Test
    void elCorreoDeEnvioIncluyeEmpresaDeRepartoYGuia() {
        String html = PlantillasCorreo.htmlCliente(pedido("Enviado", false, "Lina", "Servientrega", "G-123"), empresa());
        assertTrue(html.contains("va en camino"));
        assertTrue(html.contains("Servientrega"));
        assertTrue(html.contains("G-123"));
        assertTrue(html.contains("$460.000"));
        assertTrue(html.contains("Descuento mayorista 8%"));
        assertEquals("Tu pedido #12 va en camino – Tienda Sport",
                PlantillasCorreo.asuntoCliente(pedido("Enviado", false, "Lina", null, null), "Tienda Sport"));
    }

    @Test
    void laConfirmacionDeCompraNoMuestraDatosDeEnvio() {
        String html = PlantillasCorreo.htmlCliente(pedido("Confirmado", true, "Lina", null, null), empresa());
        assertTrue(html.contains("Gracias por tu compra"));
        assertFalse(html.contains("Datos del envío"));
    }

    @Test
    void loQueEscribeUnaPersonaNoPuedeMeterHtml() {
        String html = PlantillasCorreo.htmlCliente(pedido("Confirmado", true, "<script>alert(1)</script>", null, null), empresa());
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void elAvisoAlAdministradorTraeNombreCorreoYTelefonoDelCliente() {
        PedidoCorreo p = pedido("Confirmado", true, "Lina Tabares", null, null);
        String html = PlantillasCorreo.htmlAdmin(p, empresa());
        assertTrue(html.contains("Lina Tabares"));
        assertTrue(html.contains("lina@gmail.com"));
        assertTrue(html.contains("3001234567"));
        assertTrue(PlantillasCorreo.asuntoAdmin(p, "Tienda Sport").contains("Nuevo pedido #12"));
    }
}
