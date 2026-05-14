package com.auction;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point cho ứng dụng client JavaFX.
 *
 * Hỗ trợ hai chế độ chạy:
 *   1. LOCAL (mặc định):  java com.auction.Main
 *      → Server chạy cùng process, không cần mạng.
 *
 *   2. SOCKET:            java com.auction.Main --socket [host] [port]
 *      → Kết nối tới server đang chạy ở máy khác.
 *      → Cần chạy ServerMain trước.
 *
 * Ví dụ:
 *   java com.auction.Main                          → chế độ local
 *   java com.auction.Main --socket                  → socket tới localhost:9999
 *   java com.auction.Main --socket 192.168.1.10 8888 → socket tới IP:port chỉ định
 */
public class Main extends Application {
    private static final int DEFAULT_PORT = 9999;
    private static final String DEFAULT_HOST = "localhost";

    @Override
    public void start(Stage primaryStage) throws Exception {
        AppContext appContext = createAppContext();
        SceneNavigator navigator = new SceneNavigator(primaryStage, appContext);
        navigator.showLogin();
    }

    private AppContext createAppContext() {
        Parameters params = getParameters();
        java.util.List<String> raw = params.getRaw();

        if (raw.contains("--socket")) {
            int socketIdx = raw.indexOf("--socket");
            String host = DEFAULT_HOST;
            int port = DEFAULT_PORT;

            if (socketIdx + 1 < raw.size()) {
                host = raw.get(socketIdx + 1);
            }
            if (socketIdx + 2 < raw.size()) {
                try {
                    port = Integer.parseInt(raw.get(socketIdx + 2));
                } catch (NumberFormatException e) {
                    // giữ port mặc định
                }
            }

            System.out.println("[Client] Chế độ SOCKET → " + host + ":" + port);
            return new AppContext(host, port);
        }

        System.out.println("[Client] Chế độ LOCAL");
        return new AppContext();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
