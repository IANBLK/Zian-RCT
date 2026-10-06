package com.ianblk.zianrct.permission;

import net.minecraft.commands.CommandSourceStack;
import java.util.function.Supplier;

public final class RctPermissions {
    private RctPermissions() {}

    public static boolean allows(CommandSourceStack source, String action, boolean administrative) {
        if (source.getPlayer() == null) return source.hasPermission(2);
        return evaluate(administrative ? source.hasPermission(2) : true, () -> {
            try {
                return LuckPermsLookup.lookup(source.getPlayer(), "zianrct." + action);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        });
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
