package com.ianblk.zianrct;

/** Errores para el jugador con clave de idioma; getMessage() sigue siendo el español de siempre (logs y pruebas no cambian). */
public final class LocalizedException {
    private LocalizedException() {}

    public interface Info { Msg msg(); }

    public static final class State extends IllegalStateException implements Info {
        private final Msg msg;
        public State(String key, String es, Object... args) { super(new Msg(key, es, args).plain()); msg = new Msg(key, es, args); }
        @Override public Msg msg() { return msg; }
    }

    public static final class Argument extends IllegalArgumentException implements Info {
        private final Msg msg;
        public Argument(String key, String es, Object... args) { super(new Msg(key, es, args).plain()); msg = new Msg(key, es, args); }
        @Override public Msg msg() { return msg; }
    }
}
