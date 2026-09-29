package com.ianblk.zianrct.rct.pack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;

public record ZianRctPackSource(ZianRctVirtualPack pack) implements RepositorySource {
    @Override
    public void loadPacks(@NotNull Consumer<Pack> consumer) {
        PackLocationInfo info = new PackLocationInfo(
                ZianRctVirtualPack.PACK_ID,
                Component.literal("Zian RCT generated progression"),
                PackSource.BUILT_IN,
                Optional.empty()
        );
        PackSelectionConfig selection = new PackSelectionConfig(true, Pack.Position.TOP, true);
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public @NotNull PackResources openPrimary(@NotNull PackLocationInfo ignored) {
                return pack;
            }

            @Override
            public @NotNull PackResources openFull(
                    @NotNull PackLocationInfo ignored,
                    @NotNull Pack.Metadata metadata
            ) {
                return pack;
            }
        };
        consumer.accept(Pack.readMetaAndCreate(info, supplier, PackType.SERVER_DATA, selection));
    }
}
