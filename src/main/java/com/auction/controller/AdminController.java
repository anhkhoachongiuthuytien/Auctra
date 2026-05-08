package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.model.auction.Auction;
import com.auction.model.user.Admin;
import com.auction.model.user.User;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.util.List;

public class AdminController {
    @FXML
    private Label welcomeLabel;

    @FXML
    private Label actionMessageLabel;

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, String> userIdColumn;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableView<Auction> auctionTable;

    @FXML
    private TableColumn<Auction, String> auctionIdColumn;

    @FXML
    private TableColumn<Auction, String> itemColumn;

    @FXML
    private TableColumn<Auction, String> sellerColumn;

    @FXML
    private TableColumn<Auction, String> statusColumn;

    @FXML
    private TableColumn<Auction, Number> currentPriceColumn;

    private AppContext appContext;
    private SceneNavigator navigator;
    private Admin currentAdmin;

    public void init(AppContext appContext, SceneNavigator navigator, Admin currentAdmin) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.currentAdmin = currentAdmin;

        welcomeLabel.setText("Admin dashboard: " + currentAdmin.getUsername() + " (" + currentAdmin.getEmail() + ")");
        configureUserTable();
        configureAuctionTable();
        refreshData();
        showInfo("Admin can review users and manage auction lifecycle.");
    }

    @FXML
    private void handleRefresh() {
        refreshData();
        showInfo("Admin dashboard refreshed.");
    }

    @FXML
    private void handleCancelAuction() {
        Auction auction = auctionTable.getSelectionModel().getSelectedItem();
        if (auction == null) {
            showError("Please select an auction first.");
            return;
        }

        try {
            appContext.getAuctionService().cancelAuction(auction.getId());
            refreshData();
            showSuccess("Auction " + auction.getId() + " has been canceled.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleMarkPaid() {
        Auction auction = auctionTable.getSelectionModel().getSelectedItem();
        if (auction == null) {
            showError("Please select an auction first.");
            return;
        }

        try {
            appContext.getAuctionService().markAuctionPaid(auction.getId());
            refreshData();
            showSuccess("Auction " + auction.getId() + " has been marked PAID.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin() throws IOException {
        navigator.showLogin();
    }

    private void configureUserTable() {
        userIdColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getId()));
        usernameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        emailColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEmail()));
        roleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getClass().getSimpleName()));
    }

    private void configureAuctionTable() {
        auctionIdColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getId()));
        itemColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItem().getName()));
        sellerColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSeller().getUsername()));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        currentPriceColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getCurrentPrice()));
    }

    private void refreshData() {
        List<User> users = appContext.getUserService().getAllUsers();
        List<Auction> auctions = appContext.getAuctionService().listAuctions();
        userTable.setItems(FXCollections.observableArrayList(users));
        auctionTable.setItems(FXCollections.observableArrayList(auctions));
    }

    private void showInfo(String message) {
        actionMessageLabel.setText(message);
        actionMessageLabel.getStyleClass().removeAll("error-label", "success-label");
        if (!actionMessageLabel.getStyleClass().contains("info-label")) {
            actionMessageLabel.getStyleClass().add("info-label");
        }
    }

    private void showSuccess(String message) {
        actionMessageLabel.setText(message);
        actionMessageLabel.getStyleClass().removeAll("error-label", "info-label");
        if (!actionMessageLabel.getStyleClass().contains("success-label")) {
            actionMessageLabel.getStyleClass().add("success-label");
        }
    }

    private void showError(String message) {
        actionMessageLabel.setText(message);
        actionMessageLabel.getStyleClass().removeAll("success-label", "info-label");
        if (!actionMessageLabel.getStyleClass().contains("error-label")) {
            actionMessageLabel.getStyleClass().add("error-label");
        }
    }
}
