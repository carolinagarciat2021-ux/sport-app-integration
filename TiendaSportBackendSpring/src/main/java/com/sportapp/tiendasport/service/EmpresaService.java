package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.DatosEmpresa;
import com.sportapp.tiendasport.repository.DatosEmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final BigDecimal MAX_ENVIO = new BigDecimal("100000000");

    private final DatosEmpresaRepository repositorio;
    private final ActividadService actividadService;

    public DatosEmpresa obtener() {
        return repositorio.findById(1).orElseGet(EmpresaService::porDefecto);
    }

    /** Valida y guarda los datos. Quien llama (el controlador) ya comprobó que es Administrador. */
    @Transactional
    public DatosEmpresa actualizar(DatosEmpresa nuevos, Cliente actor) {
        if (nuevos == null) throw new IllegalArgumentException("No se recibieron datos.");

        String nombre = obligatorio(nuevos.getNombre(), "El nombre de la tienda", 80);
        String lema = obligatorio(nuevos.getLema(), "El lema", 150);
        String whatsapp = normalizarWhatsapp(nuevos.getWhatsapp());
        String correo = obligatorio(nuevos.getCorreo(), "El correo", 150);
        if (!CORREO.matcher(correo).matches()) {
            throw new IllegalArgumentException("El correo no es válido (ejemplo: tienda@gmail.com).");
        }
        String mensaje = opcional(nuevos.getMensajeWhatsapp(), "El mensaje de WhatsApp", 200);
        if (mensaje.isEmpty()) mensaje = "Hola, tengo una duda sobre un producto de " + nombre + ".";
        String direccion = opcional(nuevos.getDireccion(), "La dirección", 200);
        String horario = opcional(nuevos.getHorario(), "El horario de atención", 200);
        String tienda = opcional(nuevos.getTiendaVirtual(), "El texto de la tienda virtual", 200);
        String instagram = enlaceOpcional(nuevos.getInstagram(), "El enlace de Instagram");
        String facebook = enlaceOpcional(nuevos.getFacebook(), "El enlace de Facebook");

        String correoAvisos = opcional(nuevos.getCorreoAvisos(), "El correo para avisos de pedidos", 150);
        if (!correoAvisos.isEmpty() && !CORREO.matcher(correoAvisos).matches()) {
            throw new IllegalArgumentException("El correo para avisos de pedidos no es válido.");
        }

        DatosEmpresa actual = repositorio.findById(1).orElseGet(EmpresaService::porDefecto);

        // El envío gratis se edita en "Descuentos y compra mínima"; si aquí no viene, se conserva el actual
        BigDecimal envio = nuevos.getEnvioGratisDesde() == null
                ? (actual.getEnvioGratisDesde() == null ? BigDecimal.ZERO : actual.getEnvioGratisDesde())
                : validarEnvio(nuevos.getEnvioGratisDesde());

        List<String> cambios = new ArrayList<>();
        comparar(cambios, "nombre", actual.getNombre(), nombre);
        comparar(cambios, "lema", actual.getLema(), lema);
        comparar(cambios, "WhatsApp", actual.getWhatsapp(), whatsapp);
        comparar(cambios, "mensaje de WhatsApp", actual.getMensajeWhatsapp(), mensaje);
        comparar(cambios, "correo", actual.getCorreo(), correo);
        comparar(cambios, "correo para avisos de pedidos", actual.getCorreoAvisos(), correoAvisos);
        comparar(cambios, "dirección", actual.getDireccion(), direccion);
        comparar(cambios, "horario", actual.getHorario(), horario);
        comparar(cambios, "tienda virtual", actual.getTiendaVirtual(), tienda);
        comparar(cambios, "Instagram", actual.getInstagram(), instagram);
        comparar(cambios, "Facebook", actual.getFacebook(), facebook);
        if (actual.getEnvioGratisDesde() == null || actual.getEnvioGratisDesde().compareTo(envio) != 0) {
            cambios.add("envío gratis desde (" + (actual.getEnvioGratisDesde() == null ? "0" : actual.getEnvioGratisDesde().toPlainString())
                    + " -> " + envio.toPlainString() + ")");
        }

        actual.setIdEmpresa(1);
        actual.setNombre(nombre);
        actual.setLema(lema);
        actual.setWhatsapp(whatsapp);
        actual.setMensajeWhatsapp(mensaje);
        actual.setCorreo(correo);
        actual.setCorreoAvisos(correoAvisos);
        actual.setDireccion(direccion);
        actual.setHorario(horario);
        actual.setTiendaVirtual(tienda);
        actual.setInstagram(instagram);
        actual.setFacebook(facebook);
        actual.setEnvioGratisDesde(envio);
        DatosEmpresa guardado = repositorio.save(actual);

        actividadService.registrar(actor, "EMPRESA_ACTUALIZADA", "empresa", 1,
                cambios.isEmpty() ? "Guardado sin cambios" : "Campos modificados: " + String.join(", ", cambios));
        return guardado;
    }

    /** Lo que ve cualquier visitante: los mismos datos, pero SIN el correo privado donde llegan los avisos de pedidos. */
    public DatosEmpresa paraPublico(DatosEmpresa d) {
        DatosEmpresa copia = new DatosEmpresa();
        copia.setIdEmpresa(d.getIdEmpresa());
        copia.setNombre(d.getNombre());
        copia.setLema(d.getLema());
        copia.setWhatsapp(d.getWhatsapp());
        copia.setMensajeWhatsapp(d.getMensajeWhatsapp());
        copia.setCorreo(d.getCorreo());
        copia.setDireccion(d.getDireccion());
        copia.setHorario(d.getHorario());
        copia.setTiendaVirtual(d.getTiendaVirtual());
        copia.setInstagram(d.getInstagram());
        copia.setFacebook(d.getFacebook());
        copia.setEnvioGratisDesde(d.getEnvioGratisDesde());
        copia.setCorreoAvisos(null);
        return copia;
    }

    /** Solo cambia el valor desde el cual hay envío gratis (se edita en "Descuentos y compra mínima"). */
    @Transactional
    public DatosEmpresa actualizarEnvioGratis(BigDecimal valor, Cliente actor) {
        BigDecimal nuevo = validarEnvio(valor == null ? BigDecimal.ZERO : valor);
        DatosEmpresa actual = repositorio.findById(1).orElseGet(EmpresaService::porDefecto);
        BigDecimal antes = actual.getEnvioGratisDesde() == null ? BigDecimal.ZERO : actual.getEnvioGratisDesde();
        actual.setIdEmpresa(1);
        actual.setEnvioGratisDesde(nuevo);
        DatosEmpresa guardado = repositorio.save(actual);
        actividadService.registrar(actor, "ENVIO_GRATIS_ACTUALIZADO", "empresa", 1,
                "Envío gratis desde: $" + antes.toPlainString() + " -> $" + nuevo.toPlainString());
        return guardado;
    }

    private static BigDecimal validarEnvio(BigDecimal envio) {
        if (envio.compareTo(BigDecimal.ZERO) < 0 || envio.compareTo(MAX_ENVIO) > 0) {
            throw new IllegalArgumentException("El valor para envío gratis debe estar entre 0 y 100.000.000.");
        }
        return envio;
    }

    /**
     * Deja el WhatsApp solo con dígitos y con código de país. Acepta "+57 312 762 2295",
     * "312 762 2295" (se le agrega 57) o "573127622295".
     */
    static String normalizarWhatsapp(String texto) {
        String digitos = texto == null ? "" : texto.replaceAll("\\D", "");
        if (digitos.length() == 10 && digitos.startsWith("3")) {
            digitos = "57" + digitos; // celular colombiano sin código de país
        }
        if (digitos.length() < 11 || digitos.length() > 15) {
            throw new IllegalArgumentException(
                    "El WhatsApp debe incluir el código de país, por ejemplo 573001234567 (Colombia = 57).");
        }
        return digitos;
    }

    private static String obligatorio(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(campo + " es obligatorio.");
        return limitar(valor.trim(), campo, max);
    }

    private static String opcional(String valor, String campo, int max) {
        return valor == null ? "" : limitar(valor.trim(), campo, max);
    }

    private static String limitar(String valor, String campo, int max) {
        if (valor.length() > max) throw new IllegalArgumentException(campo + " no puede pasar de " + max + " caracteres.");
        return valor;
    }

    /** Los enlaces de redes solo pueden ser http(s): así no se cuelan enlaces peligrosos (javascript:...). */
    private static String enlaceOpcional(String valor, String campo) {
        String limpio = opcional(valor, campo, 300);
        if (!limpio.isEmpty() && !(limpio.startsWith("https://") || limpio.startsWith("http://"))) {
            throw new IllegalArgumentException(campo + " debe empezar por https://");
        }
        return limpio;
    }

    private static void comparar(List<String> cambios, String campo, String antes, String despues) {
        if (!Objects.equals(antes == null ? "" : antes, despues)) cambios.add(campo);
    }

    private static DatosEmpresa porDefecto() {
        DatosEmpresa d = new DatosEmpresa();
        d.setNombre("Tienda Sport");
        d.setLema("Ropa deportiva para toda la familia");
        d.setWhatsapp("573127622295");
        d.setMensajeWhatsapp("Hola, tengo una duda sobre un producto de Tienda Sport.");
        d.setCorreo("tiendasport@gmail.com");
        d.setCorreoAvisos("carolinagarciat2021@gmail.com");
        d.setDireccion("Carrera 108 N° 60-61");
        d.setHorario("Atención al cliente: horario de oficina");
        d.setTiendaVirtual("Tienda virtual: abierta las 24 horas");
        d.setInstagram("");
        d.setFacebook("");
        d.setEnvioGratisDesde(new BigDecimal("500000"));
        return d;
    }
}
