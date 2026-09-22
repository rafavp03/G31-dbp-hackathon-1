package com.tuckersoft.branchengine.models;

/**
 * El impacto llega como String en el request (validado con @Pattern) y aqui viven
 * las dos tablas que dependen de el: cuanto mueve los stats y si desvia la rama.
 */
public final class ImpactLevel {

    public static final String LEVE = "LEVE";
    public static final String MODERADO = "MODERADO";
    public static final String GRAVE = "GRAVE";
    public static final String CRITICO = "CRITICO";

    /** Lo que resta a la lucidez. */
    public static int deltaLucidity(String impactLevel) {
        return switch (impactLevel) {
            case LEVE -> -5;
            case MODERADO -> -15;
            case GRAVE -> -30;
            case CRITICO -> -40;
            default -> throw new IllegalArgumentException("impactLevel desconocido: " + impactLevel);
        };
    }

    /** Lo que suma al nivel de control. */
    public static int deltaControl(String impactLevel) {
        return switch (impactLevel) {
            case LEVE -> 5;
            case MODERADO -> 10;
            case GRAVE -> 20;
            case CRITICO -> 45;
            default -> throw new IllegalArgumentException("impactLevel desconocido: " + impactLevel);
        };
    }

    private ImpactLevel() {
    }
}
