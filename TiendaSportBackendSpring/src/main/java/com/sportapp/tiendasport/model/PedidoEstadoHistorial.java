package com.sportapp.tiendasport.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Cada vez que un pedido cambia de estado se guarda una fila: es la "línea del tiempo" que ve el cliente. */
@Entity
@Table(name = "pedido_estado_historial")
@Data
@NoArgsConstructor
public class PedidoEstadoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long idHistorial;

    @Column(name = "id_pedido", nullable = false)
    private Integer idPedido;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    // Correo de quien hizo el cambio (el cliente en la compra; el vendedor o administrador después)
    @Column(name = "por_usuario", length = 150)
    private String porUsuario;
}
