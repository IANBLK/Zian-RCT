package com.ianblk.zianrct.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ZianRctConfig(
        int schemaVersion,
        String activeProfile,
        Map<String, Profile> profiles,
        Messages messages
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Pattern SAFE_ID = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}]+)}");

    public ZianRctConfig {
        profiles = profiles == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(profiles));
    }

    public static ZianRctConfig defaults() {
        List<ChainEntry> chain = List.of(
                new ChainEntry("rassvet_leader_novato", null),
                new ChainEntry("rassvet_leader_ferrum", null),
                new ChainEntry("rassvet_leader_aquila", null),
                new ChainEntry("rassvet_leader_voltar", null),
                new ChainEntry("rassvet_leader_engranaje", null),
                new ChainEntry("rassvet_leader_bruma", null),
                new ChainEntry("rassvet_leader_cognitus", null),
                new ChainEntry("rassvet_leader_forjax", null),
                new ChainEntry("rassvet_leader_glacius", null),
                new ChainEntry("rassvet_master_aurelia", null)
        );
        List<MedalDefinition> medals = List.of(
                medal("novato", "rassvet_leader_novato", "Medalla Novato", 0),
                medal("ferrum", "rassvet_leader_ferrum", "Medalla Ferrum", 1),
                medal("aquila", "rassvet_leader_aquila", "Medalla Aquila", 2),
                medal("voltar", "rassvet_leader_voltar", "Medalla Voltar", 3),
                medal("engranaje", "rassvet_leader_engranaje", "Medalla Engranaje", 4),
                medal("bruma", "rassvet_leader_bruma", "Medalla Bruma", 5),
                medal("cognitus", "rassvet_leader_cognitus", "Medalla Cognitus", 6),
                medal("forjax", "rassvet_leader_forjax", "Medalla Forjax", 7),
                medal("glacius", "rassvet_leader_glacius", "Medalla Glacius", 8),
                medal("aurelia", "rassvet_master_aurelia", "Medalla Aurelia", 9)
        );
        LinkedHashMap<String, Profile> profiles = new LinkedHashMap<>();
        profiles.put("rassvet", new Profile(10, 10, 100, "rassvet", chain, medals, false));
        return new ZianRctConfig(CURRENT_SCHEMA_VERSION, "rassvet", profiles, Messages.defaults());
    }

    private static MedalDefinition medal(String id, String trainer, String name, int order) {
        return new MedalDefinition(
                id, trainer, name,
                "Derrota a " + trainer + " para obtener esta medalla.",
                "zianrct:textures/gui/medals/" + id + ".png",
                null, order
        );
    }

    public Profile activeProfileConfig() {
        return profiles.get(activeProfile);
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            errors.add("schemaVersion debe ser " + CURRENT_SCHEMA_VERSION + " pero es " + schemaVersion);
        }
        if (isBlank(activeProfile)) {
            errors.add("activeProfile no puede estar vacío");
        }
        if (profiles.isEmpty()) {
            errors.add("profiles debe contener al menos un perfil");
        } else if (!isBlank(activeProfile) && !profiles.containsKey(activeProfile)) {
            errors.add("activeProfile '" + activeProfile + "' no existe en profiles");
        }

        for (Map.Entry<String, Profile> entry : profiles.entrySet()) {
            String profileName = entry.getKey();
            Profile profile = entry.getValue();
            if (isBlank(profileName)) {
                errors.add("profiles contiene un nombre de perfil vacío");
            } else if (profile == null) {
                errors.add("profiles." + profileName + " no puede ser null");
            } else {
                validateProfile(profileName, profile, errors);
            }
        }

        if (messages == null) {
            errors.add("messages no puede ser null");
        } else {
            messages.validate("messages", errors);
        }
        return Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public void validateOrThrow() {
        List<String> errors = validate();
        if (!errors.isEmpty()) {
            throw new ConfigValidationException(errors);
        }
    }

    private static void validateProfile(String name, Profile profile, List<String> errors) {
        String prefix = "profiles." + name;
        if (profile.initialCap <= 0 || profile.initialCap > 100) {
            errors.add(prefix + ".initialCap debe estar entre 1 y 100");
        }
        if (profile.step <= 0) {
            errors.add(prefix + ".step debe ser mayor que 0");
        }
        if (profile.maxCap < profile.initialCap || profile.maxCap > 100) {
            errors.add(prefix + ".maxCap debe estar entre initialCap y 100");
        }
        if (isBlank(profile.series)) {
            errors.add(prefix + ".series no puede estar vacío");
        }
        if (profile.chain.isEmpty()) {
            errors.add(prefix + ".chain debe contener al menos un entrenador");
        }

        Set<String> trainers = new LinkedHashSet<>();
        int previousCap = profile.initialCap;
        for (int index = 0; index < profile.chain.size(); index++) {
            ChainEntry link = profile.chain.get(index);
            String path = prefix + ".chain[" + index + "]";
            if (link == null) {
                errors.add(path + " no puede ser null");
                continue;
            }
            if (isBlank(link.trainer)) {
                errors.add(path + ".trainer no puede estar vacío");
                continue;
            }
            if (!isSafeId(link.trainer)) {
                errors.add(path + ".trainer contiene caracteres inválidos: " + link.trainer);
            }
            if (!trainers.add(link.trainer)) {
                errors.add(path + ".trainer está duplicado: " + link.trainer);
            }
            int unlockCap = profile.unlockCap(index);
            if (unlockCap < profile.initialCap || unlockCap > profile.maxCap) {
                errors.add(path + ".unlockCap efectivo debe estar entre initialCap y maxCap");
            }
            if (unlockCap < previousCap) {
                errors.add(path + ".unlockCap efectivo no puede disminuir respecto al paso anterior");
            }
            previousCap = unlockCap;
        }

        Set<String> medalIds = new LinkedHashSet<>();
        Set<String> medalTrainers = new LinkedHashSet<>();
        Set<Integer> medalOrders = new LinkedHashSet<>();
        for (int index = 0; index < profile.medals.size(); index++) {
            MedalDefinition medal = profile.medals.get(index);
            String path = prefix + ".medals[" + index + "]";
            if (medal == null) {
                errors.add(path + " no puede ser null");
                continue;
            }
            if (isBlank(medal.id)) {
                errors.add(path + ".id no puede estar vacío");
            } else {
                if (!isSafeId(medal.id)) errors.add(path + ".id contiene caracteres inválidos: " + medal.id);
                if (!medalIds.add(medal.id)) errors.add(path + ".id está duplicado: " + medal.id);
            }
            if (isBlank(medal.trainer)) {
                errors.add(path + ".trainer no puede estar vacío");
            } else {
                if (!isSafeId(medal.trainer)) errors.add(path + ".trainer contiene caracteres inválidos: " + medal.trainer);
                if (!trainers.contains(medal.trainer)) errors.add(path + ".trainer no pertenece a chain: " + medal.trainer);
                if (!medalTrainers.add(medal.trainer)) errors.add(path + ".trainer ya tiene otra medalla: " + medal.trainer);
            }
            if (isBlank(medal.name)) errors.add(path + ".name no puede estar vacío");
            if (isBlank(medal.description)) errors.add(path + ".description no puede estar vacío");
            if (medal.order < 0) errors.add(path + ".order no puede ser negativo");
            else if (!medalOrders.add(medal.order)) errors.add(path + ".order está duplicado: " + medal.order);
            if (medal.color != null && medal.color.isBlank()) {
                errors.add(path + ".color debe omitirse/null o contener un valor");
            }
        }
    }

    private static void validatePlaceholders(String path, String text, Set<String> allowed, List<String> errors) {
        if (isBlank(text)) {
            errors.add(path + " no puede estar vacío");
            return;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (!allowed.contains(placeholder)) {
                errors.add(path + " usa un marcador no permitido: {" + placeholder + "}");
            }
        }
    }

    private static boolean isSafeId(String value) {
        return value != null && SAFE_ID.matcher(value).matches();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record Profile(
            int initialCap,
            int step,
            int maxCap,
            String series,
            List<ChainEntry> chain,
            List<MedalDefinition> medals,
            boolean giveMedalItem
    ) {
        public Profile {
            chain = chain == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(chain));
            medals = medals == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(medals));
        }

        public int unlockCap(int chainIndex) {
            ChainEntry entry = chain.get(chainIndex);
            if (entry == null) throw new IllegalStateException("chain[" + chainIndex + "] es null");
            if (entry.unlockCap != null) return entry.unlockCap;
            long computed = (long) initialCap + (long) step * (chainIndex + 1);
            return (int) Math.min(maxCap, computed);
        }

        public Map<String, Integer> trainerUnlockCaps() {
            LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
            for (int index = 0; index < chain.size(); index++) {
                ChainEntry entry = chain.get(index);
                if (entry != null && entry.trainer != null) result.put(entry.trainer, unlockCap(index));
            }
            return Collections.unmodifiableMap(new LinkedHashMap<>(result));
        }
    }

    public record ChainEntry(String trainer, Integer unlockCap) {
    }

    public record MedalDefinition(
            String id,
            String trainer,
            String name,
            String description,
            String texture,
            String color,
            int order
    ) {
    }

    public record Messages(
            String capUnlocked,
            String medalObtained,
            String reloadSuccess,
            String currentCap
    ) {
        public static Messages defaults() {
            return new Messages(
                    "Has desbloqueado el nivel {cap}.",
                    "Has obtenido {medal}.",
                    "Configuración de Zian RCT recargada correctamente.",
                    "Tu tope de nivel actual es {cap}."
            );
        }

        private void validate(String prefix, List<String> errors) {
            Objects.requireNonNull(errors, "errors");
            validatePlaceholders(prefix + ".capUnlocked", capUnlocked, Set.of("cap"), errors);
            validatePlaceholders(prefix + ".medalObtained", medalObtained, Set.of("medal"), errors);
            validatePlaceholders(prefix + ".reloadSuccess", reloadSuccess, Set.of(), errors);
            validatePlaceholders(prefix + ".currentCap", currentCap, Set.of("cap"), errors);
        }
    }
}
