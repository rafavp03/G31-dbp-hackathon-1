package com.tuckersoft.branchengine.exceptions;

/** Se traduce a 400 para las validaciones que no cubre Bean Validation. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
