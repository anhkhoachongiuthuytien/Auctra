package com.auction.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Quản lý các callback để cập nhật giao diện (JavaFX) khi có sự kiện Real-time từ Server.
 */
public class ClientEventManager {
    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public static void addListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public static void clearListeners() {
        listeners.clear();
    }

    public static void fireUpdate() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (RuntimeException ignored) {
                // One stale screen listener should not block realtime refreshes.
            }
        }
    }
}
