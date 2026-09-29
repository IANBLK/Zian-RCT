package com.ianblk.zianrct.client;

import com.ianblk.zianrct.network.MedalClientSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public final class ClientMedalState {
    private static final AtomicReference<MedalClientSnapshot> SNAPSHOT = new AtomicReference<>();
    private static final ConcurrentLinkedQueue<String> NOTIFICATIONS = new ConcurrentLinkedQueue<>();

    private ClientMedalState() {
    }

    public static void apply(MedalClientSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        SNAPSHOT.set(snapshot);
        NOTIFICATIONS.addAll(snapshot.notifications());
    }

    public static Optional<MedalClientSnapshot> current() {
        return Optional.ofNullable(SNAPSHOT.get());
    }

    public static List<String> drainNotifications() {
        List<String> result = new ArrayList<>();
        String medalId;
        while ((medalId = NOTIFICATIONS.poll()) != null) {
            result.add(medalId);
        }
        return List.copyOf(result);
    }

    public static void clear() {
        SNAPSHOT.set(null);
        NOTIFICATIONS.clear();
    }
}
