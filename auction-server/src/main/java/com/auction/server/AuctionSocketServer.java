package com.auction.server;

import com.auction.protocol.AuctionEvent;
import com.auction.protocol.AuctionRequest;
import com.auction.protocol.AuctionResponse;
import com.auction.protocol.ProtocolFilter;
import com.auction.protocol.RequestType;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AuctionSocketServer {
    private final int port;
    private final AuctionRequestHandler handler;
    private final Set<Socket> clients = ConcurrentHashMap.newKeySet();
    // Persistent sockets need one worker each; no unbounded queue of unhandled sockets.
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(0, 128, 30, TimeUnit.SECONDS,
            new SynchronousQueue<>(), runnable -> {
                Thread thread = new Thread(runnable, "auction-connection");
                thread.setDaemon(true);
                return thread;
            });
    private final ScheduledExecutorService writeTimeouts = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "auction-write-timeouts");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean running;
    private volatile ServerSocket serverSocket;

    public AuctionSocketServer(AuctionServerFacade facade) { this(facade, 9999); }
    public AuctionSocketServer(AuctionServerFacade facade, int port) {
        this.port = port;
        this.handler = new AuctionRequestHandler(facade);
    }

    public int getPort() { return serverSocket == null ? -1 : serverSocket.getLocalPort(); }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(5000);
                clients.add(socket);
                try { workers.execute(() -> handleClient(socket)); }
                catch (RejectedExecutionException exception) { clients.remove(socket); close(socket); }
            } catch (SocketException exception) {
                if (running) { throw exception; }
            }
        }
    }

    public void stop() {
        running = false;
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException ignored) { }
        }
        clients.forEach(AuctionSocketServer::close);
        workers.shutdownNow();
        writeTimeouts.shutdownNow();
    }

    private void handleClient(Socket socket) {
        try (socket;
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.flush();
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                in.setObjectInputFilter(ProtocolFilter.create());
                while (running && !socket.isClosed()) {
                    Object value = in.readObject();
                    if (!(value instanceof AuctionRequest request)) { break; }
                    AuctionResponse response = handler.process(request);
                    if (request.getType() == RequestType.SUBSCRIBE_UPDATES && response.isSuccess()) {
                        try (BroadcastManager.Subscription subscription = BroadcastManager.subscribe(socket)) {
                            write(socket, out, response);
                            while (running && !socket.isClosed()) {
                                handler.requireUser(request.get("sessionToken"));
                                AuctionEvent event = subscription.poll();
                                if (event != null) { write(socket, out, event); }
                            }
                        }
                        break;
                    }
                    write(socket, out, response);
                    socket.setSoTimeout(60000);
                }
            }
        } catch (EOFException | SocketException ignored) {
            // Normal disconnect, idle timeout or shutdown.
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException | ClassNotFoundException | RuntimeException exception) {
            System.err.println("[Server] Connection closed: " + exception.getClass().getSimpleName());
        } finally {
            clients.remove(socket);
            close(socket);
        }
    }

    private void write(Socket socket, ObjectOutputStream out, Object value) throws IOException {
        ScheduledFuture<?> timeout = writeTimeouts.schedule(() -> close(socket), 10, TimeUnit.SECONDS);
        try {
            out.writeObject(value);
            out.flush();
            out.reset();
        } finally { timeout.cancel(false); }
    }

    private static void close(Socket socket) {
        try { socket.close(); } catch (IOException ignored) { }
    }
}
