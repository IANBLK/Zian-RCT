package com.ianblk.zianrct.client;

import com.ianblk.zianrct.network.MedalClientSnapshot;
import com.ianblk.zianrct.network.MedalProtocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class ClientMedalState {
    private static final AtomicReference<MedalClientSnapshot> SNAPSHOT = new AtomicReference<>();
    private static final ConcurrentLinkedQueue<String> AWARDS = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean OPEN_REQUESTED = new AtomicBoolean(false);

    private ClientMedalState() {
    }

    public static void apply(MedalClientSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        MedalProtocol.validateSnapshot(snapshot);
        SNAPSHOT.set(snapshot);
    }

    public static void notifyAward(String medalId) {
        MedalProtocol.validateMedalId(medalId);
        AWARDS.add(medalId);
    }

    public static void requestOpen() {
        OPEN_REQUESTED.set(true);
    }

    public static boolean consumeOpenRequest() {
        return OPEN_REQUESTED.getAndSet(false);
    }

    public static Optional<MedalClientSnapshot> current() {
        return Optional.ofNullable(SNAPSHOT.get());
    }

    public static List<String> drainAwards() {
        List<String> result = new ArrayList<>();
        String medalId;
        while ((medalId = AWARDS.poll()) != null) {
            result.add(medalId);
        }
        return List.copyOf(result);
    }

    public static void clear() {
        SNAPSHOT.set(null);
        AWARDS.clear();
        OPEN_REQUESTED.set(false);
    }
}
