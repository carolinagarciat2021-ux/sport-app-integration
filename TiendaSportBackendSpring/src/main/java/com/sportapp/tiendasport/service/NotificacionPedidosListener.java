package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.dto.PedidoCorreo;
import com.sportapp.tiendasport.model.DatosEmpresa;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía los correos de pedidos DESPUÉS de que el pedido quedó guardado y en segundo plano.
 * Si un correo falla (clave mala, internet, etc.) solo se registra el error: el pedido no se pierde.
 *
 *  - Cada compra nueva: confirmación al cliente + aviso al administrador (correo de avisos).
 *  - Cada cambio de estado (Confirmado, Enviado, Entregado, Cancelado): correo al cliente.
 */
@Component
@RequiredArgsConstructor
public class NotificacionPedidosListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionPedidosListener.class);

    private final CorreoService correoService;
    private final EmpresaService empresaService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alCambiarPedido(PedidoCorreo pedido) {
        enviarAvisos(pedido);
    }

    void enviarAvisos(PedidoCorreo p) {
        try {
            if (!correoService.habilitado()) {
                log.info("Correos sin configurar: no se avisó del pedido #{} ({}).", p.idPedido(), p.estado());
                return;
            }
            DatosEmpresa empresa = empresaService.obtener();
            String tienda = empresa.getNombre();

            if (p.correoCliente() != null && !p.correoCliente().isBlank()) {
                correoService.enviar(new CorreoService.Mensaje(p.correoCliente(), p.nombreCliente(),
                        PlantillasCorreo.asuntoCliente(p, tienda), PlantillasCorreo.htmlCliente(p, empresa), tienda));
            }

            String avisos = empresa.getCorreoAvisos();
            if (p.compraNueva() && avisos != null && !avisos.isBlank()) {
                correoService.enviar(new CorreoService.Mensaje(avisos, "Administrador",
                        PlantillasCorreo.asuntoAdmin(p, tienda), PlantillasCorreo.htmlAdmin(p, empresa), tienda));
            }
        } catch (Exception e) {
            log.error("No se pudo avisar por correo del pedido #{}: {}", p.idPedido(), e.getMessage());
        }
    }
}
