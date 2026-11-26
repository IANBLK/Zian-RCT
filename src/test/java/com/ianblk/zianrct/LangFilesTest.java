package com.ianblk.zianrct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class LangFilesTest {
    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+\\$)?[-#+0,(]*\\d*(\\.\\d+)?[sdfxXeEgG]");
    private static final String STRING = "\"((?:[^\"\\\\]|\\\\.)*)\"";
    /** Claves cuyo respaldo en el código es inglés (texto anterior) y cuyo es_es lleva la traducción. */
    private static final Set<String> ENGLISH_FALLBACK = Set.of("zianrct.storage.unavailable", "zianrct.storage.config_failed");

    private static Map<String, String> load(String name) throws Exception {
        var json = JsonParser.parseString(new String(
            LangFilesTest.class.getResourceAsStream("/assets/zianrct/lang/" + name).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        var map = new TreeMap<String, String>();
        json.entrySet().forEach(e -> map.put(e.getKey(), e.getValue().getAsString()));
        return map;
    }
    private static String placeholders(String text) {
        var found = new java.util.ArrayList<String>();
        var m = PLACEHOLDER.matcher(text);
        while (m.find()) found.add(m.group());
        java.util.Collections.sort(found);
        return String.join(",", found);
    }

    @Test void allLanguagesHaveSameKeysAndPlaceholders() throws Exception {
        var es = load("es_es.json");
        for (var other : new String[] {"en_us.json", "es_mx.json"}) {
            var map = load(other);
            assertEquals(es.keySet(), map.keySet(), "Claves distintas en " + other);
            es.forEach((key, text) -> assertEquals(placeholders(text), placeholders(map.get(key)), key + " en " + other));
        }
    }

    @Test void mexicanSpanishMatchesSpain() throws Exception {
        assertEquals(load("es_es.json"), load("es_mx.json"));
    }

    @Test void englishIsActuallyTranslated() throws Exception {
        var es = load("es_es.json");
        var en = load("en_us.json");
        for (var key : es.keySet()) {
            if (!key.startsWith("dialog.zianrct.") && !key.startsWith("ui.zianrct.")) continue;
            var spanish = es.get(key);
            if (spanish.equals("Zian RCT")) continue;
            assertTrue(!spanish.equals(en.get(key)), key + " sigue en español en en_us");
        }
    }

    /** Todo texto con clave en el código (translatableWithFallback, t, Msg) tiene su clave en es_es con el mismo español de respaldo. */
    @Test void codeFallbacksMatchSpanishLang() throws Exception {
        var es = load("es_es.json");
        var call = Pattern.compile("(?:translatableWithFallback\\(|\\bt\\(|new Msg\\(|LocalizedException\\.(?:State|Argument)\\(|\\bnote\\()\\s*\"(zianrct\\.[a-z0-9_.]+|ui\\.zianrct\\.[0-9a-f]{12}|screen\\.zianrct\\.[a-z0-9_.]+|key\\.zianrct\\.[a-z0-9_.]+)\",\\s*" + STRING + "(?=\\s*[,)])");
        int checked = 0;
        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                var m = call.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) {
                    var key = m.group(1);
                    assertTrue(es.containsKey(key), file.getFileName() + ": falta " + key);
                    var text = unescape(m.group(2));
                    if (!ENGLISH_FALLBACK.contains(key)) assertEquals(text, es.get(key), file.getFileName() + ": el español de " + key + " no coincide con el código");
                    checked++;
                }
            }
        }
        assertTrue(checked > 100, "Se esperaban más claves en el código: " + checked);
    }
    private static String unescape(String java) { return java.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\"); }
}
