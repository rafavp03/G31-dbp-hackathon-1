package com.tuckersoft.branchengine.models;

import java.util.Set;

/**
 * El enunciado define el rol como String, no como enum: se guarda tal cual en la
 * columna y viaja igual en los DTOs. Estas constantes evitan los literales sueltos.
 */
public final class Roles {

    public static final String USER = "ROLE_USER";
    public static final String ADMIN = "ROLE_ADMIN";

    private static final Set<String> VALIDOS = Set.of(USER, ADMIN);

    public static boolean isValid(String role) {
        return role != null && VALIDOS.contains(role);
    }

    private Roles() {
    }
}
