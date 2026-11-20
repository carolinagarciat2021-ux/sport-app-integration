package com.sportapp.tiendasport.controller;

import com.sportapp.tiendasport.dto.MensajeResponse;
import com.sportapp.tiendasport.dto.MiPedido;
import com.sportapp.tiendasport.dto.PedidoConDetalle;
import com.sportapp.tiendasport.dto.PedidoDtos.ItemCompra;
import com.sportapp.tiendasport.dto.PedidoDtos.PedidoCompletoResponse;
import com.sportapp.tiendasport.dto.PedidoDtos.PedidoRequest;
import com.sportapp.tiendasport.dto.PedidoDtos.PedidoResponse;
import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.Pedido;
import com.sportapp.tiendasport.repository.ConfiguracionRepository;
import com.sportapp.tiendasport.repository.PedidoRepository;
import com.sportapp.tiendasport.service.PedidoConsultaService;
import com.sportapp.tiendasport.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoRepository pedidoRepository;
    private final PedidoService pedidoService;
    private final ConfiguracionRepository configuracionRepository;
    private final PedidoConsultaService pedidoConsultaService;

    @GetMapping
    public List<Pedido> listar(@AuthenticationPrincipal Cliente cliente) {
        boolean esAdminOVendedor = cliente.getIdRol() == 1 || cliente.getIdRol() == 3;
        // Cliente: solo ve sus propios pedidos. Administrador/Vendedor: ven todos.
        return esAdminOVendedor ? pedidoRepository.findAll() : pedidoRepository.findByIdCliente(cliente.getIdClientes());
    }

    /** Los pedidos del usuario con sesión iniciada, con su línea de tiempo (Pendiente → Confirmado → Enviado → Entregado). */
    @GetMapping("/mis-pedidos")
    public List<MiPedido> misPedidos(@AuthenticationPrincipal Cliente cliente) {
        return pedidoConsultaService.misPedidos(cliente.getIdClientes());
    }

    /**
     * Todos los pedidos con quién los hizo (nombre, correo, teléfono) y sus productos. Solo Administrador y Vendedor.
     * Filtros opcionales: q (nombre, apellido, correo o identificación), desde/hasta (AAAA-MM-DD), min/max (valor del pedido), estado.
     */
    @GetMapping("/todos")
    public PedidoConDetalle.Pagina todos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return pedidoConsultaService.buscar(q, desde, hasta, min, max, estado, pagina, tamano);
    }

    /**
     * Compra completa (checkout). Recibe SOLO [{id_producto, talla, color, cantidad}].
     * El precio y el total los calcula el servidor; cualquier precio enviado se ignora.
     */
    @PostMapping("/completo")
    public ResponseEntity<?> crearCompleto(@RequestBody List<ItemCompra> items,
                                           @AuthenticationPrincipal Cliente cliente) {
        Pedido pedido = pedidoService.crearPedidoCompleto(cliente, items);
        return ResponseEntity.status(HttpStatus.CREATED).body(new PedidoCompletoResponse(
                "Pedido confirmado exitosamente", pedido.getIdPedido(), pedido.getSubtotal(),
                pedido.getDescuentoPorcentaje(), pedido.getDescuentoValor(), pedido.getTotal(), pedido.getEstado()));
    }

    /** @deprecated flujo antiguo; usar POST /api/pedidos/completo. Se elimina cuando web y Android terminen de migrar. */
    @Deprecated
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody PedidoRequest request, @AuthenticationPrincipal Cliente cliente) {
        BigDecimal montoMinimo = configuracionRepository.findById(1)
                .map(c -> c.getMontoMinimoCompra())
                .orElse(new BigDecimal("200000"));

        if (request.getMonto_total() == null || request.getMonto_total().compareTo(montoMinimo) < 0) {
            NumberFormat formato = NumberFormat.getInstance(Locale.forLanguageTag("es-CO"));
            formato.setMaximumFractionDigits(0);
            BigDecimal montoPedido = request.getMonto_total() == null ? BigDecimal.ZERO : request.getMonto_total();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                MensajeResponse.error("El monto mínimo de compra es de $" + formato.format(montoMinimo)
                    + ". Tu pedido actual es de $" + formato.format(montoPedido) + ".")
            );
        }

        Pedido pedido = pedidoService.crearPedido(cliente.getIdClientes(), request.getEstado());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PedidoResponse("Pedido registrado exitosamente", pedido.getIdPedido()));
    }

    /** Cambio de estado (Administrador y Vendedor). Anular = estado "Cancelado" (solo Administrador). */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarEstado(@PathVariable Integer id, @RequestBody PedidoRequest request,
                                              @AuthenticationPrincipal Cliente solicitante) {
        pedidoService.cambiarEstado(id, request.getEstado(), request.getTransportadora(), request.getNumero_guia(), solicitante);
        return ResponseEntity.ok(MensajeResponse.ok("Pedido actualizado correctamente"));
    }

    /** Eliminación definitiva: solo Administrador. Devuelve el stock si el pedido no estaba anulado. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id, @AuthenticationPrincipal Cliente solicitante) {
        pedidoService.eliminarPedido(id, solicitante);
        return ResponseEntity.ok(MensajeResponse.ok("Pedido eliminado"));
    }
}
