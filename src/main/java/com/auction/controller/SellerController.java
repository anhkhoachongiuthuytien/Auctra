package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Seller;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.List;

public class SellerController {
    @FXML
    private Label welcomeLabel;

    @FXML
    private Label actionMessageLabel;

    @FXML
    private ComboBox<String> itemTypeComboBox;

    @FXML
    private TextField itemNameField;

    @FXML
    private TextField itemDescriptionField;

    @FXML
    private TextField startingPriceField;

    @FXML
    private TableView<Auction> sellerAuctionTable;

    @FXML
    private TableColumn<Auction, String> auctionIdColumn;

    @FXML
    private TableColumn<Auction, String> itemNameColumn;

    @FXML
    private TableColumn<Auction, String> statusColumn;

    @FXML
    private TableColumn<Auction, Number> currentPriceColumn;

    private AppContext appContext;
    private SceneNavigator navigator;
    private Seller currentSeller;

    public void init(AppContext appContext, SceneNavigator navigator, Seller currentSeller) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.currentSeller = currentSeller;

        welcomeLabel.setText("Seller dashboard: " + currentSeller.getUsername() + " (" + currentSeller.getEmail() + ")");
        itemTypeComboBox.setItems(FXCollections.observableArrayList("Art", "Electronics", "Vehicle"));
        itemTypeComboBox.getSelectionModel().select("Electronics");
        configureTable();
        refreshAuctions();
        showInfo("Create an item to open a new auction.");
    }

    @FXML
    private void handleCreateAuction() {
        try {
            double startingPrice = Double.parseDouble(startingPriceField.getText());
            Item item = appContext.getSellerService().createItem(
                    itemTypeComboBox.getValue(),
                    itemNameField.getText(),
                    itemDescriptionField.getText(),
                    startingPrice
            );
            Auction auction = appContext.getSellerService().createAuction(item, currentSeller);
            clearItemForm();
            refreshAuctions();
            showSuccess("Created item and auction " + auction.getId() + " successfully.");
        } catch (NumberFormatException ex) {
            showError("Starting price must be a valid number.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleStartAuction() {
        Auction auction = sellerAuctionTable.getSelectionModel().getSelectedItem();
        if (auction == null) {
            showError("Please select one of your auctions first.");
            return;
        }

        try {
            appContext.getAuctionService().startAuction(auction.getId());
            refreshAuctions();
            showSuccess("Auction " + auction.getId() + " is now RUNNING.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleFinishAuction() {
        Auction auction = sellerAuctionTable.getSelectionModel().getSelectedItem();
        if (auction == null) {
            showError("Please select one of your auctions first.");
            return;
        }

        try {
            appContext.getAuctionService().finishAuction(auction.getId());
            refreshAuctions();
            showSuccess("Auction " + auction.getId() + " has been finished.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        refreshAuctions();
        showInfo("Seller auction list refreshed.");
    }

    @FXML
    private void handleBackToLogin() throws IOException {
        navigator.showLogin();
    }

    private void configureTable() {
        auctionIdColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getId()));
        itemNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItem().getName()));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        currentPriceColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getCurrentPrice()));
    }

    private void refreshAuctions() {
        List<Auction> sellerAuctions = appContext.getAuctionService().listAuctions().stream()
                .filter(auction -> auction.getSeller().getId().equals(currentSeller.getId()))
                .toList();
        sellerAuctionTable.setItems(FXCollections.observableArrayList(sellerAuctions));
    }

    private void clearItemForm() {
        itemNameField.clear();
        itemDescriptionField.clear();
        startingPriceField.clear();
        itemTypeComboBox.getSelectionModel().select("Electronics");
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
