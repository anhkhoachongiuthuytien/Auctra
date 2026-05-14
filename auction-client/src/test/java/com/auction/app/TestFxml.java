package com.auction.app;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class TestFxml {
    public static void main(String[] args) {
        Platform.startup(() -> {});
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(TestFxml.class.getResource("/fxml/admin-view.fxml"));
                Parent root = loader.load();
                System.out.println("SUCCESS");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.exit(0);
            }
        });
    }
}
