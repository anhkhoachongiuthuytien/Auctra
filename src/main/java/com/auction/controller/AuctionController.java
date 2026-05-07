package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;

public class AuctionController {
    @FXML
    private Label welcomeLabel;

    @FXML
    private Label summaryLabel;

    @FXML
    private TableView<Auction> auctionTable;

    @FXML
    private TableColumn<Auction, String> idColumn;

    @FXML
    private TableColumn<Auction, String> itemColumn;

    @FXML
    private TableColumn<Auction, String> sellerColumn;

    @FXML
    private TableColumn<Auction, String> statusColumn;

    @FXML
    private TableColumn<Auction, Number> priceColumn;

    private AppContext appContext;
    private SceneNavigator navigator;
    private User currentUser;

    public void init(AppContext appContext, SceneNavigator navigator, User currentUser) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.currentUser = currentUser;

        configureTable();
        refreshTable();
        welcomeLabel.setText("Logged in as: " + currentUser.getUsername() + " (" + currentUser.getEmail() + ")");
    }

    @FXML
    private void handleRefresh() {
        refreshTable();
    }

    @FXML
    private void handleBackToLogin() throws IOException {
        navigator.showLogin();
    }

    private void configureTable() {
        idColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getId()));
        itemColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItem().getName()));
        sellerColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSeller().getUsername()));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        priceColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getCurrentPrice()));
    }

    private void refreshTable() {
        auctionTable.setItems(FXCollections.observableArrayList(appContext.getAuctionService().listAuctions()));
        summaryLabel.setText("Loaded " + auctionTable.getItems().size() + " auctions. Running auctions can receive concurrent bids.");
    }
}
