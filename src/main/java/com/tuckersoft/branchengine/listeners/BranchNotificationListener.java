package com.tuckersoft.branchengine.listeners;

import com.tuckersoft.branchengine.events.DecisionCommittedEvent;
import com.tuckersoft.branchengine.models.Decision;
import com.tuckersoft.branchengine.models.RealityLog;
import com.tuckersoft.branchengine.repositories.DecisionRepository;
import com.tuckersoft.branchengine.repositories.RealityLogRepository;
import com.tuckersoft.branchengine.services.RealityReportMailer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

/**
 * Envia el Informe de Realidad fuera de la peticion.
 *
 * Va en un @Component distinto al DecisionService a proposito: Spring no aplica
 * @Async a las llamadas internas de una clase, y el service no debe conocer al
 * JavaMailSender.
 *
 * AFTER_COMMIT garantiza que cuando este metodo corre, PostgreSQL ya confirmo la
 * decision: con un @EventListener normal el hilo asincrono podria buscar una fila
 * que todavia no existe.
 *
 * REQUIRES_NEW no es opcional: el listener corre en otro hilo, fuera de la
 * transaccion original, y necesita una propia para que sus saves persistan. Con un
 * @Transactional normal sobre un @TransactionalEventListener la aplicacion ni arranca.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BranchNotificationListener {

    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;
    private final RealityReportMailer mailer;

    @Async("branchExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCommit(DecisionCommittedEvent evento) {
        Decision decision = decisionRepository.findById(evento.decisionId()).orElse(null);
        if (decision == null) {
            log.error("No se encontro la decision {} al procesar el evento", evento.decisionId());
            return;
        }

        decision.setStatus(Decision.PROCESANDO);
        decision.setUpdatedAt(Instant.now());
        decisionRepository.save(decision);

        String asunto = mailer.subject(evento);

        RealityLog registro = new RealityLog();
        registro.setDecision(decision);
        registro.setRecipientEmail(evento.recipientEmail());
        registro.setSubject(asunto);
        registro.setCreatedAt(Instant.now());

        try {
            mailer.enviar(evento);

            decision.setStatus(Decision.ESTABILIZADA);
            registro.setLogStatus(RealityLog.SENT);
            registro.setSentAt(Instant.now());
            registro.setErrorMessage(null);
        } catch (Exception ex) {
            // Mismo catch para un fallo real de SMTP y para el Modo QA.
            decision.setStatus(Decision.ERROR);
            registro.setLogStatus(RealityLog.FAILED);
            registro.setSentAt(null);
            registro.setErrorMessage(ex.getMessage() == null ? ex.toString() : ex.getMessage());

            log.error("Fallo el envio del Informe de Realidad de la decision {}: {}",
                    evento.decisionId(), ex.getMessage(), ex);
        }

        decision.setUpdatedAt(Instant.now());
        decisionRepository.save(decision);
        realityLogRepository.save(registro);

        log.info("[BRANCH-LOG] Decision ID: {} | Player: {} | Branch: {} | Impact: {} | Unit: {}"
                        + " | Node: {} -> {} | Thread: {} | Status: {}",
                evento.decisionId(),
                evento.playerTag(),
                evento.branchType(),
                evento.impactLevel(),
                evento.handlerUnit(),
                evento.sourceNodeCode(),
                evento.resolvedNodeCode(),
                Thread.currentThread().getName(),
                decision.getStatus());
    }
}
