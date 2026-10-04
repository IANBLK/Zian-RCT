package com.ianblk.zianrct.permission;

import com.ianblk.zianrct.ZianRCT;
import java.util.List;

/** Bukkit's wrapper allows dispatch; Brigadier children enforce each actual permission. */
public final class YouerPermissionBridge {
    private YouerPermissionBridge() {}
    public static void register() {
        try {
            Object manager = Class.forName("org.bukkit.Bukkit").getMethod("getPluginManager").invoke(null);
            Class<?> managers = Class.forName("org.bukkit.plugin.PluginManager");
            Class<?> permissions = Class.forName("org.bukkit.permissions.Permission");
            Class<?> defaults = Class.forName("org.bukkit.permissions.PermissionDefault");
            for (String command : List.of("zianrct", "medals", "zianrctbattle")) {
                String node = "minecraft.command." + command;
                if (managers.getMethod("getPermission", String.class).invoke(manager, node) != null) continue;
                Object permission = permissions.getConstructor(String.class, String.class, defaults)
                        .newInstance(node, "Allows dispatch to Zian RCT permission checks", defaults.getField("TRUE").get(null));
                managers.getMethod("addPermission", permissions).invoke(manager, permission);
            }
        } catch (ClassNotFoundException ignored) {
            // Pure NeoForge.
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            ZianRCT.LOGGER.warn("Could not register Zian RCT Youer command permissions", exception);
        }
    }
}
