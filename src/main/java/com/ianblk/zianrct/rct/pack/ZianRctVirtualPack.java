package com.ianblk.zianrct.rct.pack;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class ZianRctVirtualPack implements PackResources {
    public static final String PACK_ID = "zianrct/generated_rct";
    public static final String RCT_NAMESPACE = "rctmod";

    private final AtomicReference<Map<ResourceLocation, byte[]>> resources =
            new AtomicReference<>(Map.of());
    private final PackMetadataSection metadata = new PackMetadataSection(
            Component.literal("Zian RCT generated progression"),
            SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA)
    );
    private final PackLocationInfo location = new PackLocationInfo(
            PACK_ID,
            Component.literal("Zian RCT generated progression"),
            PackSource.BUILT_IN,
            Optional.empty()
    );

    public void replaceResources(Map<String, byte[]> nextResources) {
        LinkedHashMap<ResourceLocation, byte[]> copy = new LinkedHashMap<>();
        nextResources.forEach((path, value) -> copy.put(
                ResourceLocation.fromNamespaceAndPath(RCT_NAMESPACE, path),
                value.clone()
        ));
        resources.set(java.util.Collections.unmodifiableMap(copy));
    }

    public void clear() {
        resources.set(Map.of());
    }

    public int resourceCount() {
        return resources.get().size();
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String @NotNull ... elements) {
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(
            @NotNull PackType packType,
            @NotNull ResourceLocation location
    ) {
        if (packType != PackType.SERVER_DATA) {
            return null;
        }
        byte[] bytes = resources.get().get(location);
        if (bytes == null) {
            return null;
        }
        return () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(
            @NotNull PackType packType,
            @NotNull String namespace,
            @NotNull String path,
            @NotNull ResourceOutput output
    ) {
        if (packType != PackType.SERVER_DATA || !RCT_NAMESPACE.equals(namespace)) {
            return;
        }
        String prefix = path.isEmpty() ? "" : path + "/";
        resources.get().forEach((location, bytes) -> {
            if (location.getNamespace().equals(namespace) && location.getPath().startsWith(prefix)) {
                output.accept(location, () -> new ByteArrayInputStream(bytes));
            }
        });
    }

    @Override
    public @NotNull Set<String> getNamespaces(PackType packType) {
        if (packType != PackType.SERVER_DATA) {
            return Set.of();
        }
        LinkedHashSet<String> namespaces = new LinkedHashSet<>();
        namespaces.add(RCT_NAMESPACE);
        resources.get().keySet().forEach(location -> namespaces.add(location.getNamespace()));
        return java.util.Collections.unmodifiableSet(namespaces);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T getMetadataSection(@NotNull MetadataSectionSerializer<T> serializer)
            throws IOException {
        return serializer == PackMetadataSection.TYPE ? (T) metadata : null;
    }

    @Override
    public @NotNull PackLocationInfo location() {
        return location;
    }

    @Override
    public @NotNull String packId() {
        return PACK_ID;
    }

    @Override
    public void close() {
    }
}
