package com.tuckersoft.branchengine.exceptions;

/** Se traduce a 404 en el GlobalExceptionHandler. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
