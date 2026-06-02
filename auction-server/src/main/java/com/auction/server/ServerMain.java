package com.auction.server;

import java.io.IOException;

/**
 * Entry point để chạy Auction Server riêng biệt.
 *
 * Cách chạy:
 *   java com.auction.server.ServerMain [port]
 *
 * Mặc định port 9999 nếu không truyền tham số.
 * Server sẽ khởi tạo database, seed dữ liệu demo,
 * và lắng nghe kết nối TCP từ client.
 */
public class ServerMain {
    private static final int DEFAULT_PORT = 9999;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Port không hợp lệ, dùng mặc định: " + DEFAULT_PORT);
            }
        }

        System.out.println("=== Auctra Auction Server ===");
        System.out.println("Đang khởi tạo database và services...");

        ServerContext serverContext = new ServerContext("jdbc:sqlite:auction-system.db");
        AuctionServerFacade facade = new AuctionServerFacade(serverContext);
        AuctionSocketServer socketServer = new AuctionSocketServer(facade, port);

        // Khởi động scheduler kiểm tra và tự động kết thúc auction hết hạn
        AuctionExpiryScheduler expiryScheduler = new AuctionExpiryScheduler(serverContext.getAuctionService());
        expiryScheduler.start();

        // Đăng ký shutdown hook để đóng server sạch khi Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nĐang tắt server...");
            expiryScheduler.stop();
            socketServer.stop();
        }));

        try {
            socketServer.start();
        } catch (IOException e) {
            System.err.println("Không thể khởi động server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
