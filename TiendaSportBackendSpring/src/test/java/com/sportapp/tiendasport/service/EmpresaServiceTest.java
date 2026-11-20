package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.DatosEmpresa;
import com.sportapp.tiendasport.repository.DatosEmpresaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceTest {

    @Mock private DatosEmpresaRepository repositorio;
    @Mock private ActividadService actividadService;
    @InjectMocks private EmpresaService servicio;

    private DatosEmpresa datos() {
        DatosEmpresa d = new DatosEmpresa();
        d.setNombre("Tienda Sport");
        d.setLema("Ropa deportiva para toda la familia");
        d.setWhatsapp("3127622295");
        d.setCorreo("tiendasport@gmail.com");
        d.setDireccion("Carrera 108 N° 60-61");
        d.setEnvioGratisDesde(new BigDecimal("500000"));
        return d;
    }

    @Test
    void elWhatsappSeNormalizaADigitosConCodigoDePais() {
        assertEquals("573127622295", EmpresaService.normalizarWhatsapp("+57 312 762 2295"));
        assertEquals("573127622295", EmpresaService.normalizarWhatsapp("312 762 2295"));
        assertEquals("573127622295", EmpresaService.normalizarWhatsapp("573127622295"));
        assertThrows(IllegalArgumentException.class, () -> EmpresaService.normalizarWhatsapp("12345"));
        assertThrows(IllegalArgumentException.class, () -> EmpresaService.normalizarWhatsapp(null));
    }

    @Test
    void guardaLosDatosValidosYLoRegistraEnElHistorial() {
        when(repositorio.findById(1)).thenReturn(Optional.empty());
        when(repositorio.save(any(DatosEmpresa.class))).thenAnswer(i -> i.getArgument(0));

        DatosEmpresa guardado = servicio.actualizar(datos(), new Cliente());

        assertEquals("573127622295", guardado.getWhatsapp());
        assertEquals(1, guardado.getIdEmpresa());
        assertTrue(guardado.getMensajeWhatsapp().contains("Tienda Sport")); // mensaje por defecto
        verify(actividadService).registrar(any(), eq("EMPRESA_ACTUALIZADA"), eq("empresa"), eq(1), anyString());
    }

    @Test
    void rechazaCorreoInvalidoEnlacesPeligrososYEnviosNegativos() {
        DatosEmpresa malCorreo = datos();
        malCorreo.setCorreo("sin-arroba");
        assertThrows(IllegalArgumentException.class, () -> servicio.actualizar(malCorreo, new Cliente()));

        DatosEmpresa enlaceMalo = datos();
        enlaceMalo.setInstagram("javascript:alert(1)");
        assertThrows(IllegalArgumentException.class, () -> servicio.actualizar(enlaceMalo, new Cliente()));

        DatosEmpresa envioNegativo = datos();
        envioNegativo.setEnvioGratisDesde(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> servicio.actualizar(envioNegativo, new Cliente()));

        DatosEmpresa sinNombre = datos();
        sinNombre.setNombre("  ");
        assertThrows(IllegalArgumentException.class, () -> servicio.actualizar(sinNombre, new Cliente()));

        verify(repositorio, never()).save(any(DatosEmpresa.class));
    }

    @Test
    void sinEnvioGratisEnElFormularioSeConservaElActualYElCorreoDeAvisosSeValida() {
        DatosEmpresa actual = datos();
        actual.setEnvioGratisDesde(new BigDecimal("500000"));
        when(repositorio.findById(1)).thenReturn(Optional.of(actual));
        when(repositorio.save(any(DatosEmpresa.class))).thenAnswer(i -> i.getArgument(0));

        DatosEmpresa nuevos = datos();
        nuevos.setEnvioGratisDesde(null); // el formulario de datos de la empresa ya no lo envía
        nuevos.setCorreoAvisos("carolinagarciat2021@gmail.com");
        DatosEmpresa guardado = servicio.actualizar(nuevos, new Cliente());

        assertEquals(0, new BigDecimal("500000").compareTo(guardado.getEnvioGratisDesde()));
        assertEquals("carolinagarciat2021@gmail.com", guardado.getCorreoAvisos());

        DatosEmpresa malAviso = datos();
        malAviso.setCorreoAvisos("no-es-correo");
        assertThrows(IllegalArgumentException.class, () -> servicio.actualizar(malAviso, new Cliente()));
    }

    @Test
    void elEnvioGratisSeActualizaPorSeparadoYSeValida() {
        when(repositorio.findById(1)).thenReturn(Optional.of(datos()));
        when(repositorio.save(any(DatosEmpresa.class))).thenAnswer(i -> i.getArgument(0));

        DatosEmpresa guardado = servicio.actualizarEnvioGratis(new BigDecimal("700000"), new Cliente());
        assertEquals(0, new BigDecimal("700000").compareTo(guardado.getEnvioGratisDesde()));
        verify(actividadService).registrar(any(), eq("ENVIO_GRATIS_ACTUALIZADO"), eq("empresa"), eq(1), anyString());

        assertThrows(IllegalArgumentException.class, () -> servicio.actualizarEnvioGratis(new BigDecimal("-5"), new Cliente()));
    }

    @Test
    void elDatoPublicoNoIncluyeElCorreoPrivadoDeAvisos() {
        DatosEmpresa d = datos();
        d.setCorreoAvisos("carolinagarciat2021@gmail.com");
        DatosEmpresa publico = servicio.paraPublico(d);
        assertNull(publico.getCorreoAvisos());
        assertEquals("tiendasport@gmail.com", publico.getCorreo());
        assertEquals("carolinagarciat2021@gmail.com", d.getCorreoAvisos()); // el original no se toca
    }

    @Test
    void aceptaEnlacesDeRedesConHttps() {
        when(repositorio.findById(1)).thenReturn(Optional.empty());
        when(repositorio.save(any(DatosEmpresa.class))).thenAnswer(i -> i.getArgument(0));
        DatosEmpresa d = datos();
        d.setInstagram("https://instagram.com/tiendasport");
        assertEquals("https://instagram.com/tiendasport", servicio.actualizar(d, new Cliente()).getInstagram());
    }
}
