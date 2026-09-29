package com.ianblk.zianrct.rct;

import com.ianblk.zianrct.config.ZianRctConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessagePlaceholderValidator {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}]+)}");

    private MessagePlaceholderValidator() {
    }

    public static List<String> validate(ZianRctConfig.Messages messages) {
        List<String> errors = new ArrayList<>();
        validateField("messages.capUnlocked", messages.capUnlocked(), Set.of("cap"), errors);
        validateField("messages.medalObtained", messages.medalObtained(), Set.of("medal"), errors);
        validateField("messages.reloadSuccess", messages.reloadSuccess(), Set.of(), errors);
        validateField("messages.currentCap", messages.currentCap(), Set.of("cap"), errors);
        return List.copyOf(errors);
    }

    private static void validateField(
            String path,
            String text,
            Set<String> allowed,
            List<String> errors
    ) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (!allowed.contains(placeholder)) {
                errors.add(path + " usa un marcador no permitido: {" + placeholder + "}");
            }
        }
    }
}
