package com.sportapp.tiendasport.controller;

import com.sportapp.tiendasport.dto.ClienteDtos.ActualizarClienteRequest;
import com.sportapp.tiendasport.dto.MensajeResponse;
import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.repository.ClienteRepository;
import com.sportapp.tiendasport.service.ActividadService;
import com.sportapp.tiendasport.util.PasswordValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActividadService actividadService;

    // Solo Administrador (ver SecurityConfig) — nunca devolvemos el campo password
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<Map<String, Object>> listar() {
        return clienteRepository.findAll().stream().map(c -> {
            Map<String, Object> mapa = new java.util.HashMap<>();
            mapa.put("id_clientes", c.getIdClientes());
            mapa.put("nombre", c.getNombre());
            mapa.put("apellido", c.getApellido());
            mapa.put("identificacion", c.getIdentificacion());
            mapa.put("telefono", c.getTelefono());
            mapa.put("direccion", c.getDireccion());
            mapa.put("correo", c.getCorreo());
            mapa.put("id_rol", c.getIdRol());
            // Ojo: "password" NUNCA se incluye aquí, ni siquiera cifrada.
            return mapa;
        }).toList();
    }

    // El propio cliente puede editar su perfil; un Administrador puede editar a cualquiera
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @Valid @RequestBody ActualizarClienteRequest datos,
                                         @AuthenticationPrincipal Cliente solicitante) {
        boolean esAdmin = solicitante.getIdRol() == 1;
        if (!esAdmin && !solicitante.getIdClientes().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(MensajeResponse.error("Solo puedes editar tu propio perfil"));
        }

        return clienteRepository.findById(id).map(cliente -> {
            if (datos.getIdentificacion() != null
                    && clienteRepository.existsByIdentificacionAndIdClientesNot(datos.getIdentificacion(), id)) {
                return ResponseEntity.badRequest().body(MensajeResponse.error("Ya existe otro usuario con esa identificación"));
            }
            if (datos.getCorreo() != null
                    && clienteRepository.existsByCorreoAndIdClientesNot(datos.getCorreo(), id)) {
                return ResponseEntity.badRequest().body(MensajeResponse.error("Ya existe otro usuario con ese correo"));
            }

            // Qué campos cambian (solo nombres de campo; NUNCA se registran contraseñas)
            java.util.List<String> cambios = new java.util.ArrayList<>();
            if (datos.getNombre() != null && !datos.getNombre().equals(cliente.getNombre())) cambios.add("nombre");
            if (datos.getApellido() != null && !datos.getApellido().equals(cliente.getApellido())) cambios.add("apellido");
            if (datos.getIdentificacion() != null && !datos.getIdentificacion().equals(cliente.getIdentificacion())) cambios.add("identificación");
            if (datos.getTelefono() != null && !datos.getTelefono().equals(cliente.getTelefono())) cambios.add("teléfono");
            if (datos.getDireccion() != null && !datos.getDireccion().equals(cliente.getDireccion())) cambios.add("dirección");
            if (datos.getCorreo() != null && !datos.getCorreo().equals(cliente.getCorreo())) cambios.add("correo (" + cliente.getCorreo() + " -> " + datos.getCorreo() + ")");
            if (datos.getPassword() != null && !datos.getPassword().isBlank()) cambios.add("contraseña");
            if (esAdmin && datos.getId_rol() != null && !datos.getId_rol().equals(cliente.getIdRol())) cambios.add("rol (" + cliente.getIdRol() + " -> " + datos.getId_rol() + ")");

            if (datos.getNombre() != null) cliente.setNombre(datos.getNombre());
            if (datos.getApellido() != null) cliente.setApellido(datos.getApellido());
            if (datos.getIdentificacion() != null) cliente.setIdentificacion(datos.getIdentificacion());
            if (datos.getTelefono() != null) cliente.setTelefono(datos.getTelefono());
            if (datos.getDireccion() != null) cliente.setDireccion(datos.getDireccion());
            if (datos.getCorreo() != null) cliente.setCorreo(datos.getCorreo());

            // Contraseña nueva: solo si viene y no está vacía; si no, se conserva la actual
            if (datos.getPassword() != null && !datos.getPassword().isBlank()) {
                String errorPassword = PasswordValidator.validar(datos.getPassword());
                if (errorPassword != null) {
                    return ResponseEntity.badRequest().body(MensajeResponse.error(errorPassword));
                }
                cliente.setPassword(passwordEncoder.encode(datos.getPassword()));
            }

            // Solo un Administrador puede cambiar el rol de alguien
            if (esAdmin && datos.getId_rol() != null) {
                cliente.setIdRol(datos.getId_rol());
            }

            clienteRepository.save(cliente);
            actividadService.registrar(solicitante, "CLIENTE_EDITADO", "cliente", id,
                    cambios.isEmpty() ? "Guardado sin cambios" : "Campos modificados: " + String.join(", ", cambios));
            return ResponseEntity.ok(MensajeResponse.ok("Cliente actualizado correctamente"));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(MensajeResponse.error("No se encontró el cliente")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> eliminar(@PathVariable Integer id, @AuthenticationPrincipal Cliente solicitante) {
        Cliente existente = clienteRepository.findById(id).orElse(null);
        if (existente == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(MensajeResponse.error("No existe ese cliente"));
        }
        // Se guarda quién era el usuario ANTES de borrarlo (sin contraseña)
        actividadService.registrar(solicitante, "CLIENTE_ELIMINADO", "cliente", id,
                "Usuario eliminado: " + existente.getNombre() + " " + existente.getApellido()
                        + " <" + existente.getCorreo() + ">, identificación " + existente.getIdentificacion()
                        + ", rol " + existente.getIdRol());
        clienteRepository.deleteById(id);
        return ResponseEntity.ok(MensajeResponse.ok("Cliente eliminado"));
    }
}
