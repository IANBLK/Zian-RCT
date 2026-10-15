package com.ianblk.zianrct.reward;
import com.ianblk.zianrct.storage.StorageFiles;

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
        if (StorageFiles.size(file) > limit) throw new IOException("Archivo de recompensas demasiado grande: " + file);
        try (var reader = StorageFiles.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (RuntimeException error) { throw new IOException("JSON inválido: " + file, error); }
    }
    static void write(Path file, Object data) throws IOException {
        writeEncoded(file,GSON.toJson(data));
    }
    static void writeEncoded(Path file,String encoded) throws IOException {
        StorageFiles.createDirectories(file.toAbsolutePath().getParent());
        Path temp = StorageFiles.createTempFile(file.toAbsolutePath().getParent(), "zianrct-reward-", ".tmp");
        try {
            StorageFiles.writeString(temp, encoded, StandardCharsets.UTF_8);
            try (var channel = StorageFiles.channel(temp, StandardOpenOption.WRITE)) { channel.force(true); }
            // Refuse a non-atomic replacement; no external mutation follows a failed journal write.
            StorageFiles.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            if(!System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("windows"))try(var directory=StorageFiles.channel(file.toAbsolutePath().getParent(),StandardOpenOption.READ)){directory.force(true);}
        } finally { StorageFiles.deleteIfExists(temp); }
    }
}
