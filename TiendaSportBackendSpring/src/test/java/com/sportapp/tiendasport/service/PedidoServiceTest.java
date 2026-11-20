package com.sportapp.tiendasport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportapp.tiendasport.dto.PedidoDtos.DetalleRequest;
import com.sportapp.tiendasport.dto.PedidoDtos.ItemCompra;
import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.DetallePedido;
import com.sportapp.tiendasport.model.Pedido;
import com.sportapp.tiendasport.model.Producto;
import com.sportapp.tiendasport.repository.ClienteRepository;
import com.sportapp.tiendasport.repository.ConfiguracionRepository;
import com.sportapp.tiendasport.repository.DetallePedidoRepository;
import com.sportapp.tiendasport.repository.EscalaDescuentoRepository;
import com.sportapp.tiendasport.model.EscalaDescuento;
import com.sportapp.tiendasport.repository.PedidoEstadoHistorialRepository;
import com.sportapp.tiendasport.repository.PedidoRepository;
import com.sportapp.tiendasport.dto.PedidoCorreo;
import org.springframework.context.ApplicationEventPublisher;
import com.sportapp.tiendasport.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas de la compra (total en servidor, stock, monto mínimo) y del cambio de estado/anulación. */
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private DetallePedidoRepository detallePedidoRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private PedidoEstadoHistorialRepository historialRepository;
    @Mock private ApplicationEventPublisher eventos;
    @Mock private ClienteRepository clienteRepository;
    @Mock private EscalaDescuentoRepository escalaDescuentoRepository; // sin stub => lista vacía => sin descuento
    @Mock private ActividadService actividadService;
    @Mock private ConfiguracionRepository configuracionRepository; // sin stub => Optional.empty => mínimo $200.000

    @InjectMocks private PedidoService pedidoService;

    private Cliente cliente(int id, int rol) {
        Cliente c = new Cliente();
        c.setIdClientes(id);
        c.setIdRol(rol);
        c.setDireccion("Calle 1 # 2-3, Tuluá");
        return c;
    }

    private Producto producto(int stock, String precio) {
        Producto p = new Producto();
        p.setIdProducto(1);
        p.setNombre("Camiseta");
        p.setStock(stock);
        p.setPrecioMayorista(new BigDecimal(precio));
        return p;
    }

    private ItemCompra item(int cantidad) {
        return new ItemCompra(1, "M", "Negro", cantidad);
    }

    private Pedido pedido(int id, int idCliente, String estado) {
        Pedido p = new Pedido();
        p.setIdPedido(id);
        p.setIdCliente(idCliente);
        p.setEstado(estado);
        return p;
    }

    // ---------------- Compra completa ----------------

    @Test
    void elTotalSeCalculaEnElServidorYSeDescuentaElStock() {
        Producto producto = producto(10, "100000");
        when(productoRepository.findWithLockByIdProducto(1)).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        Pedido pedido = pedidoService.crearPedidoCompleto(cliente(7, 2), List.of(item(3)));

        assertEquals(0, new BigDecimal("300000").compareTo(pedido.getTotal()));
        assertEquals("Confirmado", pedido.getEstado());
        assertEquals(7, pedido.getIdCliente());
        assertEquals("Calle 1 # 2-3, Tuluá", pedido.getDireccionEntrega());
        assertEquals(7, producto.getStock());
        verify(detallePedidoRepository).save(any(DetallePedido.class));
        verify(actividadService).registrar(any(Cliente.class), eq("PEDIDO_CREADO"), eq("pedido"), any(), anyString());
    }

    @Test
    void laComprasQueAlcanzaUnEscalonRecibeElDescuentoCalculadoEnElServidor() {
        // 2 x $250.000 = $500.000 -> escalón de 10 % -> descuento $50.000 -> total $450.000
        Producto producto = producto(10, "250000");
        when(productoRepository.findWithLockByIdProducto(1)).thenReturn(Optional.of(producto));
        when(escalaDescuentoRepository.findAllByOrderByMontoMinimoAsc()).thenReturn(List.of(
                new EscalaDescuento(1, new BigDecimal("200000"), new BigDecimal("5")),
                new EscalaDescuento(2, new BigDecimal("500000"), new BigDecimal("10")),
                new EscalaDescuento(3, new BigDecimal("1000000"), new BigDecimal("15"))));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        Pedido pedido = pedidoService.crearPedidoCompleto(cliente(7, 2), List.of(item(2)));

        assertEquals(0, new BigDecimal("500000").compareTo(pedido.getSubtotal()));
        assertEquals(0, new BigDecimal("10").compareTo(pedido.getDescuentoPorcentaje()));
        assertEquals(0, new BigDecimal("50000").compareTo(pedido.getDescuentoValor()));
        assertEquals(0, new BigDecimal("450000").compareTo(pedido.getTotal()));
    }

    @Test
    void sinAlcanzarElMinimoDeCompraNoSeConsideraNingunDescuento() {
        Producto producto = producto(10, "1000");
        when(productoRepository.findWithLockByIdProducto(1)).thenReturn(Optional.of(producto));
        assertThrows(IllegalArgumentException.class, () -> pedidoService.crearPedidoCompleto(cliente(7, 2), List.of(item(1))));
        verify(escalaDescuentoRepository, never()).findAllByOrderByMontoMinimoAsc();
    }

    @Test
    void unPrecioManipuladoEnElJsonSeIgnora() throws Exception {
        // El cliente intenta mandar precio_unitario = 1: el DTO no tiene ese campo.
        String json = "[{\"id_producto\":1,\"talla\":\"M\",\"color\":\"Negro\",\"cantidad\":3,\"precio_unitario\":1}]";
        List<ItemCompra> items = new ObjectMapper().readerForListOf(ItemCompra.class).readValue(json);

        Producto producto = producto(10, "100000");
        when(productoRepository.findWithLockByIdProducto(1)).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        Pedido pedido = pedidoService.crearPedidoCompleto(cliente(7, 2), items);

        assertEquals(0, new BigDecimal("300000").compareTo(pedido.getTotal()));
    }

    @Test
    void conStockInsuficienteLanzaErrorYNoGuardaNada() {
        Producto producto = producto(2, "100000");
        when(productoRepository.findWithLockByIdProducto(1)).thenReturn(Optional.of(producto));

        assertThrows(IllegalStateException.class, () -> pedidoService.crearPedidoCompleto(cliente(7, 2), List.of(item(5))));

        assertEquals(2, producto.getStock());
        verify(productoRepository, never()).save(any(Producto.class));
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void porDebajoDelMontoMinimoSeRechazaYNoDescuentaStock() {
        Producto producto = producto(10, "1000");
        when(productoRepository.findWithLockByIdProducto(1)).thenReturn(Optional.of(producto));

        assertThrows(IllegalArgumentException.class, () -> pedidoService.crearPedidoCompleto(cliente(7, 2), List.of(item(1))));

        assertEquals(10, producto.getStock());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void conProductoInexistenteLanzaError() {
        when(productoRepository.findWithLockByIdProducto(1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> pedidoService.crearPedidoCompleto(cliente(7, 2), List.of(item(1))));
    }

    @Test
    void conCarritoVacioOCantidadInvalidaLanzaError() {
        assertThrows(IllegalArgumentException.class, () -> pedidoService.crearPedidoCompleto(cliente(7, 2), List.of()));
        assertThrows(IllegalArgumentException.class, () -> pedidoService.crearPedidoCompleto(cliente(7, 2), List.of(item(0))));
    }

    // ---------------- Cambio de estado y anulación ----------------

    @Test
    void unClienteNoPuedeCambiarElEstado() {
        assertThrows(AccessDeniedException.class, () -> pedidoService.cambiarEstado(5, "Enviado", cliente(7, 2)));
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void unEstadoInvalidoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> pedidoService.cambiarEstado(5, "Borrado", cliente(1, 1)));
    }

    @Test
    void elVendedorPuedeEnviarPeroNoAnular() {
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido(5, 7, "Confirmado")));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        Pedido enviado = pedidoService.cambiarEstado(5, "Enviado", cliente(3, 3));
        assertEquals("Enviado", enviado.getEstado());

        assertThrows(AccessDeniedException.class, () -> pedidoService.cambiarEstado(5, "Cancelado", cliente(3, 3)));
    }

    @Test
    void alAnularElAdministradorSeDevuelveElStock() {
        Producto producto = producto(7, "100000");
        DetallePedido detalle = new DetallePedido();
        detalle.setIdProducto(1);
        detalle.setCantidad(3);

        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido(5, 7, "Confirmado")));
        when(detallePedidoRepository.findByIdPedido(5)).thenReturn(List.of(detalle));
        when(productoRepository.findWithLockByIdProducto(1)).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        Pedido anulado = pedidoService.cambiarEstado(5, "Cancelado", cliente(1, 1));

        assertEquals("Cancelado", anulado.getEstado());
        assertEquals(10, producto.getStock());
        verify(productoRepository).save(producto);
        verify(actividadService).registrar(any(Cliente.class), eq("PEDIDO_ANULADO"), eq("pedido"), eq(5), anyString());
    }

    @Test
    void unPedidoAnuladoNoCambiaMasYNoDevuelveStockDosVeces() {
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido(5, 7, "Cancelado")));

        assertThrows(IllegalStateException.class, () -> pedidoService.cambiarEstado(5, "Cancelado", cliente(1, 1)));

        verify(productoRepository, never()).save(any(Producto.class));
    }

    // ---------------- Línea de tiempo y correos ----------------

    @Test
    void laCompraQuedaEnLaLineaDeTiempoYAvisaPorCorreoAlClienteYAlAdministrador() {
        Producto producto = producto(10, "100000");
        when(productoRepository.findWithLockByIdProducto(1)).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        Cliente comprador = cliente(7, 2);
        comprador.setCorreo("lina@gmail.com");
        pedidoService.crearPedidoCompleto(comprador, List.of(item(3)));

        verify(historialRepository).save(any());
        org.mockito.ArgumentCaptor<PedidoCorreo> evento = org.mockito.ArgumentCaptor.forClass(PedidoCorreo.class);
        verify(eventos).publishEvent(evento.capture());
        assertTrue(evento.getValue().compraNueva());
        assertEquals("lina@gmail.com", evento.getValue().correoCliente());
        assertEquals("Camiseta", evento.getValue().productos().get(0).nombre());
    }

    @Test
    void alMarcarEnviadoSeGuardanTransportadoraYGuiaYSeAvisaAlCliente() {
        Pedido pedido = pedido(5, 7, "Confirmado");
        Cliente comprador = cliente(7, 2);
        comprador.setCorreo("lina@gmail.com");
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
        when(clienteRepository.findById(7)).thenReturn(Optional.of(comprador));

        Pedido enviado = pedidoService.cambiarEstado(5, "Enviado", " Servientrega ", "G-123", cliente(3, 3));

        assertEquals("Enviado", enviado.getEstado());
        assertEquals("Servientrega", enviado.getTransportadora());
        assertEquals("G-123", enviado.getNumeroGuia());
        verify(historialRepository).save(any());
        org.mockito.ArgumentCaptor<PedidoCorreo> evento = org.mockito.ArgumentCaptor.forClass(PedidoCorreo.class);
        verify(eventos).publishEvent(evento.capture());
        assertEquals("Enviado", evento.getValue().estado());
        assertEquals("G-123", evento.getValue().numeroGuia());
    }

    @Test
    void sinCambioRealDeEstadoNoSeEnviaCorreoNiSeRegistraOtroHito() {
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido(5, 7, "Enviado")));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        pedidoService.cambiarEstado(5, "Enviado", cliente(3, 3));

        verify(historialRepository, never()).save(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void unTextoDeGuiaDemasiadoLargoSeRechaza() {
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido(5, 7, "Confirmado")));
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.cambiarEstado(5, "Enviado", "X".repeat(200), null, cliente(3, 3)));
    }

    // ---------------- Eliminación (solo Administrador) ----------------

    @Test
    void soloElAdministradorPuedeEliminarPedidos() {
        assertThrows(AccessDeniedException.class, () -> pedidoService.eliminarPedido(5, cliente(7, 2)));
        assertThrows(AccessDeniedException.class, () -> pedidoService.eliminarPedido(5, cliente(3, 3)));
        verify(pedidoRepository, never()).delete(any(Pedido.class));
    }

    @Test
    void alEliminarUnPedidoActivoSeDevuelveElStockYSeBorraTodo() {
        Producto producto = producto(7, "100000");
        DetallePedido detalle = new DetallePedido();
        detalle.setIdProducto(1);
        detalle.setCantidad(3);
        Pedido pedido = pedido(5, 7, "Confirmado");

        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido));
        when(detallePedidoRepository.findByIdPedido(5)).thenReturn(List.of(detalle));
        when(productoRepository.findWithLockByIdProducto(1)).thenReturn(Optional.of(producto));

        pedidoService.eliminarPedido(5, cliente(1, 1));

        assertEquals(10, producto.getStock());
        verify(detallePedidoRepository).deleteAll(List.of(detalle));
        verify(pedidoRepository).delete(pedido);
        // Queda en el historial quién lo eliminó y cómo era el pedido
        verify(actividadService).registrar(any(Cliente.class), eq("PEDIDO_ELIMINADO"), eq("pedido"), eq(5), anyString());
    }

    @Test
    void alEliminarUnPedidoYaAnuladoNoSeDevuelveStockOtraVez() {
        Pedido pedido = pedido(5, 7, "Cancelado");
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido));
        when(detallePedidoRepository.findByIdPedido(5)).thenReturn(List.of());

        pedidoService.eliminarPedido(5, cliente(1, 1));

        verify(productoRepository, never()).save(any(Producto.class));
        verify(pedidoRepository).delete(pedido);
    }

    // ---------------- Flujo antiguo (mientras se migra) ----------------

    private DetalleRequest detalle(int cantidad) {
        DetalleRequest r = new DetalleRequest();
        r.setId_pedido(10);
        r.setId_producto(1);
        r.setTalla("M");
        r.setColor("Negro");
        r.setCantidad(cantidad);
        r.setPrecio_unitario(new BigDecimal("1")); // precio falso: debe ignorarse
        return r;
    }

    @Test
    void agregarDetalleUsaElPrecioDelServidor() {
        Producto producto = producto(10, "45000");
        when(pedidoRepository.findById(10)).thenReturn(Optional.of(pedido(10, 7, "Pendiente")));
        when(productoRepository.findWithLockByIdProducto(1)).thenReturn(Optional.of(producto));
        when(detallePedidoRepository.save(any(DetallePedido.class))).thenAnswer(i -> i.getArgument(0));

        DetallePedido d = pedidoService.agregarDetalle(detalle(3), cliente(7, 2));

        assertEquals(0, new BigDecimal("45000").compareTo(d.getPrecioUnitario()));
        assertEquals(7, producto.getStock());
    }

    @Test
    void unClienteNoPuedeAgregarDetallesAUnPedidoAjeno() {
        when(pedidoRepository.findById(10)).thenReturn(Optional.of(pedido(10, 99, "Pendiente")));
        assertThrows(AccessDeniedException.class, () -> pedidoService.agregarDetalle(detalle(1), cliente(7, 2)));
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    void unPedidoConfirmadoNoAceptaMasProductos() {
        when(pedidoRepository.findById(10)).thenReturn(Optional.of(pedido(10, 7, "Confirmado")));
        assertThrows(IllegalStateException.class, () -> pedidoService.agregarDetalle(detalle(1), cliente(7, 2)));
        verify(detallePedidoRepository, never()).save(any(DetallePedido.class));
    }
}
