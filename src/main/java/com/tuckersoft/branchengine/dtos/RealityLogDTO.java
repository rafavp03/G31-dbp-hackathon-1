package com.tuckersoft.branchengine.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RealityLogDTO {
    private Long id;
    private Long decisionId;
    private String recipientEmail;
    private String subject;
    private String logStatus;
    private String errorMessage;
    private Instant sentAt;
    private Instant createdAt;
}
