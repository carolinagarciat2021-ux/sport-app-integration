package com.sportapp.tiendasport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class ClienteDtos {

    /**
     * Datos editables de un cliente. Todos son opcionales (si no vienen, se conserva
     * el valor actual), pero si vienen deben ser válidos. La contraseña se valida
     * además con PasswordValidator en el controlador.
     */
    @Data
    public static class ActualizarClienteRequest {
        @Size(min = 1, max = 50, message = "debe tener entre 1 y 50 caracteres")
        private String nombre;
        @Size(min = 1, max = 50, message = "debe tener entre 1 y 50 caracteres")
        private String apellido;
        @Size(min = 1, max = 20, message = "debe tener entre 1 y 20 caracteres")
        private String identificacion;
        @Size(min = 1, max = 15, message = "debe tener entre 1 y 15 caracteres")
        private String telefono;
        @Size(min = 1, max = 100, message = "debe tener entre 1 y 100 caracteres")
        private String direccion;
        @Email(message = "no es un correo válido")
        @Size(min = 1, max = 150, message = "debe tener entre 1 y 150 caracteres")
        private String correo;
        private String password; // vacío o ausente = no cambiar
        @Min(value = 1, message = "rol no válido")
        @Max(value = 3, message = "rol no válido")
        private Integer id_rol;
    }
}
