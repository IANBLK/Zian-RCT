package com.ianblk.zianrct.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashSet;
import java.util.Set;

public final class MedalProtocol {
    public static final int CURRENT_VERSION = 1;
    public static final String NETWORK_VERSION = "1";

    public static final int MAX_SNAPSHOT_JSON_LENGTH = 131_072;
    public static final int MAX_PROFILE_LENGTH = 64;
    public static final int MAX_MEDALS = 128;
    public static final int MAX_ID_LENGTH = 128;
    public static final int MAX_NAME_LENGTH = 256;
    public static final int MAX_DESCRIPTION_LENGTH = 2_048;
    public static final int MAX_TEXTURE_LENGTH = 256;
    public static final int MAX_COLOR_LENGTH = 16;
    public static final int MAX_ORIGIN_LENGTH = 32;

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private MedalProtocol() {
    }

    public static String encode(MedalClientSnapshot snapshot) {
        validateSnapshot(snapshot);
        String json = GSON.toJson(snapshot);
        if (json.length() > MAX_SNAPSHOT_JSON_LENGTH) {
            throw new IllegalStateException("El snapshot de medallas supera el tamaño máximo permitido");
        }
        return json;
    }

    public static MedalClientSnapshot decode(int protocolVersion, String snapshotJson) {
        validateVersion(protocolVersion);
        if (snapshotJson == null || snapshotJson.length() > MAX_SNAPSHOT_JSON_LENGTH) {
            throw new IllegalStateException("El payload de medallas está vacío o excede el tamaño máximo");
        }
        MedalClientSnapshot snapshot = GSON.fromJson(snapshotJson, MedalClientSnapshot.class);
        validateSnapshot(snapshot);
        return snapshot;
    }

    public static void validateVersion(int protocolVersion) {
        if (protocolVersion != CURRENT_VERSION) {
            throw new IllegalStateException(
                    "Versión de protocolo de medallas incompatible: " + protocolVersion
                            + " (esperada " + CURRENT_VERSION + ")"
            );
        }
    }

    public static void validateMedalId(String medalId) {
        requireString("medalId", medalId, MAX_ID_LENGTH, false);
    }

    public static void validateSnapshot(MedalClientSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalStateException("El snapshot de medallas recibido está vacío");
        }
        requireString("activeProfile", snapshot.activeProfile(), MAX_PROFILE_LENGTH, false);
        if (snapshot.definitions().size() > MAX_MEDALS || snapshot.owned().size() > MAX_MEDALS) {
            throw new IllegalStateException("El snapshot contiene demasiadas medallas");
        }

        Set<String> definitionIds = new HashSet<>();
        for (MedalClientSnapshot.MedalDefinitionView definition : snapshot.definitions()) {
            if (definition == null) {
                throw new IllegalStateException("El snapshot contiene una definición nula");
            }
            requireString("definition.id", definition.id(), MAX_ID_LENGTH, false);
            requireString("definition.trainer", definition.trainer(), MAX_ID_LENGTH, false);
            requireString("definition.name", definition.name(), MAX_NAME_LENGTH, false);
            requireString("definition.description", definition.description(), MAX_DESCRIPTION_LENGTH, true);
            requireString("definition.texture", definition.texture(), MAX_TEXTURE_LENGTH, true);
            requireString("definition.color", definition.color(), MAX_COLOR_LENGTH, true);
            if (!definitionIds.add(definition.id())) {
                throw new IllegalStateException("El snapshot contiene ids de medalla duplicados: " + definition.id());
            }
            if (definition.unlockCap() < 0 || definition.unlockCap() > 10_000) {
                throw new IllegalStateException("unlockCap fuera de rango para " + definition.id());
            }
        }

        Set<String> ownedIds = new HashSet<>();
        for (MedalClientSnapshot.OwnedMedalView owned : snapshot.owned()) {
            if (owned == null) {
                throw new IllegalStateException("El snapshot contiene una medalla obtenida nula");
            }
            requireString("owned.medalId", owned.medalId(), MAX_ID_LENGTH, false);
            requireString("owned.origin", owned.origin(), MAX_ORIGIN_LENGTH, false);
            if (!definitionIds.contains(owned.medalId())) {
                throw new IllegalStateException("Medalla obtenida sin definición: " + owned.medalId());
            }
            if (!ownedIds.add(owned.medalId())) {
                throw new IllegalStateException("Medalla obtenida duplicada: " + owned.medalId());
            }
            if (owned.grantedAtEpochMilli() < 0) {
                throw new IllegalStateException("Fecha de medalla inválida para " + owned.medalId());
            }
        }
    }

    private static void requireString(String field, String value, int maxLength, boolean allowEmpty) {
        if (value == null || (!allowEmpty && value.isBlank())) {
            throw new IllegalStateException(field + " no puede estar vacío");
        }
        if (value.length() > maxLength) {
            throw new IllegalStateException(field + " supera el máximo de " + maxLength + " caracteres");
        }
    }
}
