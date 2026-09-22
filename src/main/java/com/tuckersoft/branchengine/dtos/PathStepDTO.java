package com.tuckersoft.branchengine.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Un paso del recorrido. 'order' empieza en 1. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PathStepDTO {
    private Integer order;
    private Long decisionId;
    private String fromNodeCode;
    private String toNodeCode;
    private String branchType;
    private String impactLevel;
    private Instant createdAt;
}
