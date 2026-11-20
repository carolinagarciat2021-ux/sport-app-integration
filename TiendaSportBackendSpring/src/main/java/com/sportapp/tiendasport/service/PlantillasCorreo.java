package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.dto.PedidoCorreo;
import com.sportapp.tiendasport.model.DatosEmpresa;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Textos y diseño de los correos de pedidos. Todo lo que escribe una persona se "escapa" para que no pueda meter HTML. */
public final class PlantillasCorreo {

    private PlantillasCorreo() {}

    private static String esc(String t) {
        return t == null ? "" : HtmlUtils.htmlEscape(t);
    }

    private static String plata(BigDecimal v) {
        NumberFormat f = NumberFormat.getInstance(Locale.forLanguageTag("es-CO"));
        f.setMaximumFractionDigits(0);
        return "$" + f.format(v == null ? BigDecimal.ZERO : v);
    }

    // ---------------------------------------------------------------- Correo al cliente

    public static String asuntoCliente(PedidoCorreo p, String tienda) {
        String n = "#" + p.idPedido();
        if (p.compraNueva()) return "Recibimos tu pedido " + n + " – " + tienda;
        switch (p.estado()) {
            case "Confirmado": return "Tu pedido " + n + " fue confirmado – " + tienda;
            case "Enviado":    return "Tu pedido " + n + " va en camino – " + tienda;
            case "Entregado":  return "Tu pedido " + n + " fue entregado – " + tienda;
            case "Cancelado":  return "Tu pedido " + n + " fue anulado – " + tienda;
            default:           return "Actualización de tu pedido " + n + " – " + tienda;
        }
    }

    public static String htmlCliente(PedidoCorreo p, DatosEmpresa e) {
        String titulo;
        String texto;
        if (p.compraNueva()) {
            titulo = "¡Gracias por tu compra!";
            texto = "Recibimos tu pedido y quedó confirmado. Te iremos avisando por este correo cada vez que avance.";
        } else {
            switch (p.estado()) {
                case "Confirmado":
                    titulo = "Tu pedido fue confirmado";
                    texto = "Ya confirmamos tu pedido y lo estamos preparando.";
                    break;
                case "Enviado":
                    titulo = "¡Tu pedido va en camino!";
                    texto = "Tu pedido ya fue entregado a la empresa de reparto y pronto llegará a tu domicilio.";
                    break;
                case "Entregado":
                    titulo = "Tu pedido fue entregado";
                    texto = "Registramos la entrega de tu pedido. ¡Gracias por comprar con nosotros!";
                    break;
                case "Cancelado":
                    titulo = "Tu pedido fue anulado";
                    texto = "Anulamos este pedido. Si tienes dudas, escríbenos y con gusto te ayudamos.";
                    break;
                default:
                    titulo = "Actualización de tu pedido";
                    texto = "El estado de tu pedido cambió a " + esc(p.estado()) + ".";
            }
        }

        StringBuilder sb = new StringBuilder();
        abrir(sb, e);
        sb.append("<h2 style=\"font-family:Georgia,serif;color:#1f1d1b;margin:0 0 8px\">").append(esc(titulo)).append("</h2>");
        sb.append("<p style=\"margin:0 0 14px\">Hola ").append(esc(p.nombreCliente())).append(",</p>");
        sb.append("<p style=\"margin:0 0 16px\">").append(texto).append("</p>");

        if ("Enviado".equals(p.estado()) && !p.compraNueva()) {
            sb.append("<div style=\"background:#fbf7ef;border:1px solid #e8dcc3;padding:12px 16px;margin:0 0 16px\">");
            sb.append("<strong>Datos del envío</strong><br>");
            if (hay(p.transportadora())) sb.append("Empresa de reparto: ").append(esc(p.transportadora())).append("<br>");
            if (hay(p.numeroGuia())) sb.append("Número de guía: <strong>").append(esc(p.numeroGuia())).append("</strong><br>");
            if (!hay(p.transportadora()) && !hay(p.numeroGuia())) sb.append("Te avisaremos si hay más datos del envío.<br>");
            sb.append("</div>");
        }
        resumen(sb, p, true);
        cerrar(sb, e);
        return sb.toString();
    }

    // ---------------------------------------------------------------- Aviso al administrador

    public static String asuntoAdmin(PedidoCorreo p, String tienda) {
        return "Nuevo pedido #" + p.idPedido() + " de " + p.nombreCliente() + " – " + tienda;
    }

