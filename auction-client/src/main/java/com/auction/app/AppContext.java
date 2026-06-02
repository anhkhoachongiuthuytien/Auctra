package com.auction.app;

import com.auction.client.AuctionClientGateway;
import com.auction.client.LocalAuctionClientGateway;
import com.auction.client.SocketAuctionClientGateway;
import com.auction.server.AuctionServerFacade;
import com.auction.server.ServerContext;

/**
 * Composition root của ứng dụng client.
 *
 * Hỗ trợ hai chế độ:
 * - LOCAL: client gọi trực tiếp server facade (cùng process, không qua mạng)
 * - SOCKET: client giao tiếp với server qua TCP socket (cần ServerMain chạy riêng)
 *
 * Mặc định chạy LOCAL. Để chuyển sang SOCKET, dùng constructor:
 *   new AppContext("localhost", 9999)
 */
public class AppContext {
    private final AuctionClientGateway gateway;

    /**
     * Chế độ LOCAL — server chạy cùng process.
     */
    public AppContext() {
        ServerContext serverContext = new ServerContext("jdbc:sqlite:auction-system.db");
        AuctionServerFacade serverFacade = new AuctionServerFacade(serverContext);
        this.gateway = new LocalAuctionClientGateway(serverFacade);

        // Khởi động scheduler kiểm tra auction hết hạn (daemon thread, tự dọn khi app tắt)
        com.auction.server.AuctionExpiryScheduler expiryScheduler =
                new com.auction.server.AuctionExpiryScheduler(serverContext.getAuctionService());
        expiryScheduler.start();
    }

    /**
     * Chế độ SOCKET — kết nối tới server qua mạng.
     *
     * @param host địa chỉ server (ví dụ "localhost" hoặc "192.168.1.100")
     * @param port cổng server (mặc định 9999)
     */
    public AppContext(String host, int port) {
        SocketAuctionClientGateway socketGateway = new SocketAuctionClientGateway(host, port);
        socketGateway.connect();
        this.gateway = socketGateway;
    }

    public AppContext(AuctionClientGateway gateway) {
        this.gateway = gateway;
    }

    public AuctionClientGateway getGateway() {
        return gateway;
    }
}
