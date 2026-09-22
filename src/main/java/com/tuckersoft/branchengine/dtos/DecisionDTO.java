package com.tuckersoft.branchengine.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * playthroughStatus, lucidity, controlLevel y endingCode son el estado de la partida
 * DESPUES de aplicar la decision.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DecisionDTO {
    private Long id;
    private Long playthroughId;
    private String playerTag;
    private String sourceNodeCode;
    private String resolvedNodeCode;
    private String rawInput;
    private String branchType;
    private String impactLevel;
    private String handlerUnit;
    private String outcomeCode;
    private String status;
    private String playthroughStatus;
    private Integer lucidity;
    private Integer controlLevel;
    private String endingCode;
    private Instant createdAt;
    private Instant updatedAt;
}
