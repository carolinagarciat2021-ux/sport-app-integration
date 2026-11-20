package com.sportapp.tiendasport.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Una línea del historial de actividad: quién hizo qué y cuándo.
 * A propósito NO tiene llaves foráneas: el registro debe sobrevivir aunque
 * después se elimine el pedido, el producto o el usuario al que se refiere.
 * Nunca se edita ni se borra desde la aplicación.
 */
@Entity
@Table(name = "registro_actividad")
@Data
@NoArgsConstructor
public class RegistroActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Long idRegistro;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    // null cuando no hay usuario identificado (ej.: intento de login con un correo que no existe)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "correo_usuario", length = 150)
    private String correoUsuario;

    @Column(name = "rol_usuario", length = 20)
    private String rolUsuario;

    @Column(nullable = false, length = 40)
    private String accion;

    @Column(length = 30)
    private String entidad;

    @Column(name = "id_entidad")
    private Integer idEntidad;

    @Column(length = 1000)
    private String detalle;
}
