package com.tuckersoft.branchengine.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** El formato de error del enunciado: error, message, timestamp, path. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ApiError {
    private String error;
    private String message;
    private Instant timestamp;
    private String path;
}
