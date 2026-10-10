package com.auction.client;

import com.auction.exception.CommunicationException;
import com.auction.protocol.AuctionEvent;
import com.auction.protocol.AuctionRequest;
import com.auction.protocol.AuctionResponse;
import com.auction.protocol.ProtocolFilter;
import com.auction.protocol.ProtocolErrors;
import com.auction.protocol.RequestType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketAuctionClientGateway extends AbstractAuctionClientGateway {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile Socket eventSocket;
    private volatile Thread listener;
    private volatile long listenerGeneration;

    public SocketAuctionClientGateway() { this("localhost", 9999); }
    public SocketAuctionClientGateway(String host, int port) { this.host = host; this.port = port; }

    private Socket openSocket() throws IOException {
        Socket connection = new Socket();
        try {
            connection.connect(new InetSocketAddress(host, port), 5000);
            connection.setSoTimeout(15000);
            return connection;
        } catch (IOException exception) { close(connection); throw exception; }
    }

    public synchronized void connect() {
        if (socket != null && !socket.isClosed()) { return; }
        try {
            socket = openSocket();
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            in.setObjectInputFilter(ProtocolFilter.create());
        } catch (IOException exception) {
            closeRequestConnection();
            throw new CommunicationException("Không thể kết nối tới server", exception);
        }
    }

    @Override
    protected AuctionResponse exchange(AuctionRequest request) {
        connect();
        try { return exchangeOnce(request); }
        catch (IOException | ClassNotFoundException exception) {
            closeRequestConnection();
            // A lost response must never replay a bid, registration or auction creation.
            if (isReadOnly(request.getType())) {
                connect();
                try { return exchangeOnce(request); }
                catch (IOException | ClassNotFoundException retryFailure) {
                    closeRequestConnection();
                    throw new CommunicationException("Mất kết nối tới server; vui lòng thử lại", retryFailure);
                }
            }
            throw new CommunicationException("Chưa xác nhận được kết quả; hãy tải lại trước khi thực hiện lại thao tác", exception);
        }
    }

    private AuctionResponse exchangeOnce(AuctionRequest request) throws IOException, ClassNotFoundException {
        out.writeObject(request);
        out.flush();
        out.reset();
        Object value = in.readObject();
        if (!(value instanceof AuctionResponse response)) { throw new IOException("Invalid server response"); }
        return response;
    }

    private static boolean isReadOnly(RequestType type) {
        return switch (type) {
            case LIST_AUCTIONS, LIST_AUCTIONS_FOR_SELLER, LIST_USERS, GET_REGISTRATION_ROLES, GET_AUTO_BID_STATUS -> true;
            default -> false;
        };
    }

    @Override
    protected void sessionChanged() {
        stopListener();
        if (sessionToken == null) { return; }
        long generation = listenerGeneration;
        String token = sessionToken;
        Thread thread = new Thread(() -> listen(generation, token), "auction-events");
        thread.setDaemon(true);
        listener = thread;
        thread.start();
    }

    private void listen(long generation, String token) {
        while (generation == listenerGeneration && !Thread.currentThread().isInterrupted()) {
            try (Socket connection = openSocket();
                 ObjectOutputStream eventOut = new ObjectOutputStream(connection.getOutputStream())) {
                eventOut.flush();
                synchronized (this) {
                    if (generation != listenerGeneration) { return; }
                    eventSocket = connection;
                }
                try (ObjectInputStream eventIn = new ObjectInputStream(connection.getInputStream())) {
                    eventIn.setObjectInputFilter(ProtocolFilter.create());
                    eventOut.writeObject(new AuctionRequest(RequestType.SUBSCRIBE_UPDATES).put("sessionToken", token));
                    eventOut.flush();
                    Object acknowledgement = eventIn.readObject();
                    if (!(acknowledgement instanceof AuctionResponse response)) { throw new IOException("Invalid subscription"); }
                    if (!response.isSuccess()) { throw ProtocolErrors.toException(response); }
                    connection.setSoTimeout(0);
                    fireUpdate(); // Refresh anything missed while the event socket was disconnected.
                    while (generation == listenerGeneration) {
                        Object value = eventIn.readObject();
                        if (value instanceof AuctionEvent) { fireUpdate(); }
                    }
                }
            } catch (com.auction.exception.AuthenticationException exception) {
                return;
            } catch (IOException | ClassNotFoundException exception) {
                if (generation != listenerGeneration) { return; }
                try { Thread.sleep(1000); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private static void fireUpdate() {
        try { javafx.application.Platform.runLater(ClientEventManager::fireUpdate); }
        catch (IllegalStateException exception) { ClientEventManager.fireUpdate(); }
    }

    private void stopListener() {
        listenerGeneration++;
        Thread current = listener;
        if (current != null) { current.interrupt(); }
        close(eventSocket);
        eventSocket = null;
        listener = null;
    }

    private void closeRequestConnection() {
        close(socket);
        socket = null;
        in = null;
        out = null;
    }

    public synchronized void disconnect() { stopListener(); closeRequestConnection(); }

    @Override
    public synchronized void close() { disconnect(); sessionToken = null; }

    private static void close(Socket connection) {
        if (connection != null) { try { connection.close(); } catch (IOException ignored) { } }
    }
}
