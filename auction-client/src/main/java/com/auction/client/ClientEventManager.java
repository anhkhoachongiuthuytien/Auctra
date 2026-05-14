package com.auction.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý các callback để cập nhật giao diện (JavaFX) khi có sự kiện Real-time từ Server.
 */
public class ClientEventManager {
    private static final List<Runnable> listeners = new ArrayList<>();

    public static void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public static void clearListeners() {
        listeners.clear();
    }

    public static void fireUpdate() {
        for (Runnable r : listeners) {
            if (r != null) {
                r.run();
            }
        }
    }
}
