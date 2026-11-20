package com.facturaarca.util;

/**
 * Utilidades para consultas SQL/JPQL.
 */
public final class SqlUtils {

    private SqlUtils() {
    }

    /**
     * Escapa los caracteres especiales de LIKE ('!', '%', '_') usando '!' como carácter de escape.
     */
    public static String escaparLike(String valor) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            return "";
        }
        return limpio
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
