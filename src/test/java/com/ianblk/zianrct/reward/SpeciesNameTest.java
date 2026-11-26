package com.ianblk.zianrct.reward;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SpeciesNameTest {
    private static String lang(String file, String key) throws Exception {
        var json = JsonParser.parseString(new String(SpeciesNameTest.class.getResourceAsStream("/assets/zianrct/lang/" + file).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        return json.get(key).getAsString();
    }
    @Test void shinyAdjectiveFollowsTheLanguage() throws Exception {
        var msg = RewardMessages.species("Mewtwo", true);
        assertEquals("Mewtwo shiny", msg.plain());
        assertEquals("Mewtwo shiny", String.format(lang("es_es.json", msg.key()), "Mewtwo"));
        assertEquals("Shiny Mewtwo", String.format(lang("en_us.json", msg.key()), "Mewtwo"));
        assertEquals("Mewtwo", RewardMessages.species("Mewtwo", false).plain());
    }
}
