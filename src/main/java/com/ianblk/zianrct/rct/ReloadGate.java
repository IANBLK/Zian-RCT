package com.ianblk.zianrct.rct;

import com.ianblk.zianrct.LocalizedException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Recarga de todo o nada y de una en una: stage valida sin tocar el estado vivo; commit solo se ejecuta si stage terminó bien. Solo hilo del servidor. */
public final class ReloadGate {
    private boolean running;

    public <T> void run(Supplier<T> stage, Consumer<T> commit) {
        if (running) throw new LocalizedException.State("zianrct.err.reload_running", "Ya hay una recarga de entrenadores en curso.");
        running = true;
        try { commit.accept(stage.get()); } finally { running = false; }
    }

    public boolean isRunning() { return running; }
}
