package com.ianblk.zianrct.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ZianRctConfig(
        int schemaVersion,
        String activeProfile,
        Map<String, Profile> profiles,
        Messages messages
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public ZianRctConfig {
        profiles = profiles == null ? Map.of() : Map.copyOf(profiles);
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

        Profile rassvet = new Profile(10, 10, 100, "rassvet", chain, medals, false);
        return new ZianRctConfig(
                CURRENT_SCHEMA_VERSION,
                "rassvet",
                Map.of("rassvet", rassvet),
                Messages.defaults()
        );
    }

    private static MedalDefinition medal(String id, String trainer, String name, int order) {
        return new MedalDefinition(
                id,
                trainer,
                name,
                "Derrota a " + trainer + " para obtener esta medalla.",
                "zianrct:textures/gui/medals/" + id + ".png",
                null,
                order
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
                continue;
            }
            if (profile == null) {
                errors.add("profiles." + profileName + " no puede ser null");
                continue;
            }
            validateProfile(profileName, profile, errors);
        }

        if (messages == null) {
            errors.add("messages no puede ser null");
        } else {
            messages.validate("messages", errors);
        }

        return List.copyOf(errors);
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
            String linkPath = prefix + ".chain[" + index + "]";
            if (link == null) {
                errors.add(linkPath + " no puede ser null");
                continue;
            }
            if (isBlank(link.trainer)) {
                errors.add(linkPath + ".trainer no puede estar vacío");
                continue;
            }
            if (!trainers.add(link.trainer)) {
                errors.add(linkPath + ".trainer está duplicado: " + link.trainer);
            }

            int unlockCap = profile.unlockCap(index);
            if (unlockCap < profile.initialCap || unlockCap > profile.maxCap) {
                errors.add(linkPath + ".unlockCap efectivo debe estar entre initialCap y maxCap");
            }
            if (unlockCap < previousCap) {
                errors.add(linkPath + ".unlockCap efectivo no puede disminuir respecto al paso anterior");
            }
            previousCap = unlockCap;
        }

        Set<String> medalIds = new LinkedHashSet<>();
        Set<String> medalTrainers = new LinkedHashSet<>();
        Set<Integer> medalOrders = new LinkedHashSet<>();
        for (int index = 0; index < profile.medals.size(); index++) {
            MedalDefinition medal = profile.medals.get(index);
            String medalPath = prefix + ".medals[" + index + "]";
            if (medal == null) {
                errors.add(medalPath + " no puede ser null");
                continue;
            }
            if (isBlank(medal.id)) {
                errors.add(medalPath + ".id no puede estar vacío");
            } else if (!medalIds.add(medal.id)) {
                errors.add(medalPath + ".id está duplicado: " + medal.id);
            }
            if (isBlank(medal.trainer)) {
                errors.add(medalPath + ".trainer no puede estar vacío");
            } else {
                if (!trainers.contains(medal.trainer)) {
                    errors.add(medalPath + ".trainer no pertenece a chain: " + medal.trainer);
                }
                if (!medalTrainers.add(medal.trainer)) {
                    errors.add(medalPath + ".trainer ya tiene otra medalla: " + medal.trainer);
                }
            }
            if (isBlank(medal.name)) {
                errors.add(medalPath + ".name no puede estar vacío");
            }
            if (isBlank(medal.description)) {
                errors.add(medalPath + ".description no puede estar vacío");
            }
            if (medal.order < 0) {
                errors.add(medalPath + ".order no puede ser negativo");
            } else if (!medalOrders.add(medal.order)) {
                errors.add(medalPath + ".order está duplicado: " + medal.order);
            }
            if (medal.color != null && medal.color.isBlank()) {
                errors.add(medalPath + ".color debe omitirse/null o contener un valor");
            }
        }
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
            chain = chain == null ? List.of() : List.copyOf(chain);
            medals = medals == null ? List.of() : List.copyOf(medals);
        }

        public int unlockCap(int chainIndex) {
            ChainEntry entry = chain.get(chainIndex);
            if (entry.unlockCap != null) {
                return entry.unlockCap;
            }
            long computed = (long) initialCap + (long) step * (chainIndex + 1);
            return (int) Math.min(maxCap, computed);
        }

        public Map<String, Integer> trainerUnlockCaps() {
            Map<String, Integer> result = new LinkedHashMap<>();
            for (int index = 0; index < chain.size(); index++) {
                ChainEntry entry = chain.get(index);
                if (entry != null && entry.trainer != null) {
                    result.put(entry.trainer, unlockCap(index));
                }
            }
            return Map.copyOf(result);
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
            if (isBlank(capUnlocked)) {
                errors.add(prefix + ".capUnlocked no puede estar vacío");
            }
            if (isBlank(medalObtained)) {
                errors.add(prefix + ".medalObtained no puede estar vacío");
            }
            if (isBlank(reloadSuccess)) {
                errors.add(prefix + ".reloadSuccess no puede estar vacío");
            }
            if (isBlank(currentCap)) {
                errors.add(prefix + ".currentCap no puede estar vacío");
            }
        }
    }
}
