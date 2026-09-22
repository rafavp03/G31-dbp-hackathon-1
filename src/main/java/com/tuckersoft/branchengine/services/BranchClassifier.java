package com.tuckersoft.branchengine.services;

import java.text.Normalizer;

/**
 * El motor clasifica con reglas propias: aqui no hay IA ni servicios externos, y
 * por eso el resultado es siempre el mismo para la misma entrada.
 *
 * Metodos estaticos a proposito: el DecisionService lo usa directamente y los tests
 * unitarios no tienen que mockearlo.
 */
public final class BranchClassifier {

    public static final String ENTRADA_CORRUPTA = "ENTRADA_CORRUPTA";
    public static final String RUPTURA_CUARTA_PARED = "RUPTURA_CUARTA_PARED";
    public static final String SOSPECHA = "SOSPECHA";
    public static final String REBELDIA = "REBELDIA";
    public static final String OBEDIENCIA = "OBEDIENCIA";

    /**
     * Minusculas y sin tildes, para que "CAMARA", "camara" y "camara" se comparen igual.
     */
    public static String normalizar(String rawInput) {
        return Normalizer.normalize(rawInput, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    /**
     * Las reglas se evaluan EN ESTE ORDEN y gana la primera que se cumple.
     *
     * Por eso "Stefan destruye la camara" es RUPTURA_CUARTA_PARED y no REBELDIA:
     * la regla 2 se evalua antes que la 4.
     */
    public static String clasificar(String rawInput) {
        String texto = normalizar(rawInput);

        // Regla 1: no contiene ninguna letra de la a a la z.
        if (!texto.matches(".*[a-z].*")) {
            return ENTRADA_CORRUPTA;
        }
        // Regla 2
        if (contiene(texto, "netflix", "camara", "espectador", "videojuego")) {
            return RUPTURA_CUARTA_PARED;
        }
        // Regla 3
        if (contiene(texto, "vigilan", "simbolo", "conspiracion")) {
            return SOSPECHA;
        }
        // Regla 4
        if (contiene(texto, "rechaza", "destruye", "desobedece", "renuncia")) {
            return REBELDIA;
        }
        // Regla 5: cualquier otro caso.
        return OBEDIENCIA;
    }

    /** El departamento que atiende la rama. Va sin tilde, tal cual. */
    public static String handlerUnit(String branchType) {
        return switch (branchType) {
            case OBEDIENCIA -> "Mesa de Guion";
            case REBELDIA -> "Control de Continuidad";
            case SOSPECHA -> "Oficina de Seguridad";
            case RUPTURA_CUARTA_PARED -> "Departamento Netflix";
            case ENTRADA_CORRUPTA -> "Archivo de Errores";
            default -> throw new IllegalArgumentException("branchType desconocido: " + branchType);
        };
    }

    public static String outcomeCode(String branchType) {
        return switch (branchType) {
            case OBEDIENCIA -> "ADVANCE_MAIN_PATH";
            case REBELDIA -> "FORK_TIMELINE";
            case SOSPECHA -> "INJECT_WHITE_BEAR_SYMBOL";
            case RUPTURA_CUARTA_PARED -> "BREAK_FOURTH_WALL";
            case ENTRADA_CORRUPTA -> "DISCARD_INPUT";
            default -> throw new IllegalArgumentException("branchType desconocido: " + branchType);
        };
    }

    private static boolean contiene(String texto, String... claves) {
        for (String clave : claves) {
            if (texto.contains(clave)) {
                return true;
            }
        }
        return false;
    }

    private BranchClassifier() {
    }
}
