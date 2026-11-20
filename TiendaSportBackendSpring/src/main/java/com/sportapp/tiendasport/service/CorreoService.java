package com.sportapp.tiendasport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Envía correos por UNA de dos vías, según las variables de entorno:
 *  - API HTTPS de Brevo (BREVO_API_KEY + MAIL_FROM): funciona en Render gratis, que bloquea los puertos SMTP.
 *  - SMTP (MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD + MAIL_FROM): por ejemplo Gmail en tu computador.
 * Si no hay ninguna configurada, no envía nada y la tienda sigue funcionando. Nunca escribe claves en el log.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);
    private static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";

    public record Mensaje(String paraCorreo, String paraNombre, String asunto, String html, String remitenteNombre) {
    }

    private final ObjectProvider<JavaMailSender> smtp;
    private final String desde;
    private final String claveBrevo;
    private final String hostSmtp;
    private final String usuarioSmtp;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public CorreoService(ObjectProvider<JavaMailSender> smtp,
                         @Value("${app.mail.from:}") String desde,
                         @Value("${app.mail.brevo-api-key:}") String claveBrevo,
                         @Value("${spring.mail.host:}") String hostSmtp,
                         @Value("${spring.mail.username:}") String usuarioSmtp) {
        this.smtp = smtp;
        this.desde = desde == null ? "" : desde.trim();
        this.claveBrevo = claveBrevo == null ? "" : claveBrevo.trim();
        this.hostSmtp = hostSmtp == null ? "" : hostSmtp.trim();
        this.usuarioSmtp = usuarioSmtp == null ? "" : usuarioSmtp.trim();
    }

    public boolean usaBrevo() {
        return !desde.isEmpty() && !claveBrevo.isEmpty();
    }

    public boolean usaSmtp() {
        return !desde.isEmpty() && !hostSmtp.isEmpty() && !usuarioSmtp.isEmpty();
    }

    public boolean habilitado() {
        return usaBrevo() || usaSmtp();
    }

    /** Devuelve true si el correo salió. Nunca lanza excepciones: un correo fallido no debe afectar al pedido. */
    public boolean enviar(Mensaje m) {
        try {
            if (usaBrevo()) return enviarPorBrevo(m);
            if (usaSmtp()) return enviarPorSmtp(m);
            log.info("Correo no configurado: no se envió \"{}\"", m.asunto());
            return false;
        } catch (Exception e) {
            log.error("No se pudo enviar el correo \"{}\": {}", m.asunto(), e.getMessage());
            return false;
        }
    }

    private boolean enviarPorBrevo(Mensaje m) throws Exception {
        Map<String, Object> cuerpo = Map.of(
                "sender", Map.of("name", m.remitenteNombre() == null ? "" : m.remitenteNombre(), "email", desde),
                "to", List.of(Map.of("email", m.paraCorreo(), "name", m.paraNombre() == null ? "" : m.paraNombre())),
                "subject", m.asunto(),
                "htmlContent", m.html());
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(URL_BREVO))
                .timeout(Duration.ofSeconds(15))
                .header("api-key", claveBrevo)
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(cuerpo)))
                .build();
        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
        if (respuesta.statusCode() >= 200 && respuesta.statusCode() < 300) {
            log.info("Correo enviado por Brevo: \"{}\"", m.asunto());
            return true;
        }
        log.warn("Brevo rechazó el correo \"{}\" (código {}): {}", m.asunto(), respuesta.statusCode(), respuesta.body());
        return false;
    }

    private boolean enviarPorSmtp(Mensaje m) throws Exception {
        JavaMailSender sender = smtp.getIfAvailable();
        if (sender == null) return false;
        MimeMessage mensaje = sender.createMimeMessage();
        MimeMessageHelper ayuda = new MimeMessageHelper(mensaje, "UTF-8");
        ayuda.setFrom(desde, m.remitenteNombre() == null ? desde : m.remitenteNombre());
        ayuda.setTo(m.paraCorreo());
        ayuda.setSubject(m.asunto());
        ayuda.setText(m.html(), true);
        sender.send(mensaje);
        log.info("Correo enviado por SMTP: \"{}\"", m.asunto());
        return true;
    }
}
