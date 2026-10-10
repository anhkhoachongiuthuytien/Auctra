package com.auction.server;

import com.auction.protocol.AuctionEvent;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Enqueue only: a slow subscriber never holds up bids or other subscribers. */
public final class BroadcastManager {
    private static final Set<Subscription> CLIENTS = ConcurrentHashMap.newKeySet();

    private BroadcastManager() { }

    public static Subscription subscribe(Socket socket) {
        Subscription subscription = new Subscription(socket);
        CLIENTS.add(subscription);
        return subscription;
    }

    public static void broadcast(AuctionEvent event) {
        for (Subscription subscription : CLIENTS) {
            if (!subscription.events.offer(event)) { subscription.close(); }
        }
    }

    public static final class Subscription implements AutoCloseable {
        private final Socket socket;
        private final BlockingQueue<AuctionEvent> events = new ArrayBlockingQueue<>(64);

        private Subscription(Socket socket) { this.socket = socket; }
        public AuctionEvent poll() throws InterruptedException { return events.poll(1, TimeUnit.SECONDS); }

        @Override
        public void close() {
            CLIENTS.remove(this);
            try { socket.close(); } catch (IOException ignored) { }
        }
    }
}
