package com.auction.controller;

import com.auction.model.auction.Auction;
import com.auction.model.user.Bidder;
import com.auction.service.BidService;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class BidController {
    private BidService bidService;
    private Auction auction;

    StringProperty currentId = new SimpleStringProperty("");
    DoubleProperty currentPrice = new SimpleDoubleProperty(0);

    public void setBidService(BidService bidService) {
        this.bidService = bidService;
    }

    public void setAuctionAndService(Auction auction, BidService service){
        this.auction = auction;
        this.bidService = service;
        if (this.auction!=null){
            currentId.set(auction.getId());
            currentPrice.set(auction.getCurrentPrice());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML private Label lblItemName;
    @FXML private Label lblCurrentPrice;
    @FXML private TextField txtBidAmount;

    @FXML
    public void initialize(){
        lblItemName.textProperty().bind(currentId);
        lblCurrentPrice.textProperty().bind(currentPrice.asString("%,.0f VND"));
    }

    @FXML
    public void handlePlaceBid() {
        try {
            double bidAmount = Double.parseDouble(txtBidAmount.getText()); // Lấy số money Bidder
            Bidder currentBidder = new Bidder();// Lấy thông tin Bidder (tạm thời)
            bidService.placeBid(auction.getId(), currentBidder, bidAmount);
            currentPrice.set(auction.getCurrentPrice());
            txtBidAmount.clear();
            showAlert(Alert.AlertType.INFORMATION, "Success", "You have paid the bid successfully");

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter the number!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.WARNING, "Error", e.getMessage());
        }
    }
}