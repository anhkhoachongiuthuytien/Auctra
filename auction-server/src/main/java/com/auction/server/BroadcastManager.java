package com.auction.server;

import com.auction.protocol.AuctionEvent;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BroadcastManager {
    // Thread-safe list of active client streams that subscribed to updates
    private static final List<ObjectOutputStream> clients = Collections.synchronizedList(new ArrayList<>());

    public static void addClient(ObjectOutputStream out) {
        clients.add(out);
        System.out.println("[BroadcastManager] Đã thêm client mới. Tổng số client theo dõi: " + clients.size());
    }

    public static void removeClient(ObjectOutputStream out) {
        clients.remove(out);
        System.out.println("[BroadcastManager] Đã xóa client. Tổng số client theo dõi: " + clients.size());
    }

    /**
     * Gửi sự kiện tới tất cả client đang kết nối.
     * Đồng bộ hóa để đảm bảo an toàn luồng khi duyệt.
     */
    public static void broadcast(AuctionEvent event) {
        synchronized (clients) {
            List<ObjectOutputStream> deadClients = new ArrayList<>();
            for (ObjectOutputStream out : clients) {
                try {
                    out.writeObject(event);
                    out.flush();
                    out.reset();
                } catch (Exception e) {
                    deadClients.add(out);
                }
            }
            clients.removeAll(deadClients);
        }
    }
}
