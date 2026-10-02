package com.ianblk.zianrct.config;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultMedalResourcesTest {
    @Test
    void everyDefaultMedalHasAValidEmbeddedPng() throws Exception {
        for (ZianRctConfig.MedalDefinition medal : ZianRctConfig.defaults().activeProfileConfig().medals()) {
            String prefix = "zianrct:textures/";
            assertTrue(medal.texture().startsWith(prefix), "Textura inesperada para " + medal.id());
            String classpathPath = "/assets/zianrct/textures/" + medal.texture().substring(prefix.length());
            try (InputStream stream = DefaultMedalResourcesTest.class.getResourceAsStream(classpathPath)) {
                assertNotNull(stream, "Falta la textura de " + medal.id() + ": " + classpathPath);
                var image = ImageIO.read(stream);
                assertNotNull(image, "PNG inválido para " + medal.id());
                assertEquals(38, image.getWidth(), "Ancho inválido para " + medal.id());
                assertEquals(38, image.getHeight(), "Alto inválido para " + medal.id());
            }
        }
    }
}
