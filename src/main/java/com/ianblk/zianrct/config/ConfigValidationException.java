package com.ianblk.zianrct.config;

import java.util.List;

public final class ConfigValidationException extends IllegalArgumentException {
    private final List<String> errors;

    public ConfigValidationException(List<String> errors) {
        super("Configuración de Zian RCT inválida:\n - " + String.join("\n - ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
