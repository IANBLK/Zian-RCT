package com.ianblk.zianrct.reward;
import com.ianblk.zianrct.storage.StorageFiles;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Owner-local durable journals; all receipts retained, at most cacheLimit owners in memory. */
final class ShardedRewardJournal extends RewardJournal {
  private final Path manifest, directory;
  private final int limit;
  private final LinkedHashMap<UUID, RewardJournal> cache = new LinkedHashMap<>(16, .75f, true);
  private final Map<UUID, Integer> pins = new HashMap<>();

  ShardedRewardJournal(Path path, JsonObject model) throws IOException {
    super(path);
    if (!model.keySet().equals(Set.of("schemaVersion", "layout", "directory", "cacheLimit"))
        || !model.get("layout").getAsString().equals("per-player-v1"))
      throw new IOException("Invalid sharded rewards manifest");
    manifest = path.toAbsolutePath().normalize();
    String name = model.get("directory").getAsString(), prefix = path.getFileName() + ".players-";
    if (!name.startsWith(prefix)
        || !UUID.fromString(name.substring(prefix.length()))
            .toString()
            .equals(name.substring(prefix.length())))
      throw new IOException("Invalid rewards directory");
    directory = manifest.getParent().resolve(name);
    limit = model.get("cacheLimit").getAsBigDecimal().intValueExact();
    if (limit < 1 || limit > 64 || !StorageFiles.isDirectory(directory) || StorageFiles.isSymbolicLink(directory))
      throw new IOException("Invalid rewards cache/directory");
  }

  private RewardJournal owner(UUID player) {
    try {
      ensureHealthy();
      var existing = cache.get(player);
      if (existing != null) return existing;
      Path file = directory.resolve(player + ".json");
      if (StorageFiles.isSymbolicLink(file)) throw new IOException("Symlink reward shard refused");
      if (StorageFiles.exists(file)) {
        var json = RewardFiles.read(file, 16777216).getAsJsonObject();
        int version = json.get("schemaVersion").getAsBigDecimal().intValueExact();
        if (version != 1 && version != 2)
          throw new IOException("Unsupported owner journal version");
      }
      var loaded = RewardJournal.open(file);
      if (loaded.allForMaintenance().stream().anyMatch(c -> !player.equals(c.player())))
        throw new IOException("Reward shard belongs to another player");
      cache.put(player, loaded);
      trim();
      return loaded;
    } catch (IOException failure) {
      throw new UncheckedIOException(
          "Cannot load player reward journal; original preserved", failure);
    }
  }

  private void trim() {
    var iterator = cache.entrySet().iterator();
    while (cache.size() > limit && iterator.hasNext()) {
      var entry = iterator.next();
      if (!pins.containsKey(entry.getKey())) iterator.remove();
    }
  }

  private RewardJournal loaded(UUID id) {
    for (var value : cache.values()) if (value.get(id) != null) return value;
    throw new IllegalStateException("Load owning player before changing a reward");
  }

  @Override
  public synchronized OwnerLease usePlayer(UUID player) {
    pins.merge(player, 1, Integer::sum);
    try {
      owner(player);
    } catch (RuntimeException error) {
      pins.computeIfPresent(player, (p, n) -> n == 1 ? null : n - 1);
      throw error;
    }
    return () -> {
      synchronized (this) {
        pins.computeIfPresent(player, (p, n) -> n == 1 ? null : n - 1);
        trim();
      }
    };
  }

  @Override
  synchronized void ensureHealthy() throws IOException {
    if (StorageFiles.exists(manifest.resolveSibling(manifest.getFileName() + ".maintenance")))
      throw new IOException("Reward storage maintenance in progress");
  }

  @Override
  public synchronized RewardClaim get(UUID player, UUID id) {
    return owner(player).get(player, id);
  }

  @Override
  public synchronized RewardClaim get(UUID id) {
    for (var value : cache.values()) {
      var claim = value.get(id);
      if (claim != null) return claim;
    }
    return null;
  }

  @Override
  public synchronized List<RewardClaim> forPlayer(UUID player) {
    return owner(player).forPlayer(player);
  }

  @Override
  public synchronized RewardClaim latest(UUID player, String trainer) {
    return owner(player).latest(player, trainer);
  }

  @Override
  public synchronized long remaining(
      UUID player, String trainer, RewardDefinition definition, long now) {
    return owner(player).remaining(player, trainer, definition, now);
  }

  @Override
  public synchronized boolean reserve(UUID player, String trainer, RewardDefinition reward)
      throws IOException {
    return owner(player).reserve(player, trainer, reward);
  }

  @Override
  public synchronized boolean reserveAt(
      UUID player, String trainer, RewardDefinition reward, long now, UUID battle)
      throws IOException {
    return owner(player).reserveAt(player, trainer, reward, now, battle);
  }

  @Override
  public synchronized void phase(UUID id, int index, RewardClaim.Phase phase, String detail)
      throws IOException {
    ensureHealthy();
    loaded(id).phase(id, index, phase, detail);
  }

  @Override
  public synchronized void resolve(
      UUID player, UUID id, int index, UUID admin, String decision, String evidence)
      throws IOException {
    ensureHealthy();
    owner(player).resolve(player, id, index, admin, decision, evidence);
  }

  @Override
  synchronized List<RewardClaim> allForMaintenance() {
    throw new IllegalStateException("Sharded journals must be inspected per player");
  }
}
