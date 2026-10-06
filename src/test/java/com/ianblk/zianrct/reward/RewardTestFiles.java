package com.ianblk.zianrct.reward;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

/** Bounded cleanup retry for Windows' delayed unlink of atomically replaced test files. */
public final class RewardTestFiles {
    private RewardTestFiles() {}
    public static void cleanup(Path directory) throws IOException, InterruptedException {
        Path root = directory.toAbsolutePath().normalize();
        IOException failure = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                if (!Files.exists(root)) return;
                java.util.List<Path> paths;
                try (var stream = Files.walk(root)) { paths = stream.sorted(Comparator.reverseOrder()).toList(); }
                for (Path path : paths) {
                    if (!path.toAbsolutePath().normalize().startsWith(root)) throw new IOException("Unexpected test path");
                    Files.deleteIfExists(path);
                }
                return;
            } catch (IOException error) { failure = error; Thread.sleep(50); }
        }
        throw failure;
    }
}
