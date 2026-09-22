package com.tuckersoft.branchengine.events;

import java.time.Instant;

/**
 * Lo que se publica cuando una decision queda registrada.
 *
 * El listener corre en otro hilo y despues del commit, asi que ahi ya no hay usuario
 * autenticado ni sesion de JPA viva: el evento lleva dentro TODO lo que hace falta
 * para armar el Informe de Realidad.
 */
public record DecisionCommittedEvent(
        Long decisionId,
        String recipientEmail,
        String recipientDisplayName,
        String playerTag,
        String branchType,
        String impactLevel,
        String handlerUnit,
        String outcomeCode,
        String sourceNodeCode,
        String resolvedNodeCode,
        String playthroughStatus,
        Integer lucidity,
        Integer controlLevel,
        String endingCode,
        String rawInput,
        Instant createdAt,
        String simulate) {
}
