package com.arquixpress.marketplace.notifications;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final Resend resend;
    private final String fromEmail;
    private final String appUrl;

    public EmailService(
            @Value("${app.notifications.resend-api-key}") String apiKey,
            @Value("${app.notifications.from-email}") String fromEmail,
            @Value("${app.notifications.app-url}") String appUrl) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
        this.appUrl = appUrl;
    }

    // ── Registro de usuario ──────────────────────────────────────────────────

    public void sendWelcome(String toEmail, String displayName) {
        String subject = "¡Bienvenido a ArquiXpress, " + displayName + "!";
        String html = """
                <h2>¡Hola, %s!</h2>
                <p>Tu cuenta en <strong>ArquiXpress</strong> ha sido creada exitosamente.</p>
                <p>Ya puedes explorar nuestro catálogo y hacer tus primeras compras.</p>
                <a href="%s" style="background:#1a73e8;color:white;padding:10px 20px;
                   border-radius:6px;text-decoration:none;">Ir a ArquiXpress</a>
                <p style="margin-top:24px;color:#666;font-size:12px;">
                    Si no creaste esta cuenta, ignora este mensaje.</p>
                """.formatted(displayName, appUrl);
        send(toEmail, subject, html);
    }

    // ── Cambio de contraseña ─────────────────────────────────────────────────

    public void sendSellerApproved(String toEmail, String displayName) {
        String subject = "Tu cuenta vendedora fue aprobada";
        String html = """
                <h2>Hola, %s</h2>
                <p>Tu solicitud para vender en <strong>ArquiXpress</strong> fue aprobada.</p>
                <p>Ya puedes administrar publicaciones desde tu portal de vendedor.</p>
                <a href="%s/vendedor" style="background:#1a73e8;color:white;padding:10px 20px;
                   border-radius:6px;text-decoration:none;">Ir al portal vendedor</a>
                """.formatted(displayName, appUrl);
        send(toEmail, subject, html);
    }

    public void sendSellerRejected(String toEmail, String displayName) {
        String subject = "Tu solicitud de vendedor fue revisada";
        String html = """
                <h2>Hola, %s</h2>
                <p>Tu solicitud para vender en ArquiXpress fue revisada y no fue aprobada.</p>
                <p>Puedes revisar la informacion enviada y crear una nueva solicitud desde el centro de vendedores.</p>
                <a href="%s/operaciones" style="background:#1a73e8;color:white;padding:10px 20px;
                   border-radius:6px;text-decoration:none;">Ver centro de vendedores</a>
                """.formatted(displayName, appUrl);
        send(toEmail, subject, html);
    }

    public void sendSellerSale(String toEmail, String displayName, String orderId) {
        String subject = "Nueva venta en ArquiXpress - Orden #" + shortId(orderId);
        String html = """
                <h2>Hola, %s</h2>
                <p>Recibiste una nueva compra asociada a la orden <strong>#%s</strong>.</p>
                <p>Revisa tu portal de vendedor para gestionar tus productos y stock.</p>
                <a href="%s/vendedor" style="background:#1a73e8;color:white;padding:10px 20px;
                   border-radius:6px;text-decoration:none;">Ir al portal vendedor</a>
                """.formatted(displayName, shortId(orderId), appUrl);
        send(toEmail, subject, html);
    }

    public void sendPasswordChanged(String toEmail, String displayName) {
        String subject = "Tu contraseña de ArquiXpress fue cambiada";
        String html = """
                <h2>Hola, %s</h2>
                <p>Te informamos que la contraseña de tu cuenta fue modificada recientemente.</p>
                <p>Si fuiste tú, no necesitas hacer nada más.</p>
                <p>Si <strong>no reconoces este cambio</strong>, contáctanos de inmediato
                   respondiendo este correo.</p>
                """.formatted(displayName);
        send(toEmail, subject, html);
    }

    // ── Orden pagada ─────────────────────────────────────────────────────────

    public void sendOrderPaid(String toEmail, String displayName, String orderId) {
        String subject = "¡Tu pago fue aprobado! Orden #" + shortId(orderId);
        String html = """
                <h2>¡Pago aprobado, %s!</h2>
                <p>Tu orden <strong>#%s</strong> fue pagada exitosamente
                   y está siendo procesada.</p>
                <a href="%s/orders/%s" style="background:#1a73e8;color:white;padding:10px 20px;
                   border-radius:6px;text-decoration:none;">Ver mi orden</a>
                """.formatted(displayName, shortId(orderId), appUrl, orderId);
        send(toEmail, subject, html);
    }

    // ── Pago rechazado ───────────────────────────────────────────────────────

    public void sendPaymentRejected(String toEmail, String displayName, String orderId) {
        String subject = "Tu pago no pudo procesarse — Orden #" + shortId(orderId);
        String html = """
                <h2>Hola, %s</h2>
                <p>Lamentablemente el pago de tu orden <strong>#%s</strong>
                   fue rechazado.</p>
                <p>Puedes intentarlo de nuevo con otro método de pago.</p>
                <a href="%s/orders/%s/retry" style="background:#e53935;color:white;
                   padding:10px 20px;border-radius:6px;text-decoration:none;">
                   Reintentar pago</a>
                """.formatted(displayName, shortId(orderId), appUrl, orderId);
        send(toEmail, subject, html);
    }

    // ── Actualización de envío ───────────────────────────────────────────────

    public void sendShipmentUpdated(String toEmail, String displayName,
                                    String orderId, String shipmentStatus) {
        String label = shipmentLabel(shipmentStatus);
        String subject = "Tu envío fue actualizado — Orden #" + shortId(orderId);
        String html = """
                <h2>Hola, %s</h2>
                <p>El estado de envío de tu orden <strong>#%s</strong>
                   cambió a: <strong>%s</strong></p>
                <a href="%s/orders/%s" style="background:#1a73e8;color:white;padding:10px 20px;
                   border-radius:6px;text-decoration:none;">Ver mi orden</a>
                """.formatted(displayName, shortId(orderId), label, appUrl, orderId);
        send(toEmail, subject, html);
    }

    // ── Envío entregado ──────────────────────────────────────────────────────

    public void sendShipmentDelivered(String toEmail, String displayName, String orderId) {
        String subject = "¡Tu pedido fue entregado! Orden #" + shortId(orderId);
        String html = """
                <h2>¡Tu pedido llegó, %s!</h2>
                <p>Tu orden <strong>#%s</strong> fue marcada como entregada.</p>
                <p>Esperamos que disfrutes tu compra. ¡Gracias por confiar en ArquiXpress!</p>
                <a href="%s" style="background:#1a73e8;color:white;padding:10px 20px;
                   border-radius:6px;text-decoration:none;">Seguir comprando</a>
                """.formatted(displayName, shortId(orderId), appUrl);
        send(toEmail, subject, html);
    }

    // ── Orden cancelada ──────────────────────────────────────────────────────

    public void sendOrderCancelled(String toEmail, String displayName, String orderId) {
        String subject = "Tu orden #" + shortId(orderId) + " fue cancelada";
        String html = """
                <h2>Hola, %s</h2>
                <p>Tu orden <strong>#%s</strong> ha sido cancelada.</p>
                <p>Si tienes dudas sobre esta cancelación, contáctanos respondiendo
                   este correo.</p>
                <a href="%s" style="background:#1a73e8;color:white;padding:10px 20px;
                   border-radius:6px;text-decoration:none;">Volver al catálogo</a>
                """.formatted(displayName, shortId(orderId), appUrl);
        send(toEmail, subject, html);
    }

    // ── Interno ──────────────────────────────────────────────────────────────

    private void send(String to, String subject, String html) {
        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(to)
                    .subject(subject)
                    .html(html)
                    .build();
            resend.emails().send(options);
            log.info("Email enviado a {} — asunto: {}", to, subject);
        } catch (ResendException e) {
            log.error("Error enviando email a {} — asunto: {} — error: {}",
                    to, subject, e.getMessage());
            throw new RuntimeException("Fallo al enviar email", e);
        }
    }

    private String shortId(String uuid) {
        return uuid.substring(0, 8).toUpperCase();
    }

    private String shipmentLabel(String status) {
        return switch (status) {
            case "PREPARING"  -> "En preparación";
            case "SHIPPED"    -> "Enviado";
            case "IN_TRANSIT" -> "En tránsito";
            case "DELIVERED"  -> "Entregado";
            default           -> status;
        };
    }
}
