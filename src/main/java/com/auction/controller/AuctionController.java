package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.presentation.AuctionListViewModel;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.List;

public class AuctionController {
    @FXML
    private Label welcomeLabel;

    @FXML
    private Label summaryLabel;

    @FXML
    private Label actionMessageLabel;

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

    @FXML
    private TextField bidAmountField;

    @FXML
    private Button placeBidButton;

    @FXML
    private Button finishAuctionButton;

    private AppContext appContext;
    private SceneNavigator navigator;
    private User currentUser;
    private AuctionListViewModel viewModel;

    public void init(AppContext appContext, SceneNavigator navigator, User currentUser) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.currentUser = currentUser;
        this.viewModel = new AuctionListViewModel(appContext.getAuctionService(), appContext.getBidService());

        configureTable();
        refreshTable();
        configureActions();
        welcomeLabel.setText(viewModel.getWelcomeMessage(currentUser));
    }

    @FXML
    private void handleRefresh() {
        refreshTable();
    }

    @FXML
    private void handleBackToLogin() throws IOException {
        navigator.showLogin();
    }

    @FXML
    private void handlePlaceBid() {
        AuctionListViewModel.ActionResult result =
                viewModel.placeBid(currentUser, auctionTable.getSelectionModel().getSelectedItem(), bidAmountField.getText());
        actionMessageLabel.setText(result.message());
        if (result.success()) {
            bidAmountField.clear();
            refreshTable();
        }
    }

    @FXML
    private void handleFinishAuction() {
        AuctionListViewModel.ActionResult result =
                viewModel.finishAuction(auctionTable.getSelectionModel().getSelectedItem());
        actionMessageLabel.setText(result.message());
        if (result.success()) {
            refreshTable();
        }
    }

    private void configureTable() {
        idColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getId()));
        itemColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItem().getName()));
        sellerColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSeller().getUsername()));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        priceColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getCurrentPrice()));
    }

    private void configureActions() {
        boolean isBidder = currentUser.getClass().getSimpleName().equals("Bidder");
        placeBidButton.setDisable(!isBidder);
        bidAmountField.setDisable(!isBidder);
        finishAuctionButton.setDisable(isBidder);
        actionMessageLabel.setText("Select an auction and perform an action.");
    }

    private void refreshTable() {
        List<Auction> auctions = viewModel.loadAuctions();
        auctionTable.setItems(FXCollections.observableArrayList(auctions));
        summaryLabel.setText(viewModel.getSummaryMessage(auctions));
    }
}
