package com.velrix.masterdata.domain;

import java.util.Locale;

public final class CodeNormalizer {

    private CodeNormalizer() {
    }

    public static String normalizeCode(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }
}
