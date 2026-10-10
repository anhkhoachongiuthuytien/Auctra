package com.auction.client;

import com.auction.exception.AuthenticationException;
import com.auction.exception.AuthorizationException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.ValidationException;
import com.auction.model.auction.Auction;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.presentation.LoginViewModel;
import com.auction.protocol.AuctionRequest;
import com.auction.protocol.AuctionResponse;
import com.auction.protocol.ErrorCode;
import com.auction.protocol.RequestType;
import com.auction.server.AuctionServerFacade;
import com.auction.server.AuctionSocketServer;
import com.auction.server.ServerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketGatewayIntegrationTest {
    @TempDir Path directory;
    private AuctionSocketServer server;
    private Thread serverThread;
    private AuctionServerFacade facade;
    private Auction auction;
    private final List<SocketAuctionClientGateway> clients = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        ServerContext context = new ServerContext("jdbc:sqlite:" + directory.resolve("socket.db"), false);
        Seller seller = context.getAuthService().registerSeller("seller", "seller@test.local", "password123");
        context.getAuthService().registerBidder("first", "first@test.local", "password123");
        context.getAuthService().registerBidder("second", "second@test.local", "password123");
        facade = new AuctionServerFacade(context);
        auction = facade.createAuctionForSeller(seller, "Electronics", "Laptop", "description", 100);
        facade.startAuction(auction.getId());
        server = new AuctionSocketServer(facade, 0);
        serverThread = new Thread(() -> {
            try { server.start(); }
            catch (java.io.IOException exception) { throw new RuntimeException(exception); }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (server.getPort() <= 0 && System.nanoTime() < deadline) { Thread.sleep(10); }
        assertTrue(server.getPort() > 0);
    }

    private SocketAuctionClientGateway client() {
        SocketAuctionClientGateway gateway = new SocketAuctionClientGateway("localhost", server.getPort());
        clients.add(gateway);
        return gateway;
    }

    @AfterEach
    void tearDown() throws Exception {
        clients.forEach(SocketAuctionClientGateway::close);
        if (server != null) { server.stop(); }
        if (serverThread != null) { serverThread.join(5000); assertFalse(serverThread.isAlive()); }
    }

    @Test
    void socketErrorsRetainTheirTypesAndLoginViewModelShowsFailure() {
        SocketAuctionClientGateway gateway = client();
        assertFalse(new LoginViewModel(gateway).login("first@test.local", "incorrect123").success());
        Bidder first = (Bidder) gateway.login("first@test.local", "password123");
        assertThrows(InvalidBidException.class, () -> gateway.placeBid(auction.getId(), first, 99));
        assertThrows(ValidationException.class, () -> gateway.registerAutoBid(auction.getId(), first.getId(), 200, 0));
        assertThrows(AuthorizationException.class, gateway::listUsers);
        gateway.logout();
        assertThrows(AuthenticationException.class, gateway::listAuctions);
    }

    @Test
    void automaticBiddingWorksAcrossTwoActualSocketClientsAndPersists() {
        SocketAuctionClientGateway firstClient = client();
        SocketAuctionClientGateway secondClient = client();
        Bidder first = (Bidder) firstClient.login("first@test.local", "password123");
        Bidder second = (Bidder) secondClient.login("second@test.local", "password123");
        firstClient.registerAutoBid(auction.getId(), first.getId(), 200, 10);
        assertEquals(110, facade.getAuction(auction.getId()).getCurrentPrice());
        secondClient.placeBid(auction.getId(), second, 120);
        Auction stored = firstClient.listAuctions().get(0);
        assertEquals(130, stored.getCurrentPrice());
        assertEquals(first.getId(), stored.getWinner().getId());
        assertEquals(3, stored.getBids().size());
        assertNotNull(firstClient.getAutoBid(auction.getId(), first.getId()));
        firstClient.cancelAutoBid(auction.getId(), first.getId());
        assertNull(firstClient.getAutoBid(auction.getId(), first.getId()));
        secondClient.placeBid(auction.getId(), second, 140);
        assertEquals(second.getId(), facade.getAuction(auction.getId()).getWinner().getId());
    }

    @Test
    void moreThanFiveClientsWithPersistentEventConnectionsRemainResponsive() {
        for (int i = 0; i < 8; i++) {
            SocketAuctionClientGateway gateway = client();
            gateway.login("first@test.local", "password123");
        }
        for (SocketAuctionClientGateway gateway : clients) { assertEquals(1, gateway.listAuctions().size()); }
    }

    @Test
    void rawUnauthenticatedRequestsAreRejectedAndUnknownSerializedClassesAreFiltered() throws Exception {
        try (Socket socket = new Socket("localhost", server.getPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            socket.setSoTimeout(3000);
            out.flush();
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                out.writeObject(new AuctionRequest(RequestType.LIST_USERS));
                out.flush();
                AuctionResponse response = (AuctionResponse) in.readObject();
                assertEquals(ErrorCode.AUTHENTICATION, response.getErrorCode());
                out.writeObject(new java.io.File("untrusted"));
                out.flush();
                assertThrows(java.io.IOException.class, in::readObject);
            }
        }
    }

    @Test
    void localGatewayUsesTheSameSecurityAndExceptionBoundary() {
        LocalAuctionClientGateway local = new LocalAuctionClientGateway(facade);
        assertThrows(AuthenticationException.class, local::listUsers);
        Bidder first = (Bidder) local.login("first@test.local", "password123");
        assertThrows(AuthorizationException.class, local::listUsers);
        assertThrows(InvalidBidException.class, () -> local.placeBid(auction.getId(), first, 99));
        local.registerAutoBid(auction.getId(), first.getId(), 200, 10);
        assertEquals(110, local.listAuctions().get(0).getCurrentPrice());
    }

    @Test
    void authenticatedSubscriberReceivesTheCommittedBidEvent() throws Exception {
        SocketAuctionClientGateway firstClient = client();
        SocketAuctionClientGateway secondClient = client();
        Bidder first = (Bidder) firstClient.login("first@test.local", "password123");
        Bidder second = (Bidder) secondClient.login("second@test.local", "password123");
        firstClient.registerAutoBid(auction.getId(), first.getId(), 200, 10);
        try (Socket socket = new Socket("localhost", server.getPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            socket.setSoTimeout(5000);
            out.flush();
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                out.writeObject(new AuctionRequest(RequestType.SUBSCRIBE_UPDATES).put("sessionToken", firstClient.sessionToken));
                out.flush();
                assertTrue(((AuctionResponse) in.readObject()).isSuccess());
                secondClient.placeBid(auction.getId(), second, 120);
                com.auction.protocol.AuctionEvent event = (com.auction.protocol.AuctionEvent) in.readObject();
                assertEquals("NEW_BID", event.getEventType());
                assertEquals(auction.getId(), event.getAuctionId());
                assertEquals(130, firstClient.listAuctions().get(0).getCurrentPrice());
            }
        }
    }
}
