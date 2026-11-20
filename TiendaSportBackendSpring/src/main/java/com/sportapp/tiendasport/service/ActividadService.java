package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.RegistroActividad;
import com.sportapp.tiendasport.repository.RegistroActividadRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escribe el historial de actividad. Se ejecuta DENTRO de la transacción de quien
 * lo llama: si se elimina un pedido, el registro de "quién lo eliminó y cómo era"
 * se guarda o se descarta junto con esa operación. NUNCA debe recibir contraseñas
 * ni tokens: solo texto descriptivo.
 */
@Service
@RequiredArgsConstructor
public class ActividadService {

    private static final Logger log = LoggerFactory.getLogger(ActividadService.class);
    private static final int MAX_DETALLE = 1000;

    private final RegistroActividadRepository repositorio;

    /** Registra una acción hecha por un usuario identificado. */
    @Transactional
    public void registrar(Cliente actor, String accion, String entidad, Integer idEntidad, String detalle) {
        RegistroActividad r = new RegistroActividad();
        if (actor != null) {
            r.setIdUsuario(actor.getIdClientes());
            r.setCorreoUsuario(actor.getCorreo());
            r.setRolUsuario(nombreRol(actor.getIdRol()));
        }
        guardar(r, accion, entidad, idEntidad, detalle);
    }

    /** Registra un evento sin usuario identificado (ej.: login fallido). */
    @Transactional
    public void registrarAnonimo(String correoIntentado, String accion, String detalle) {
        RegistroActividad r = new RegistroActividad();
        r.setCorreoUsuario(correoIntentado == null ? null : recortar(correoIntentado, 150));
        guardar(r, accion, null, null, detalle);
    }

    private void guardar(RegistroActividad r, String accion, String entidad, Integer idEntidad, String detalle) {
        r.setAccion(accion);
        r.setEntidad(entidad);
        r.setIdEntidad(idEntidad);
        r.setDetalle(detalle == null ? null : recortar(detalle, MAX_DETALLE));
        repositorio.save(r);
        log.info("ACTIVIDAD {} por {} ({}) sobre {} {}", accion, r.getCorreoUsuario(), r.getRolUsuario(), entidad, idEntidad);
    }

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 3) + "...";
    }

    private static String nombreRol(Integer idRol) {
        if (idRol == null) return null;
        return switch (idRol) {
            case 1 -> "Administrador";
            case 2 -> "Cliente";
            case 3 -> "Vendedor";
            default -> "Rol " + idRol;
        };
    }
}
