package com.auction.app;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class TestAdminFxml {
    public static void main(String[] args) {
        Platform.startup(() -> {
            try {
                System.out.println("Loading admin-view.fxml...");
                FXMLLoader loader = new FXMLLoader(TestAdminFxml.class.getResource("/fxml/admin-view.fxml"));
                Parent root = loader.load();
                System.out.println("Success!");
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}
