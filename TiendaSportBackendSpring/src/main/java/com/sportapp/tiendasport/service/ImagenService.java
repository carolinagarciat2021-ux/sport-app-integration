package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.model.Cliente;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Guarda las fotos de los productos en la base de datos (tabla imagen_producto), no en el disco
 * del servidor: en Render el disco se borra en cada despliegue, la base de datos no.
 * Solo se aceptan JPG, PNG y WEBP, verificados por el contenido real del archivo (no por el nombre).
 */
@Service
@RequiredArgsConstructor
public class ImagenService {

    public static final int MAX_BYTES = 3 * 1024 * 1024; // 3 MB

    public record ImagenGuardada(String tipo, byte[] datos) {}

    private final JdbcTemplate jdbc;
    private final ActividadService actividadService;

    /** Reconoce el formato por los primeros bytes. Devuelve null si no es JPG, PNG ni WEBP. */
    public static String detectarTipo(byte[] d) {
        if (d == null || d.length < 12) return null;
        if ((d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) return "image/jpeg";
        if ((d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') return "image/png";
        if (d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') return "image/webp";
        return null;
    }

    @Transactional
    public long guardar(byte[] datos, Cliente actor) {
        if (datos == null || datos.length == 0) {
            throw new IllegalArgumentException("No se recibió ninguna imagen.");
        }
        if (datos.length > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen es demasiado grande (máximo 3 MB).");
        }
        String tipo = detectarTipo(datos);
        if (tipo == null) {
            throw new IllegalArgumentException("Formato no permitido. Usa una imagen JPG, PNG o WEBP.");
        }

        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO imagen_producto (tipo_contenido, datos, fecha) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, tipo);
            ps.setBytes(2, datos);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, llave);

        Number id = llave.getKey();
        if (id == null) {
            throw new IllegalStateException("No se pudo guardar la imagen.");
        }
        actividadService.registrar(actor, "IMAGEN_SUBIDA", "imagen", id.intValue(),
                "Imagen subida (" + tipo + ", " + (datos.length / 1024) + " KB)");
        return id.longValue();
    }

    public Optional<ImagenGuardada> buscar(long id) {
        return jdbc.query(
                "SELECT tipo_contenido, datos FROM imagen_producto WHERE id_imagen = ?",
                rs -> rs.next()
                        ? Optional.of(new ImagenGuardada(rs.getString("tipo_contenido"), rs.getBytes("datos")))
                        : Optional.<ImagenGuardada>empty(),
                id);
    }
}