    public static String htmlAdmin(PedidoCorreo p, DatosEmpresa e) {
        StringBuilder sb = new StringBuilder();
        abrir(sb, e);
        sb.append("<h2 style=\"font-family:Georgia,serif;color:#1f1d1b;margin:0 0 8px\">Nuevo pedido #").append(p.idPedido()).append("</h2>");
        sb.append("<p style=\"margin:0 0 16px\">Un cliente acaba de hacer un pedido en la tienda.</p>");
        sb.append("<div style=\"background:#fbf7ef;border:1px solid #e8dcc3;padding:12px 16px;margin:0 0 16px\">");
        sb.append("<strong>Cliente</strong><br>");
        sb.append(esc(p.nombreCliente())).append("<br>");
        sb.append("Correo: ").append(esc(p.correoCliente())).append("<br>");
        if (hay(p.telefonoCliente())) sb.append("Teléfono: ").append(esc(p.telefonoCliente())).append("<br>");
        if (hay(p.direccionEntrega())) sb.append("Dirección de entrega: ").append(esc(p.direccionEntrega())).append("<br>");
        sb.append("</div>");
        resumen(sb, p, false);
        sb.append("<p style=\"margin:16px 0 0\">Entra a la tienda → Pedidos para confirmarlo y cambiar su estado.</p>");
        cerrar(sb, e);
        return sb.toString();
    }

    // ---------------------------------------------------------------- Partes comunes

    private static boolean hay(String t) {
        return t != null && !t.isBlank();
    }

    private static void abrir(StringBuilder sb, DatosEmpresa e) {
        sb.append("<div style=\"background:#faf8f5;padding:24px 0;font-family:Arial,Helvetica,sans-serif;color:#2d2a26\">");
        sb.append("<div style=\"max-width:600px;margin:0 auto;background:#ffffff;border:1px solid #e6dfd2\">");
        sb.append("<div style=\"background:#1f1d1b;padding:18px 24px;text-align:center\">");
        sb.append("<span style=\"font-family:Georgia,serif;font-size:22px;letter-spacing:4px;color:#ffffff;text-transform:uppercase\">")
          .append(esc(e.getNombre())).append("</span><br>");
        sb.append("<span style=\"font-size:11px;letter-spacing:2px;color:#b8975a;text-transform:uppercase\">")
          .append(esc(e.getLema())).append("</span></div>");
        sb.append("<div style=\"padding:24px\">");
    }

    private static void resumen(StringBuilder sb, PedidoCorreo p, boolean mostrarDireccion) {
        sb.append("<table style=\"width:100%;border-collapse:collapse;font-size:14px\">");
        sb.append("<tr style=\"border-bottom:2px solid #eee;text-align:left\"><th style=\"padding:6px\">Producto</th>")
          .append("<th style=\"padding:6px\">Cant.</th><th style=\"padding:6px;text-align:right\">Subtotal</th></tr>");
        if (p.productos() != null) {
            for (PedidoCorreo.Linea l : p.productos()) {
                BigDecimal precio = l.precioUnitario() == null ? BigDecimal.ZERO : l.precioUnitario();
                BigDecimal sub = precio.multiply(BigDecimal.valueOf(l.cantidad() == null ? 0 : l.cantidad()));
                sb.append("<tr style=\"border-bottom:1px solid #f1ece2\"><td style=\"padding:6px\">").append(esc(l.nombre()));
                String det = ((hay(l.talla()) ? "Talla " + l.talla() : "") + (hay(l.talla()) && hay(l.color()) ? " · " : "")
                        + (hay(l.color()) ? l.color() : ""));
                if (!det.isEmpty()) sb.append("<br><span style=\"color:#7a7468;font-size:12px\">").append(esc(det)).append("</span>");
                sb.append("</td><td style=\"padding:6px\">").append(l.cantidad()).append("</td>")
                  .append("<td style=\"padding:6px;text-align:right\">").append(plata(sub)).append("</td></tr>");
            }
        }
        sb.append("</table>");
        sb.append("<p style=\"text-align:right;margin:12px 0 0;font-size:14px\">");
        if (p.descuentoValor() != null && p.descuentoValor().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Subtotal ").append(plata(p.subtotal())).append("<br>");
            sb.append("<span style=\"color:#2f6b3f\">Descuento mayorista ")
              .append(p.descuentoPorcentaje() == null ? "" : p.descuentoPorcentaje().stripTrailingZeros().toPlainString())
              .append("%: −").append(plata(p.descuentoValor())).append("</span><br>");
        }
        sb.append("<strong style=\"font-size:18px\">Total ").append(plata(p.total())).append("</strong></p>");
        if (mostrarDireccion && hay(p.direccionEntrega())) {
            sb.append("<p style=\"margin:16px 0 0;font-size:14px\"><strong>Dirección de entrega:</strong> ")
              .append(esc(p.direccionEntrega())).append("</p>");
        }
    }

    private static void cerrar(StringBuilder sb, DatosEmpresa e) {
        sb.append("</div>");
        sb.append("<div style=\"background:#1f1d1b;color:#e6e0d4;padding:16px 24px;font-size:12px;text-align:center\">");
        if (hay(e.getWhatsapp())) {
            sb.append("¿Dudas? Escríbenos por <a style=\"color:#e8d9b8\" href=\"https://wa.me/")
              .append(esc(e.getWhatsapp().replaceAll("\\D", ""))).append("\">WhatsApp</a>");
            if (hay(e.getCorreo())) sb.append(" o a ").append(esc(e.getCorreo()));
            sb.append("<br>");
        }
        sb.append("© ").append(esc(e.getNombre())).append("</div></div></div>");
    }
}
