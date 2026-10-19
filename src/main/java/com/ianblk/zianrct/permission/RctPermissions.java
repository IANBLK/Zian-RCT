package com.ianblk.zianrct.permission;

import net.minecraft.commands.CommandSourceStack;
import java.util.function.Supplier;

public final class RctPermissions {
    private RctPermissions() {}

    public static boolean allows(CommandSourceStack source, String action, boolean administrative) {
        if (source.getPlayer() == null) return source.hasPermission(2);
        return evaluate(action, administrative, administrative ? source.hasPermission(2) : true, () -> {
            try {
                return LuckPermsLookup.lookup(source.getPlayer(), "zianrct." + action);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    static boolean evaluate(String action, boolean administrative, boolean fallback,
                            Supplier<LuckPermsLookup.Decision> lookup) {
        // Battle participation is public; ownership and progression are checked by the caller.
        if (!administrative && ("battle".equals(action) || "legendary.claim".equals(action))) return true;
        return evaluate(fallback, lookup);
    }

    static boolean evaluate(boolean fallback, Supplier<LuckPermsLookup.Decision> lookup) {
        try {
            return switch (lookup.get()) {
                case TRUE -> true;
                case FALSE -> false;
                case ABSENT, UNDEFINED -> fallback;
            };
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }
}
