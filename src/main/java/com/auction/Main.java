package com.auction;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        AppContext appContext = new AppContext();
        SceneNavigator navigator = new SceneNavigator(primaryStage, appContext);
        navigator.showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
