package com.auction.client;

import com.auction.exception.CommunicationException;
import com.auction.model.user.Seller;
import com.auction.protocol.AuctionRequest;
import com.auction.protocol.AuctionResponse;
import com.auction.protocol.RequestType;
import org.junit.jupiter.api.Test;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LostResponseTest {
    @Test
    void lostMutationResponseDoesNotReplayAuctionCreation() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (ServerSocket endpoint = new ServerSocket(0)) {
            endpoint.setSoTimeout(800);
            Future<Integer> calls = executor.submit(() -> {
                int count = 0;
                try (Socket connection = endpoint.accept();
                     ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream())) {
                    out.flush();
                    try (ObjectInputStream in = new ObjectInputStream(connection.getInputStream())) {
                        assertEquals(RequestType.CREATE_AUCTION, ((AuctionRequest) in.readObject()).getType());
                        count++;
                        // Simulate a committed operation whose response is lost on the network.
                    }
                }
                try (Socket duplicate = endpoint.accept()) { count++; }
                catch (SocketTimeoutException expected) { }
                return count;
            });
            SocketAuctionClientGateway gateway = new SocketAuctionClientGateway("localhost", endpoint.getLocalPort());
            try {
                assertThrows(CommunicationException.class, () -> gateway.createAuctionForSeller(
                        new Seller("seller", "seller", "seller@test.local"), "Electronics", "item", "description", 100));
                assertEquals(1, calls.get(3, TimeUnit.SECONDS));
            } finally { gateway.close(); }
        } finally { executor.shutdownNow(); }
    }

    @Test
    void readOnlyRequestsReconnectAndKeepTheSessionToken() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (ServerSocket endpoint = new ServerSocket(0)) {
            endpoint.setSoTimeout(3000);
            Future<?> served = executor.submit(() -> {
                for (int attempt = 0; attempt < 2; attempt++) {
                    try (Socket connection = endpoint.accept();
                         ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream())) {
                        out.flush();
                        try (ObjectInputStream in = new ObjectInputStream(connection.getInputStream())) {
                            AuctionRequest request = (AuctionRequest) in.readObject();
                            assertEquals("existing-session", request.get("sessionToken"));
                            if (attempt == 1) { out.writeObject(AuctionResponse.ok(new ArrayList<>())); out.flush(); }
                        }
                    } catch (Exception exception) { throw new RuntimeException(exception); }
                }
            });
            SocketAuctionClientGateway gateway = new SocketAuctionClientGateway("localhost", endpoint.getLocalPort());
            gateway.sessionToken = "existing-session";
            try {
                assertTrue(gateway.listAuctions().isEmpty());
                served.get(5, TimeUnit.SECONDS);
            } finally { gateway.close(); }
        } finally { executor.shutdownNow(); }
    }
}
