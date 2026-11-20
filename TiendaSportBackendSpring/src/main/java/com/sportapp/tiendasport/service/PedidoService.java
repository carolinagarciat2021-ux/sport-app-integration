package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.dto.PedidoCorreo;
import com.sportapp.tiendasport.dto.PedidoDtos.DetalleRequest;
import com.sportapp.tiendasport.dto.PedidoDtos.ItemCompra;
import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.DetallePedido;
import com.sportapp.tiendasport.model.EstadoPedido;
import com.sportapp.tiendasport.model.Pedido;
import com.sportapp.tiendasport.model.PedidoEstadoHistorial;
import com.sportapp.tiendasport.model.Producto;
import com.sportapp.tiendasport.repository.ClienteRepository;
import com.sportapp.tiendasport.repository.ConfiguracionRepository;
import com.sportapp.tiendasport.repository.DetallePedidoRepository;
import com.sportapp.tiendasport.repository.EscalaDescuentoRepository;
import com.sportapp.tiendasport.repository.PedidoEstadoHistorialRepository;
import com.sportapp.tiendasport.repository.PedidoRepository;
import com.sportapp.tiendasport.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final int ROL_ADMINISTRADOR = 1;
    private static final int ROL_VENDEDOR = 3;

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final ClienteRepository clienteRepository;
    private final EscalaDescuentoRepository escalaDescuentoRepository;
    private final ActividadService actividadService;
    private final PedidoEstadoHistorialRepository historialRepository;
    private final ApplicationEventPublisher eventos;

    // ------------------------------------------------------------------
    // Compra (checkout): UNA sola transacción
    // ------------------------------------------------------------------

    /**
     * Confirma la compra. Todo ocurre en una sola transacción: si algo falla
     * (stock, monto mínimo, producto inexistente) no queda nada a medias.
     *
     * - El precio y el total se calculan AQUÍ con los precios de la tabla producto.
     * - El pedido nace "Confirmado" (= pagado) y desde ahí ya no se borra ni se
     *   le cambian los productos.
     * - La dirección de entrega se copia del cliente en este momento.
     */
    @Transactional
    public Pedido crearPedidoCompleto(Cliente cliente, List<ItemCompra> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío.");
        }

        // 1) Validar y agrupar cantidades por producto (TreeMap = orden por id,
        //    así dos compras simultáneas bloquean en el mismo orden y no se traban).
        Map<Integer, Integer> cantidadPorProducto = new TreeMap<>();
        for (ItemCompra item : items) {
            if (item == null || item.getId_producto() == null) {
                throw new IllegalArgumentException("Cada producto debe indicar su id_producto.");
            }
            if (item.getCantidad() == null || item.getCantidad() < 1) {
                throw new IllegalArgumentException("La cantidad de cada producto debe ser mínimo 1.");
            }
            cantidadPorProducto.merge(item.getId_producto(), item.getCantidad(), Integer::sum);
        }

        // 2) Bloquear cada producto y validar existencia y stock
        Map<Integer, Producto> productos = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : cantidadPorProducto.entrySet()) {
            Producto producto = productoRepository.findWithLockByIdProducto(e.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("El producto ID " + e.getKey() + " no existe."));
            if (producto.getStock() < e.getValue()) {
                throw new IllegalStateException("Stock insuficiente para \"" + producto.getNombre() + "\" (ID "
                        + e.getKey() + "): disponible " + producto.getStock() + ", solicitado " + e.getValue() + ".");
            }
            productos.put(e.getKey(), producto);
        }

        // 3) Subtotal calculado en el servidor con el precio de la base de datos
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCompra item : items) {
            BigDecimal precio = productos.get(item.getId_producto()).getPrecioMayorista();
            subtotal = subtotal.add(precio.multiply(BigDecimal.valueOf(item.getCantidad())));
        }

        // 4) Monto mínimo (configuracion_tienda; si no existe la fila, $200.000)
        BigDecimal montoMinimo = configuracionRepository.findById(1)
                .map(c -> c.getMontoMinimoCompra())
                .orElse(new BigDecimal("200000"));
        if (subtotal.compareTo(montoMinimo) < 0) {
            NumberFormat formato = NumberFormat.getInstance(Locale.forLanguageTag("es-CO"));
            formato.setMaximumFractionDigits(0);
            throw new IllegalArgumentException("El monto mínimo de compra es de $" + formato.format(montoMinimo)
                    + ". Tu pedido actual es de $" + formato.format(subtotal) + ".");
        }

        // 4b) Descuento mayorista: escalón más alto que alcance el subtotal (lo define el Administrador)
        CalculadoraDescuento.Resultado descuento =
                CalculadoraDescuento.calcular(escalaDescuentoRepository.findAllByOrderByMontoMinimoAsc(), subtotal);
        BigDecimal total = subtotal.subtract(descuento.valor());

        // 5) Descontar stock
        for (Map.Entry<Integer, Integer> e : cantidadPorProducto.entrySet()) {
            Producto producto = productos.get(e.getKey());
            producto.setStock(producto.getStock() - e.getValue());
            productoRepository.save(producto);
        }

        // 6) Pedido + detalles
        Pedido pedido = new Pedido();
        pedido.setIdCliente(cliente.getIdClientes());
        pedido.setEstado(EstadoPedido.CONFIRMADO.getEtiqueta());
        pedido.setSubtotal(subtotal);
        pedido.setDescuentoPorcentaje(descuento.porcentaje());
        pedido.setDescuentoValor(descuento.valor());
        pedido.setTotal(total);
        pedido.setDireccionEntrega(cliente.getDireccion());
        pedido = pedidoRepository.save(pedido);

        for (ItemCompra item : items) {
            DetallePedido detalle = new DetallePedido();
            detalle.setIdPedido(pedido.getIdPedido());
            detalle.setIdProducto(item.getId_producto());
            detalle.setTalla(item.getTalla());
            detalle.setColor(item.getColor());
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(productos.get(item.getId_producto()).getPrecioMayorista());
            detallePedidoRepository.save(detalle);
        }
        actividadService.registrar(cliente, "PEDIDO_CREADO", "pedido", pedido.getIdPedido(),
                "Compra confirmada: subtotal $" + subtotal.toPlainString() + ", descuento "
                        + descuento.porcentaje().stripTrailingZeros().toPlainString() + "% (-$" + descuento.valor().toPlainString()
                        + "), total $" + total.toPlainString() + ", " + items.size() + " línea(s)");

        // Línea de tiempo: la compra queda como "Confirmado"
        registrarHito(pedido.getIdPedido(), EstadoPedido.CONFIRMADO.getEtiqueta(), cliente.getCorreo());

        // Avisos por correo (se envían DESPUÉS de guardar el pedido y en segundo plano: si fallan, el pedido no se pierde)
        List<PedidoCorreo.Linea> lineasCorreo = new ArrayList<>();
        for (ItemCompra item : items) {
            Producto p = productos.get(item.getId_producto());
            lineasCorreo.add(new PedidoCorreo.Linea(p.getNombre(), item.getTalla(), item.getColor(), item.getCantidad(), p.getPrecioMayorista()));
        }
        eventos.publishEvent(new PedidoCorreo(pedido.getIdPedido(), pedido.getEstado(), true,
                nombreCompleto(cliente), cliente.getCorreo(), cliente.getTelefono(), lineasCorreo,
                subtotal, descuento.porcentaje(), descuento.valor(), total, pedido.getDireccionEntrega(), null, null));
        return pedido;
    }

    // ------------------------------------------------------------------
    // Cambio de estado / anulación
    // ------------------------------------------------------------------

    /**
     * Solo Administrador y Vendedor cambian el estado. Solo el Administrador
     * puede anular (Cancelado), y al anular se devuelve el stock en la misma
     * transacción. Un pedido Cancelado ya no cambia más (así el stock no se
     * devuelve dos veces).
     */
    @Transactional
    public Pedido cambiarEstado(Integer idPedido, String estadoTexto, Cliente solicitante) {
        return cambiarEstado(idPedido, estadoTexto, null, null, solicitante);
    }

    /**
     * Igual que la anterior, pero al marcar "Enviado" se pueden guardar la empresa de reparto y el número
     * de guía (opcionales). Cada cambio queda en la línea de tiempo y se avisa al cliente por correo.
     */
    @Transactional
    public Pedido cambiarEstado(Integer idPedido, String estadoTexto, String transportadora, String numeroGuia, Cliente solicitante) {
        Integer rol = solicitante.getIdRol();
        if (rol == null || (rol != ROL_ADMINISTRADOR && rol != ROL_VENDEDOR)) {
            throw new AccessDeniedException("Solo el Administrador o el Vendedor pueden cambiar el estado de un pedido.");
        }

        EstadoPedido nuevo = EstadoPedido.desdeTexto(estadoTexto);
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el pedido."));

        if (EstadoPedido.CANCELADO.getEtiqueta().equalsIgnoreCase(pedido.getEstado())) {
            throw new IllegalStateException("El pedido ya fue anulado y no puede cambiar de estado.");
        }

        String estadoAnterior = pedido.getEstado();
        boolean cambio = !nuevo.getEtiqueta().equalsIgnoreCase(estadoAnterior);
        List<DetallePedido> detalles = null;
        if (nuevo == EstadoPedido.CANCELADO) {
            if (rol != ROL_ADMINISTRADOR) {
                throw new AccessDeniedException("Solo el Administrador puede anular un pedido.");
            }
            detalles = detallePedidoRepository.findByIdPedido(idPedido);
            devolverStock(detalles);
        }

        if (nuevo == EstadoPedido.ENVIADO) {
            String empresa = limpiarOpcional(transportadora, "La empresa de reparto", 80);
            String guia = limpiarOpcional(numeroGuia, "El número de guía", 60);
            if (empresa != null) pedido.setTransportadora(empresa);
            if (guia != null) pedido.setNumeroGuia(guia);
        }

        pedido.setEstado(nuevo.getEtiqueta());
        Pedido guardado = pedidoRepository.save(pedido);
        actividadService.registrar(solicitante,
                nuevo == EstadoPedido.CANCELADO ? "PEDIDO_ANULADO" : "PEDIDO_ESTADO", "pedido", idPedido,
                "Estado: " + estadoAnterior + " -> " + nuevo.getEtiqueta()
                        + (nuevo == EstadoPedido.CANCELADO ? " (stock devuelto)" : "")
                        + (nuevo == EstadoPedido.ENVIADO && guardado.getTransportadora() != null
                                ? ". Reparte: " + guardado.getTransportadora()
                                    + (guardado.getNumeroGuia() != null ? ", guía " + guardado.getNumeroGuia() : "") : ""));

        if (cambio) {
            registrarHito(idPedido, nuevo.getEtiqueta(), solicitante.getCorreo());
            // "Pendiente" no genera correo; los demás estados sí (se envía después de guardar y en segundo plano)
            if (nuevo != EstadoPedido.PENDIENTE) {
                if (detalles == null) detalles = detallePedidoRepository.findByIdPedido(idPedido);
                avisarCambioDeEstado(guardado, detalles);
            }
        }
        return guardado;
    }

    private void avisarCambioDeEstado(Pedido pedido, List<DetallePedido> detalles) {
        Cliente comprador = clienteRepository.findById(pedido.getIdCliente()).orElse(null);
        if (comprador == null || comprador.getCorreo() == null || comprador.getCorreo().isBlank()) return;

        Map<Integer, String> nombres = new HashMap<>();
        List<Integer> ids = new ArrayList<>();
        for (DetallePedido d : detalles) ids.add(d.getIdProducto());
        if (!ids.isEmpty()) {
            for (Producto p : productoRepository.findAllById(ids)) nombres.put(p.getIdProducto(), p.getNombre());
        }
        List<PedidoCorreo.Linea> lineas = new ArrayList<>();
        for (DetallePedido d : detalles) {
            lineas.add(new PedidoCorreo.Linea(nombres.getOrDefault(d.getIdProducto(), "Producto #" + d.getIdProducto()),
                    d.getTalla(), d.getColor(), d.getCantidad(), d.getPrecioUnitario()));
        }
        eventos.publishEvent(new PedidoCorreo(pedido.getIdPedido(), pedido.getEstado(), false,
                nombreCompleto(comprador), comprador.getCorreo(), comprador.getTelefono(), lineas,
                pedido.getSubtotal(), pedido.getDescuentoPorcentaje(), pedido.getDescuentoValor(), pedido.getTotal(),
                pedido.getDireccionEntrega(), pedido.getTransportadora(), pedido.getNumeroGuia()));
    }

    private void registrarHito(Integer idPedido, String estado, String porUsuario) {
        PedidoEstadoHistorial h = new PedidoEstadoHistorial();
        h.setIdPedido(idPedido);
        h.setEstado(estado);
        h.setPorUsuario(porUsuario);
        historialRepository.save(h);
    }

    private static String nombreCompleto(Cliente c) {
        String n = ((c.getNombre() == null ? "" : c.getNombre()) + " " + (c.getApellido() == null ? "" : c.getApellido())).trim();
        return n.isEmpty() ? "cliente" : n;
    }

    private static String limpiarOpcional(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) return null;
        String limpio = valor.trim();
        if (limpio.length() > max) {
            throw new IllegalArgumentException(campo + " no puede pasar de " + max + " caracteres.");
        }
        return limpio;
    }

    /**
     * Eliminación definitiva: SOLO el Administrador. En una sola transacción:
     * si el pedido no estaba anulado, primero se devuelve el stock (así el
     * inventario no queda descontado por una venta que ya no existe), y luego
     * se borran sus detalles y el pedido. Esta acción no se puede deshacer.
     */
    @Transactional
    public void eliminarPedido(Integer idPedido, Cliente solicitante) {
        if (solicitante.getIdRol() == null || solicitante.getIdRol() != ROL_ADMINISTRADOR) {
            throw new AccessDeniedException("Solo el Administrador puede eliminar pedidos.");
        }
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el pedido."));

        List<DetallePedido> detalles = detallePedidoRepository.findByIdPedido(idPedido);
        boolean yaAnulado = EstadoPedido.CANCELADO.getEtiqueta().equalsIgnoreCase(pedido.getEstado());

        // Se guarda en el historial cómo era el pedido ANTES de borrarlo (misma transacción).
        actividadService.registrar(solicitante, "PEDIDO_ELIMINADO", "pedido", idPedido,
                describirPedido(pedido, detalles) + (yaAnulado ? " [ya estaba anulado]" : " [stock devuelto]"));

        if (!yaAnulado) {
            devolverStock(detalles);
        }
        detallePedidoRepository.deleteAll(detalles);
        historialRepository.borrarDePedido(idPedido);
        pedidoRepository.delete(pedido);
    }

    private String describirPedido(Pedido p, List<DetallePedido> detalles) {
        String comprador = clienteRepository.findById(p.getIdCliente())
                .map(c -> c.getNombre() + " " + c.getApellido() + " <" + c.getCorreo() + ">")
                .orElse("cliente ID " + p.getIdCliente());
        StringBuilder sb = new StringBuilder("Pedido #").append(p.getIdPedido())
                .append(" de ").append(comprador)
                .append(", fecha ").append(p.getFecha())
                .append(", estado ").append(p.getEstado())
                .append(", total $").append(p.getTotal() == null ? "0" : p.getTotal().toPlainString())
                .append(", entrega: ").append(p.getDireccionEntrega())
                .append(". Productos: ");
        for (DetallePedido d : detalles) {
            sb.append(d.getCantidad()).append("x producto ").append(d.getIdProducto())
              .append(" (").append(d.getTalla()).append(", ").append(d.getColor()).append(") a $")
              .append(d.getPrecioUnitario()).append("; ");
        }
        return sb.toString();
    }

    private void devolverStock(List<DetallePedido> detalles) {
        Map<Integer, Integer> cantidadPorProducto = new TreeMap<>();
        for (DetallePedido d : detalles) {
            cantidadPorProducto.merge(d.getIdProducto(), d.getCantidad(), Integer::sum);
        }
        for (Map.Entry<Integer, Integer> e : cantidadPorProducto.entrySet()) {
            // Si el producto ya no existe no se puede devolver stock, pero la anulación sigue.
            productoRepository.findWithLockByIdProducto(e.getKey()).ifPresent(producto -> {
                producto.setStock(producto.getStock() + e.getValue());
                productoRepository.save(producto);
            });
        }
    }

    // ------------------------------------------------------------------
    // Flujo ANTIGUO (POST /api/pedidos + POST /api/detalle_pedido).
    // Se mantiene solo mientras web y Android migran a /api/pedidos/completo.
    // ------------------------------------------------------------------

    @Deprecated
    public Pedido crearPedido(Integer idCliente, String estado) {
        Pedido pedido = new Pedido();
        pedido.setIdCliente(idCliente);
        pedido.setEstado(estado != null ? EstadoPedido.desdeTexto(estado).getEtiqueta() : EstadoPedido.PENDIENTE.getEtiqueta());
        return pedidoRepository.save(pedido);
    }

    /**
     * Flujo antiguo, ahora con candados: el pedido debe ser del usuario y seguir
     * "Pendiente", y el precio sale de la tabla producto (se ignora el del cliente).
     */
    @Deprecated
    @Transactional
    public DetallePedido agregarDetalle(DetalleRequest request, Cliente cliente) {
        Pedido pedido = pedidoRepository.findById(request.getId_pedido())
                .orElseThrow(() -> new NoSuchElementException("No se encontró el pedido."));
        if (!pedido.getIdCliente().equals(cliente.getIdClientes())) {
            throw new AccessDeniedException("No puedes agregar productos a un pedido que no es tuyo.");
        }
        if (!EstadoPedido.PENDIENTE.getEtiqueta().equalsIgnoreCase(pedido.getEstado())) {
            throw new IllegalStateException("El pedido ya está confirmado y no se puede modificar.");
        }

        Producto producto = productoRepository.findWithLockByIdProducto(request.getId_producto())
                .orElseThrow(() -> new IllegalArgumentException("El producto ID " + request.getId_producto() + " no existe."));

        if (producto.getStock() < request.getCantidad()) {
            throw new IllegalStateException(
                "Stock insuficiente para el producto ID " + request.getId_producto() +
                " (disponible: " + producto.getStock() + ", solicitado: " + request.getCantidad() + ")"
            );
        }

        producto.setStock(producto.getStock() - request.getCantidad());
        productoRepository.save(producto);

        DetallePedido detalle = new DetallePedido();
        detalle.setIdPedido(request.getId_pedido());
        detalle.setIdProducto(request.getId_producto());
        detalle.setTalla(request.getTalla());
        detalle.setColor(request.getColor());
        detalle.setCantidad(request.getCantidad());
        detalle.setPrecioUnitario(producto.getPrecioMayorista()); // precio del servidor, no del cliente

        BigDecimal linea = producto.getPrecioMayorista().multiply(BigDecimal.valueOf(request.getCantidad()));
        pedido.setSubtotal(pedido.getSubtotal().add(linea));
        pedido.setTotal(pedido.getTotal().add(linea));
        pedidoRepository.save(pedido);

        return detallePedidoRepository.save(detalle);
    }
}
