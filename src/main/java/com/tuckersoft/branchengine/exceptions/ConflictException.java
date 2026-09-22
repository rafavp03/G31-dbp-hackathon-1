package com.tuckersoft.branchengine.exceptions;

/** Se traduce a 409: email, nodeCode o playerTag repetido, o partida ya FINALIZADA. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
