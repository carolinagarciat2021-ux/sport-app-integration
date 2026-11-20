package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.model.Cliente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ImagenServiceTest {

    @Mock private JdbcTemplate jdbc;
    @Mock private ActividadService actividadService;
    @InjectMocks private ImagenService servicio;

    private byte[] cabecera(int... bytes) {
        byte[] d = new byte[64];
        for (int i = 0; i < bytes.length; i++) d[i] = (byte) bytes[i];
        return d;
    }

    @Test
    void reconocePorElContenidoJpgPngYWebp() {
        assertEquals("image/jpeg", ImagenService.detectarTipo(cabecera(0xFF, 0xD8, 0xFF, 0xE0)));
        assertEquals("image/png", ImagenService.detectarTipo(cabecera(0x89, 'P', 'N', 'G')));
        assertEquals("image/webp", ImagenService.detectarTipo(cabecera('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P')));
    }

    @Test
    void rechazaCualquierOtroContenidoAunqueSeLlameImagenJpg() {
        // Un archivo que empieza como un ejecutable o un texto no es una imagen
        assertNull(ImagenService.detectarTipo(cabecera('M', 'Z', 0, 0)));
        assertNull(ImagenService.detectarTipo("<script>alert(1)</script>".getBytes()));
        assertNull(ImagenService.detectarTipo(new byte[5]));
        assertNull(ImagenService.detectarTipo(null));
    }

    @Test
    void guardarRechazaVaciosFormatosInvalidosYArchivosGrandes() {
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(new byte[0], new Cliente()));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cabecera('M', 'Z'), new Cliente()));

        byte[] enorme = new byte[ImagenService.MAX_BYTES + 1];
        enorme[0] = (byte) 0xFF; enorme[1] = (byte) 0xD8; enorme[2] = (byte) 0xFF;
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(enorme, new Cliente()));
    }
}
