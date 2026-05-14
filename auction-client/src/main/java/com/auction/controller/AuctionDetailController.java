package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.enums.AuctionStatus;
import com.auction.model.auction.Auction;
import com.auction.model.auction.BidTransaction;
import com.auction.model.user.User;
import com.auction.presentation.AuctionListViewModel;
import com.auction.util.UiEffects;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AuctionDetailController {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    @FXML private StackPane rootPane;
    @FXML private Label itemNameLabel;
    @FXML private Label itemTypeChip;
    @FXML private Label bidCountChip;
    @FXML private Label itemDescriptionLabel;
    @FXML private Label sellerLabel;
    @FXML private Label statusLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label winnerLabel;
    @FXML private Label bidHintLabel;
    @FXML private Label bidCountLabel;
    @FXML private Label imagePlaceholderLabel;

    @FXML private javafx.scene.image.ImageView itemImageView;

    @FXML private VBox bidFormPanel;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidButton;

    @FXML private TableView<BidTransaction> bidTable;
    @FXML private TableColumn<BidTransaction, Number> bidIndexColumn;
    @FXML private TableColumn<BidTransaction, String> bidderColumn;
    @FXML private TableColumn<BidTransaction, Number> bidAmountColumn;
    @FXML private TableColumn<BidTransaction, LocalDateTime> bidTimeColumn;

    private AppContext appContext;
    private SceneNavigator navigator;
    private User currentUser;
    private String auctionId;
    private AuctionListViewModel viewModel;

    public void init(AppContext appContext, SceneNavigator navigator, User currentUser, Auction auction) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.currentUser = currentUser;
        this.auctionId = auction.getId();
        this.viewModel = new AuctionListViewModel(appContext.getGateway());

        configureBidTable();

        // Enter trong ô bid → đặt giá
        bidAmountField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handlePlaceBid();
            }
        });

        renderAuction(auction);
    }

    private void configureBidTable() {
        bidIndexColumn.setCellValueFactory(cell -> {
            int index = bidTable.getItems().indexOf(cell.getValue()) + 1;
            return new SimpleIntegerProperty(index);
        });
        bidderColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getBidder() == null ? "?" : cell.getValue().getBidder().getUsername()));
        bidAmountColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getAmount()));
        bidTimeColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getBidTime()));

        bidAmountColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? null : String.format(Locale.US, "$%,.2f", n.doubleValue()));
                setStyle("-fx-text-fill: #4285F4; -fx-font-weight: bold;");
            }
        });
        bidTimeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.format(TIME_FMT));
            }
        });
    }

    private void renderAuction(Auction auction) {
        itemNameLabel.setText(auction.getItem().getName());
        itemTypeChip.setText(auction.getItem().getClass().getSimpleName());
        itemDescriptionLabel.setText(auction.getItem().getDescription());
        sellerLabel.setText(auction.getSeller().getUsername());

        // Load ảnh nếu có
        String imagePath = auction.getItem().getImagePath();
        if (com.auction.util.ImageStorage.exists(imagePath)) {
            try {
                itemImageView.setImage(new javafx.scene.image.Image(
                        new java.io.File(imagePath).toURI().toString(),
                        440, 340, true, true));
                imagePlaceholderLabel.setVisible(false);
            } catch (Exception ex) {
                itemImageView.setImage(null);
                imagePlaceholderLabel.setVisible(true);
            }
        } else {
            itemImageView.setImage(null);
            imagePlaceholderLabel.setVisible(true);
        }

        statusLabel.setText(auction.getStatus().name());
        statusLabel.getStyleClass().removeAll("badge", "badge-open", "badge-running",
                "badge-finished", "badge-paid", "badge-canceled");
        statusLabel.getStyleClass().addAll("badge", switch (auction.getStatus()) {
            case OPEN     -> "badge-open";
            case RUNNING  -> "badge-running";
            case FINISHED -> "badge-finished";
            case PAID     -> "badge-paid";
            case CANCELED -> "badge-canceled";
        });

        currentPriceLabel.setText(String.format(Locale.US, "$%,.2f", auction.getCurrentPrice()));
        startingPriceLabel.setText("Giá khởi điểm: "
                + String.format(Locale.US, "$%,.2f", auction.getItem().getStartingPrice()));
        winnerLabel.setText(auction.getWinner() == null ? "Chưa có"
                : auction.getWinner().getUsername());

        // Lịch sử bid — order mới nhất ở cuối (theo addBid logic)
        bidTable.setItems(FXCollections.observableArrayList(auction.getBids()));
        bidCountLabel.setText(auction.getBids().size() + " lượt");
        if (bidCountChip != null) bidCountChip.setText(auction.getBids().size() + " lượt");

        // Bật/tắt form đặt giá
        boolean canBid = auction.getStatus() == AuctionStatus.RUNNING
                && "Bidder".equals(currentUser.getClass().getSimpleName());
        bidFormPanel.setVisible(canBid);
        bidFormPanel.setManaged(canBid);
        if (canBid) {
            bidHintLabel.setText("Số tiền tối thiểu: "
                    + String.format(Locale.US, "$%,.2f", auction.getCurrentPrice() + 1));
        }
    }

    @FXML
    private void handlePlaceBid() {
        Auction auction = findAuction();
        if (auction == null) {
            UiEffects.showToast(rootPane, "Không tìm thấy phiên đấu giá",
                    UiEffects.ToastType.ERROR, 2400);
            return;
        }
        AuctionListViewModel.ActionResult result =
                viewModel.placeBid(currentUser, auction, bidAmountField.getText());
        if (result.success()) {
            UiEffects.showToast(rootPane,
                    "Đặt giá thành công: " + bidAmountField.getText(),
                    UiEffects.ToastType.SUCCESS, 2000);
            bidAmountField.clear();
            reloadAuction();
        } else {
            UiEffects.showToast(rootPane, result.message(), UiEffects.ToastType.ERROR, 2400);
        }
    }

    @FXML private void bidPlusSmall()  { addToBid(10); }
    @FXML private void bidPlusMedium() { addToBid(100); }
    @FXML private void bidPlusLarge()  { addToBid(1000); }

    private void addToBid(double delta) {
        Auction a = findAuction();
        if (a == null) return;
        double base;
        try {
            base = Double.parseDouble(bidAmountField.getText());
        } catch (NumberFormatException ex) {
            base = a.getCurrentPrice();
        }
        bidAmountField.setText(String.format(Locale.US, "%.0f", base + delta));
    }

    @FXML
    private void handleRefresh() {
        UiEffects.runWithLoading(rootPane, 400, this::reloadAuction, () ->
                UiEffects.showToast(rootPane, "Đã cập nhật", UiEffects.ToastType.INFO, 1400));
    }

    @FXML
    private void handleBack() throws IOException {
        navigator.showHome(currentUser);
    }

    private void reloadAuction() {
        Auction a = findAuction();
        if (a != null) renderAuction(a);
    }

    private Auction findAuction() {
        return appContext.getGateway().listAuctions().stream()
                .filter(x -> x.getId().equals(auctionId))
                .findFirst()
                .orElse(null);
    }
}
