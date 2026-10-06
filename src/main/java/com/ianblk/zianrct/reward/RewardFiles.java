package com.ianblk.zianrct.reward;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

final class RewardFiles {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private RewardFiles() {}
    static JsonElement read(Path file, long limit) throws IOException {
        if (Files.size(file) > limit) throw new IOException("Archivo de recompensas demasiado grande: " + file);
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (RuntimeException error) { throw new IOException("JSON inválido: " + file, error); }
    }
    static void write(Path file, Object data) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp = Files.createTempFile(file.toAbsolutePath().getParent(), "zianrct-reward-", ".tmp");
        try {
            Files.writeString(temp, GSON.toJson(data), StandardCharsets.UTF_8);
            try (var channel = FileChannel.open(temp, StandardOpenOption.WRITE)) { channel.force(true); }
            // Refuse a non-atomic replacement; no external mutation follows a failed journal write.
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
