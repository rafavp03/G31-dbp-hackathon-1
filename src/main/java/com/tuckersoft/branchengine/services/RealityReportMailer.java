package com.tuckersoft.branchengine.services;

import com.tuckersoft.branchengine.events.DecisionCommittedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Arma y envia el Informe de Realidad.
 *
 * Se usa MimeMessage con charset UTF-8 y no SimpleMailMessage para que las tildes y
 * los separadores del cuerpo lleguen legibles al servidor de correo.
 */
@Service
@RequiredArgsConstructor
public class RealityReportMailer {

    /** El valor que activa el Modo QA desde la cabecera X-Bandersnatch-Simulate. */
    public static final String MAIL_FAILURE = "MAIL_FAILURE";

    private static final String SEPARADOR = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:qa@tuckersoft.test}")
    private String remitente;

    /** El asunto se calcula aparte porque el RealityLog lo necesita aunque el envio falle. */
    public String subject(DecisionCommittedEvent evento) {
        return "[TUCKERSOFT] " + evento.branchType() + " en " + evento.playerTag()
                + " | Impacto " + evento.impactLevel();
    }

    public void enviar(DecisionCommittedEvent evento) throws MessagingException {
        // Modo QA: una excepcion de verdad, lanzada dentro del mismo flujo que el envio
        // real, para que la atrape el mismo catch del listener.
        if (MAIL_FAILURE.equals(evento.simulate())) {
            throw new MailSendException(
                    "Fallo de SMTP simulado por la cabecera X-Bandersnatch-Simulate: " + MAIL_FAILURE);
        }

        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");

        helper.setFrom(remitente);
        helper.setTo(evento.recipientEmail());
        helper.setSubject(subject(evento));
        helper.setText(cuerpo(evento));

        mailSender.send(mensaje);
    }

    private String cuerpo(DecisionCommittedEvent e) {
        return """
                Hola %s,

                Una partida de prueba acaba de ramificarse.

                %s
                Decision ID      : #%d
                Jugador          : %s
                Rama             : %s
                Impacto          : %s
                Departamento     : %s
                Consecuencia     : %s
                Nodo origen      : %s
                Nodo destino     : %s
                Estado partida   : %s
                Lucidez          : %d/100
                Nivel de control : %d/100
                Final            : %s
                Registrada       : %s
                %s

                Decisión original del jugador:
                "%s"

                — Tuckersoft Branch Engine, 1984
                """.formatted(
                e.recipientDisplayName(),
                SEPARADOR,
                e.decisionId(),
                e.playerTag(),
                e.branchType(),
                e.impactLevel(),
                e.handlerUnit(),
                e.outcomeCode(),
                oGuion(e.sourceNodeCode()),
                oGuion(e.resolvedNodeCode()),
                e.playthroughStatus(),
                e.lucidity(),
                e.controlLevel(),
                oGuion(e.endingCode()),
                e.createdAt(),
                SEPARADOR,
                e.rawInput());
    }

    /** Un valor nulo se imprime como '-', nunca como "null". */
    private String oGuion(String valor) {
        return valor == null ? "-" : valor;
    }
}
