package com.ianblk.zianrct.reward;

import com.google.gson.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Explicit offline migration; complete backups and every legacy receipt are retained. */
public final class RewardStorageMaintenance {
  enum Point {
    SHARDS_PREPARED,
    MANIFEST_COMMITTED
  }

  @FunctionalInterface
  interface Fault {
    void at(Point point) throws IOException;
  }

  public record Result(int players, int claims, Path directory, Path archive) {}

  public static Result migrateOffline(Path path) throws IOException {
    return migrateOffline(path, p -> {});
  }

  static Result migrateOffline(Path path, Fault fault) throws IOException {
    path = path.toAbsolutePath().normalize();
    if (Files.isSymbolicLink(path)) throw new IOException("Symlink ledger refused");
    boolean existed = Files.exists(path);
    if (existed && Files.size(path) > 16777216) throw new IOException("Oversized rewards ledger; original preserved");
    byte[] before = existed ? Files.readAllBytes(path) : null;
    var original = RewardJournal.open(path);
    var claims = original.allForMaintenance(); // refuses migration of an already-sharded ledger
    if (existed && !Arrays.equals(before, Files.readAllBytes(path)))
      throw new IOException("Ledger changed during validation");
    var model =
        existed
            ? JsonParser.parseString(new String(before, java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonObject()
            : new JsonObject();
    if (!existed) {
      model.addProperty("schemaVersion", 2);
      model.add("claims", new JsonArray());
    }
    Map<UUID, JsonArray> groups = new LinkedHashMap<>();
    for (var entry : model.getAsJsonArray("claims")) {
      UUID owner = UUID.fromString(entry.getAsJsonObject().get("player").getAsString());
      groups.computeIfAbsent(owner, p -> new JsonArray()).add(entry.deepCopy());
    }
    Path parent = path.getParent();
    Files.createDirectories(parent);
    String token = UUID.randomUUID().toString();
    String name = path.getFileName() + ".players-" + token;
    Path stage = parent.resolve(name + ".staging"), directory = parent.resolve(name);
    Path archive = existed ? parent.resolve(path.getFileName() + ".archive-" + token) : null;
    Path marker = path.resolveSibling(path.getFileName() + ".maintenance");
    boolean ownsMarker = false;
    try {
      Files.writeString(
          marker, "Offline rewards migration; directory=" + name, StandardOpenOption.CREATE_NEW);
      ownsMarker = true;
      forceFile(marker);
      forceDirectory(parent);
      Files.createDirectory(stage);
      for (var entry : groups.entrySet()) {
        var shard = new JsonObject();
        shard.addProperty("schemaVersion", 2);
        shard.add("claims", entry.getValue());
        Path target = stage.resolve(entry.getKey() + ".json");
        RewardFiles.write(target, shard);
        var verified = RewardJournal.open(target);
        if (!verified.allForMaintenance().equals(original.forPlayer(entry.getKey())))
          throw new IOException("Owner receipt verification failed");
      }
      forceDirectory(stage);
      if (existed) {
        if (!Arrays.equals(before, Files.readAllBytes(path)))
          throw new IOException("Ledger changed; server must be offline");
        Files.write(archive, before, StandardOpenOption.CREATE_NEW);
        forceFile(archive);
        if (!Arrays.equals(before, Files.readAllBytes(archive)))
          throw new IOException("Backup verification failed");
      } else if (Files.exists(path))
        throw new IOException("Ledger appeared; server must be offline");
      forceDirectory(parent);
      fault.at(Point.SHARDS_PREPARED);
      Files.move(stage, directory, StandardCopyOption.ATOMIC_MOVE);
      forceDirectory(parent);
      var manifest = new JsonObject();
      manifest.addProperty("schemaVersion", 3);
      manifest.addProperty("layout", "per-player-v1");
      manifest.addProperty("directory", name);
      manifest.addProperty("cacheLimit", 32);
      RewardFiles.write(path, manifest);
      fault.at(Point.MANIFEST_COMMITTED);
    } finally {
      // Interrupted staging directories/backups are evidence, never deleted automatically.
      if (ownsMarker) {
        Files.deleteIfExists(marker);
        forceDirectory(parent);
      }
    }
    return new Result(groups.size(), claims.size(), directory, archive);
  }

  private static void forceFile(Path path) throws IOException {
    try (var file = FileChannel.open(path, StandardOpenOption.WRITE)) {
      file.force(true);
    }
  }

  private static void forceDirectory(Path path) throws IOException {
    if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"))
      try (var directory = FileChannel.open(path, StandardOpenOption.READ)) {
        directory.force(true);
      }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 2 || !args[0].equals("migrate-offline"))
      throw new IllegalArgumentException(
          "Usage: migrate-offline <rewards.json>; stop server and disable restart first");
    var result = migrateOffline(Path.of(args[1]));
    var json = new JsonObject();
    json.addProperty("players", result.players());
    json.addProperty("claims", result.claims());
    json.addProperty("directory", result.directory().toString());
    json.addProperty("archive", result.archive() == null ? "" : result.archive().toString());
    System.out.println(json);
  }
}
