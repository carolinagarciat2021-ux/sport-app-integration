package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.dto.MiPedido;
import com.sportapp.tiendasport.dto.PedidoConDetalle;
import com.sportapp.tiendasport.dto.PedidoListado;
import com.sportapp.tiendasport.model.DetallePedido;
import com.sportapp.tiendasport.model.EstadoPedido;
import com.sportapp.tiendasport.model.Pedido;
import com.sportapp.tiendasport.model.PedidoEstadoHistorial;
import com.sportapp.tiendasport.model.Producto;
import com.sportapp.tiendasport.repository.DetallePedidoRepository;
import com.sportapp.tiendasport.repository.PedidoEstadoHistorialRepository;
import com.sportapp.tiendasport.repository.PedidoRepository;
import com.sportapp.tiendasport.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Consulta de pedidos para Administrador y Vendedor, con filtros y el comprador ya resuelto. */
@Service
@RequiredArgsConstructor
public class PedidoConsultaService {

    private static final BigDecimal MAX_VALOR = new BigDecimal("999999999999");

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository;
    private final PedidoEstadoHistorialRepository historialRepository;

    /** Los pedidos del propio cliente, con productos y línea de tiempo, en UNA sola respuesta. */
    public List<MiPedido> misPedidos(Integer idCliente) {
        List<Pedido> pedidos = pedidoRepository.findByIdClienteOrderByFechaDescIdPedidoDesc(idCliente);
        if (pedidos.isEmpty()) return List.of();

        List<Integer> ids = pedidos.stream().map(Pedido::getIdPedido).collect(Collectors.toList());
        List<DetallePedido> detalles = detallePedidoRepository.findByIdPedidoIn(ids);
        Map<Integer, Producto> productos = new HashMap<>();
        Set<Integer> idsProductos = detalles.stream().map(DetallePedido::getIdProducto).collect(Collectors.toSet());
        if (!idsProductos.isEmpty()) {
            for (Producto p : productoRepository.findAllById(idsProductos)) productos.put(p.getIdProducto(), p);
        }
        List<PedidoEstadoHistorial> hitos = historialRepository.findByIdPedidoInOrderByFechaAscIdHistorialAsc(ids);

        List<MiPedido> lista = new ArrayList<>();
        for (Pedido p : pedidos) {
            List<PedidoConDetalle.Linea> lineas = new ArrayList<>();
            for (DetallePedido d : detalles) {
                if (!d.getIdPedido().equals(p.getIdPedido())) continue;
                Producto prod = productos.get(d.getIdProducto());
                BigDecimal precio = d.getPrecioUnitario() == null ? BigDecimal.ZERO : d.getPrecioUnitario();
                lineas.add(new PedidoConDetalle.Linea(d.getIdProducto(),
                        prod != null ? prod.getNombre() : "Producto eliminado",
                        d.getTalla(), d.getColor(), d.getCantidad(), precio,
                        precio.multiply(BigDecimal.valueOf(d.getCantidad() == null ? 0 : d.getCantidad())),
                        prod != null ? prod.getImagenUrl() : null));
            }
            List<MiPedido.Hito> historial = new ArrayList<>();
            for (PedidoEstadoHistorial h : hitos) {
                if (h.getIdPedido().equals(p.getIdPedido())) historial.add(new MiPedido.Hito(h.getEstado(), h.getFecha()));
            }
            lista.add(new MiPedido(p.getIdPedido(), p.getFecha(), p.getEstado(), p.getSubtotal(), p.getDescuentoPorcentaje(),
                    p.getDescuentoValor(), p.getTotal(), p.getDireccionEntrega(), p.getTransportadora(), p.getNumeroGuia(),
                    lineas, historial));
        }
        return lista;
    }

    public PedidoConDetalle.Pagina buscar(String texto, LocalDate desde, LocalDate hasta, BigDecimal min,
                                          BigDecimal max, String estado, int pagina, int tamano) {
        String estadoFiltro = "";
        if (estado != null && !estado.isBlank()) {
            estadoFiltro = EstadoPedido.desdeTexto(estado).getEtiqueta(); // rechaza estados inventados
        }
        // Se quita el comodín "%" que escriba la persona. El guion bajo se deja: en un correo como
        // lina_tabares@gmail.com hace falta, y como comodín solo reemplaza un carácter.
        String textoFiltro = texto == null ? "" : texto.trim().toLowerCase().replace("%", "");
        String patron = "%" + textoFiltro + "%";

        LocalDateTime desdeF = (desde == null ? LocalDate.of(1970, 1, 1) : desde).atStartOfDay();
        LocalDateTime hastaF = (hasta == null ? LocalDate.of(9999, 1, 1) : hasta.plusDays(1)).atStartOfDay(); // el día "hasta" entra completo
        BigDecimal minF = min == null ? BigDecimal.ZERO : min;
        BigDecimal maxF = max == null ? MAX_VALOR : max;

        int p = Math.max(pagina, 0);
        int t = Math.min(Math.max(tamano, 1), 100);

        Page<PedidoListado> resultado = pedidoRepository.buscarListado(
                desdeF, hastaF, minF, maxF, estadoFiltro, textoFiltro, patron, PageRequest.of(p, t));

        // Productos de todos los pedidos de la página con UNA sola consulta (y los nombres con otra)
        List<Integer> idsPedidos = resultado.getContent().stream().map(PedidoListado::idPedido).collect(Collectors.toList());
        List<DetallePedido> detalles = idsPedidos.isEmpty() ? List.of() : detallePedidoRepository.findByIdPedidoIn(idsPedidos);
        Set<Integer> idsProductos = detalles.stream().map(DetallePedido::getIdProducto).collect(Collectors.toSet());
        Map<Integer, Producto> productos = new HashMap<>();
        if (!idsProductos.isEmpty()) {
            for (Producto prod : productoRepository.findAllById(idsProductos)) {
                productos.put(prod.getIdProducto(), prod);
            }
        }

        List<PedidoConDetalle> lista = new ArrayList<>();
        for (PedidoListado fila : resultado.getContent()) {
            List<PedidoConDetalle.Linea> lineas = new ArrayList<>();
            for (DetallePedido d : detalles) {
                if (!d.getIdPedido().equals(fila.idPedido())) continue;
                Producto prod = productos.get(d.getIdProducto());
                BigDecimal precio = d.getPrecioUnitario() == null ? BigDecimal.ZERO : d.getPrecioUnitario();
                lineas.add(new PedidoConDetalle.Linea(
                        d.getIdProducto(),
                        prod != null ? prod.getNombre() : "Producto eliminado (ID " + d.getIdProducto() + ")",
                        d.getTalla(), d.getColor(), d.getCantidad(), precio,
                        precio.multiply(BigDecimal.valueOf(d.getCantidad() == null ? 0 : d.getCantidad())),
                        prod != null ? prod.getImagenUrl() : null));
            }
            lista.add(new PedidoConDetalle(fila.idPedido(), fila.fecha(), fila.estado(), fila.subtotal(),
                    fila.descuentoPorcentaje(), fila.descuentoValor(), fila.total(), fila.direccionEntrega(),
                    fila.idCliente(), fila.nombreCliente(), fila.apellidoCliente(), fila.correoCliente(),
                    fila.telefonoCliente(), fila.transportadora(), fila.numeroGuia(), lineas));
        }
        return new PedidoConDetalle.Pagina(lista, resultado.getNumber(), Math.max(resultado.getTotalPages(), 1), resultado.getTotalElements());
    }
}
