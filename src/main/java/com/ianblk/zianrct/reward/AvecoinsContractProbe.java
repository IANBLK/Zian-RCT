package com.ianblk.zianrct.reward;

import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Read-only compatibility check; it never reads or changes a player wallet. */
public final class AvecoinsContractProbe {
    private static final Set<String> SUPPORTED_VERSIONS = Set.of("2.3", "2.4");
    private AvecoinsContractProbe() {
    }

    public static ProbeResult inspect() {
        try {
            var mod = ModList.get().getModContainerById("avecoins");
            if (mod.isEmpty()) {
                return new ProbeResult(false, "AVECOINS no está instalado", List.of());
            }
            String version = mod.get().getModInfo().getVersion().toString();
            if (!supportedVersion(version)) {
                return new ProbeResult(false, "AVECOINS " + version + "; se requiere 2.3 o 2.4", List.of());
            }

            Class<?> crafting = Class.forName("net.sundggs.avecoins.config.CraftingConfig");
            Class<?> store = Class.forName("net.sundggs.avecoins.shop.WalletStore");
            Class<?> data = Class.forName("net.sundggs.avecoins.shop.WalletData");
            ProbeResult layout = inspectLayout(crafting, store, data);
            return layout.compatible()
                ? new ProbeResult(true, "AVECOINS " + version + " compatible", layout.currencies())
                : layout;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            return new ProbeResult(
                false,
                "contrato AVECOINS incompatible: " + error.getClass().getSimpleName(),
                List.of()
            );
        }
    }

    static boolean supportedVersion(String version) {
        return SUPPORTED_VERSIONS.contains(version);
    }

    static ProbeResult inspectLayout(Class<?> crafting, Class<?> store, Class<?> data)
        throws ReflectiveOperationException {
            Field managedField = crafting.getField("MANAGED_RESULTS");
            if (!Modifier.isStatic(managedField.getModifiers())) {
                return incompatible("MANAGED_RESULTS no es estático");
            }
            Object managedValue = managedField.get(null);
            if (!(managedValue instanceof Set<?> managed)
                || managed.isEmpty()
                || managed.stream().anyMatch(value -> !(value instanceof String text) || text.isBlank())) {
                return incompatible("MANAGED_RESULTS incompatible");
            }
            List<String> currencies = managed.stream().map(String.class::cast).sorted().toList();

            var get = store.getMethod("get");
            var save = store.getMethod("save", data);
            var copy = data.getMethod("copy");
            var balance = data.getMethod("balance", UUID.class, String.class);
            var balances = data.getMethod("balances", UUID.class);
            data.getMethod("credit", UUID.class, String.class, long.class);
            var debit = data.getMethod("debit", UUID.class, String.class, long.class);
            if (!Modifier.isStatic(get.getModifiers())
                || !data.isAssignableFrom(get.getReturnType())
                || !Modifier.isStatic(save.getModifiers())
                || !booleanType(save.getReturnType())
                || !data.isAssignableFrom(copy.getReturnType())
                || !longType(balance.getReturnType())
                || !Map.class.isAssignableFrom(balances.getReturnType())
                || !booleanType(debit.getReturnType())) {
                return incompatible("firmas de cartera incompatibles");
            }
            Field slotsField = data.getField("SLOT_COUNT");
            Field stackSizeField = data.getField("STACK_SIZE");
            if (!Modifier.isStatic(slotsField.getModifiers())
                || !Modifier.isStatic(stackSizeField.getModifiers())) {
                return incompatible("estructura de cartera incompatible");
            }
            int slots = slotsField.getInt(null);
            int stackSize = stackSizeField.getInt(null);
            if (slots != 27 || stackSize != 64) {
                return incompatible("estructura de cartera incompatible");
            }
            return new ProbeResult(true, "Contrato AVECOINS compatible", currencies);
    }

    private static boolean booleanType(Class<?> type) {
        return type == boolean.class || type == Boolean.class;
    }

    private static boolean longType(Class<?> type) {
        return type == long.class || type == Long.class;
    }

    private static ProbeResult incompatible(String detail) {
        return new ProbeResult(false, detail, List.of());
    }

    public record ProbeResult(boolean compatible, String detail, List<String> currencies) {
        public ProbeResult {
            currencies = List.copyOf(currencies);
        }
    }
}

