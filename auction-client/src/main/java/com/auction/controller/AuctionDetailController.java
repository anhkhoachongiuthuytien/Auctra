package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.enums.AuctionStatus;
import com.auction.model.auction.Auction;
import com.auction.model.auction.BidTransaction;
import com.auction.model.user.User;
import com.auction.presentation.AuctionListViewModel;
import com.auction.ui.UIAnimations;
import com.auction.util.ItemImageHelper;
import com.auction.util.UiEffects;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

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
    @FXML private Label detailUserInitialsLabel;
    @FXML private Label detailUserNameLabel;

    @FXML private javafx.scene.image.ImageView itemImageView;
    @FXML private FlowPane thumbnailContainer;

    @FXML private VBox bidFormPanel;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidButton;

    @FXML private VBox autoBidFormPanel;
    @FXML private Label autoBidStatusLabel;
    @FXML private TextField autoBidMaxField;
    @FXML private TextField autoBidIncrementField;
    @FXML private Button toggleAutoBidButton;

    @FXML private Label countdownLabel;
    @FXML private Label realtimeStatusLabel;
    @FXML private ProgressBar progressBar;
    @FXML private LineChart<String, Number> priceLineChart;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;

    @FXML private TableView<BidTransaction> bidTable;
    @FXML private TableColumn<BidTransaction, Number> bidIndexColumn;
    @FXML private TableColumn<BidTransaction, String> bidderColumn;
    @FXML private TableColumn<BidTransaction, Number> bidAmountColumn;
    @FXML private TableColumn<BidTransaction, LocalDateTime> bidTimeColumn;

    private javafx.animation.Timeline countdownTimeline;
    private com.auction.model.auction.AutoBidConfig currentAutoBid;

    private AppContext appContext;
    private SceneNavigator navigator;
    private User currentUser;
    private String auctionId;
    private AuctionListViewModel viewModel;
    private Auction currentAuction;

    public void init(AppContext appContext, SceneNavigator navigator, User currentUser, Auction auction) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.currentUser = currentUser;
        this.auctionId = auction.getId();
        this.viewModel = new AuctionListViewModel(appContext.getGateway());
        this.currentAuction = auction;

        configureBidTable();

        bidTable.setPlaceholder(new Label("Chưa có lượt đặt giá. Hãy là người đầu tiên."));
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null && countdownTimeline != null) countdownTimeline.stop();
        });

        // Đăng ký nhận sự kiện realtime cho phòng đấu giá này
        com.auction.client.ClientEventManager.clearListeners();
        com.auction.client.ClientEventManager.addListener(this::reloadAuction);

        // Khởi động đếm ngược
        startCountdown();

        updateUserChrome();
        renderAuction(auction);
    }

    private void configureBidTable() {
        bidIndexColumn.setCellValueFactory(cell -> {
            int index = bidTable.getItems().size() - bidTable.getItems().indexOf(cell.getValue());
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
                getStyleClass().remove("price-cell");
                if (!empty && n != null) {
                    getStyleClass().add("price-cell");
                }
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
        this.currentAuction = auction;
        itemNameLabel.setText(auction.getItem().getName());
        itemTypeChip.setText(auction.getItem().getClass().getSimpleName());
        itemDescriptionLabel.setText(auction.getItem().getDescription());
        sellerLabel.setText(auction.getSeller().getUsername());

        // Load ảnh nếu có (hỗ trợ nhiều ảnh)
        java.util.List<String> imagePaths = auction.getItem().getImagePaths();
        if (thumbnailContainer != null) {
            thumbnailContainer.getChildren().clear();
        }

        if (!imagePaths.isEmpty()) {
            String firstImage = imagePaths.get(0);
            setMainImage(firstImage);

            if (thumbnailContainer != null && imagePaths.size() > 1) {
                thumbnailContainer.setManaged(true);
                thumbnailContainer.setVisible(true);
                for (String path : imagePaths) {
                    javafx.scene.image.ImageView thumbView = ItemImageHelper.createImageView(path, 60, 60);
                    if (thumbView != null) {
                        StackPane thumbBox = new StackPane(thumbView);
                        thumbBox.getStyleClass().add("image-preview-box");
                        thumbBox.setPrefSize(64, 64);
                        thumbBox.setMinSize(64, 64);
                        thumbBox.setCursor(javafx.scene.Cursor.HAND);

                        if (path.equals(firstImage)) {
                            thumbBox.getStyleClass().add("detail-thumbnail-selected");
                        }

                        thumbBox.setOnMouseClicked(e -> {
                            setMainImage(path);
                            for (javafx.scene.Node node : thumbnailContainer.getChildren()) {
                                node.getStyleClass().remove("detail-thumbnail-selected");
                            }
                            thumbBox.getStyleClass().add("detail-thumbnail-selected");
                        });

                        thumbnailContainer.getChildren().add(thumbBox);
                    }
                }
            } else if (thumbnailContainer != null) {
                thumbnailContainer.setManaged(false);
                thumbnailContainer.setVisible(false);
            }
        } else {
            itemImageView.setImage(null);
            imagePlaceholderLabel.setVisible(true);
            if (thumbnailContainer != null) {
                thumbnailContainer.setManaged(false);
                thumbnailContainer.setVisible(false);
            }
        }

        statusLabel.setText(com.auction.ui.BadgeFactory.statusText(auction.getStatus()));
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

        List<BidTransaction> history = new java.util.ArrayList<>(auction.getBids());
        java.util.Collections.reverse(history);
        bidTable.setItems(FXCollections.observableArrayList(history));
        bidCountLabel.setText(auction.getBids().size() + " lượt");
        if (bidCountChip != null) bidCountChip.setText(auction.getBids().size() + " lượt");

        // Bật/tắt form đặt giá
        boolean canBid = auction.getStatus() == AuctionStatus.RUNNING
                && auction.getEndTime() != null && LocalDateTime.now().isBefore(auction.getEndTime())
                && "Bidder".equals(currentUser.getClass().getSimpleName());
        bidFormPanel.setVisible(canBid);
        bidFormPanel.setManaged(canBid);
        if (canBid) {
            bidHintLabel.setText("Giá gợi ý: "
                    + String.format(Locale.US, "$%,.2f", auction.getCurrentPrice() + 1));
        }

        // Cập nhật biểu đồ giá
        updatePriceChart(auction);

        // Hiển thị cấu hình Auto-Bid cho Bidder
        if ("Bidder".equals(currentUser.getClass().getSimpleName())) {
            autoBidFormPanel.setVisible(true);
            autoBidFormPanel.setManaged(true);
            try {
                com.auction.model.auction.AutoBidConfig config = appContext.getGateway().getAutoBid(auctionId, currentUser.getId());
                if (config != null) {
                    currentAutoBid = config;
                    autoBidStatusLabel.setText(String.format(Locale.US, "Đang bật · Giới hạn $%,.2f · Bước nâng $%,.2f",
                            config.getMaxPrice(), config.getIncrement()));
                    toggleAutoBidButton.setText("Tắt tự động đặt giá");
                    toggleAutoBidButton.getStyleClass().removeAll("button-success", "button-danger");
                    toggleAutoBidButton.getStyleClass().add("button-danger");
                    autoBidMaxField.setDisable(true);
                    autoBidIncrementField.setDisable(true);
                    boolean isWinner = auction.getWinner() != null && auction.getWinner().getId().equals(currentUser.getId());
                    if (!canBid) {
                        autoBidStatusLabel.setText("Trạng thái: Phiên đã kết thúc; Auto-Bid không còn trả giá");
                    } else if (!isWinner && config.getMaxPrice() < auction.getCurrentPrice() + config.getIncrement()) {
                        autoBidStatusLabel.setText("Trạng thái: Đã đạt giới hạn giá; có thể hủy để đặt lại cấu hình");
                    }
                } else {
                    currentAutoBid = null;
                    autoBidStatusLabel.setText("Trạng thái: Chưa kích hoạt");
                    toggleAutoBidButton.setText("Bật tự động đặt giá");
                    toggleAutoBidButton.getStyleClass().removeAll("button-success", "button-danger");
                    toggleAutoBidButton.getStyleClass().add("button-success");
                    autoBidMaxField.setDisable(!canBid);
                    autoBidIncrementField.setDisable(!canBid);
                    if (!canBid) { autoBidStatusLabel.setText("Trạng thái: Phiên chưa mở hoặc đã kết thúc"); }
                }
                toggleAutoBidButton.setDisable(!canBid && currentAutoBid == null);
            } catch (Exception e) {
                toggleAutoBidButton.setDisable(true);
                autoBidStatusLabel.setText("Lỗi kiểm tra Auto-Bid: " + e.getMessage());
            }
        } else {
            autoBidFormPanel.setVisible(false);
            autoBidFormPanel.setManaged(false);
        }
    }

    private void updateUserChrome() {
        if (currentUser == null) {
            return;
        }
        if (detailUserNameLabel != null) {
            detailUserNameLabel.setText(currentUser.getUsername());
        }
        if (detailUserInitialsLabel != null) {
            String name = currentUser.getUsername();
            detailUserInitialsLabel.setText(name == null || name.isBlank()
                    ? "U"
                    : name.substring(0, 1).toUpperCase(Locale.ROOT));
        }
    }

    private void updatePriceChart(Auction auction) {
        priceLineChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Lịch sử giá");

        // Điểm xuất phát: Giá khởi điểm
        double startingPrice = auction.getItem().getStartingPrice();
        series.getData().add(new XYChart.Data<>("Khởi điểm", startingPrice));

        // Các mốc bid tiếp theo
        List<BidTransaction> bids = auction.getBids();
        for (int i = 0; i < bids.size(); i++) {
            BidTransaction bid = bids.get(i);
            series.getData().add(new XYChart.Data<>("Lượt " + (i + 1), bid.getAmount()));
        }

        if (bids.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Hiện tại", auction.getCurrentPrice()));
        }

        priceLineChart.getData().add(series);
        if (realtimeStatusLabel != null) {
            realtimeStatusLabel.setText(bids.isEmpty()
                    ? "Chưa có lượt đặt giá"
                    : "Dữ liệu hiện tại: " + bids.size() + " lượt đặt giá");
        }
    }

    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        countdownTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> updateCountdown())
        );
        countdownTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        updateCountdown();
        if (currentAuction != null && currentAuction.getStatus() == AuctionStatus.RUNNING
                && currentAuction.getEndTime() != null && LocalDateTime.now().isBefore(currentAuction.getEndTime())) {
            countdownTimeline.play();
        }
    }

    private void updateCountdown() {
        Auction a = currentAuction;
        if (a == null) {
            countdownLabel.setText("Không tìm thấy phiên");
            progressBar.setProgress(0);
            return;
        }

        if (a.getStatus() != AuctionStatus.RUNNING) {
            countdownLabel.setText("Phiên đấu giá: " + com.auction.ui.BadgeFactory.statusText(a.getStatus()));
            progressBar.setProgress(0);
            countdownTimeline.stop();
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = a.getEndTime();
        if (end == null) {
            countdownLabel.setText("Không có thời hạn");
            progressBar.setProgress(1.0);
            return;
        }

        java.time.Duration remaining = java.time.Duration.between(now, end);
        long seconds = remaining.getSeconds();

        if (seconds <= 0) {
            countdownLabel.setText("Phiên đã kết thúc");
            progressBar.setProgress(0);
            countdownTimeline.stop();
            bidFormPanel.setVisible(false);
            bidFormPanel.setManaged(false);
            autoBidFormPanel.setVisible(false);
            autoBidFormPanel.setManaged(false);
            reloadAuction();
        } else {
            long h = remaining.toHours();
            long m = remaining.toMinutesPart();
            long s = remaining.toSecondsPart();
            if (h > 0) {
                countdownLabel.setText(String.format("Thời gian còn lại: %02dh %02dm %02ds", h, m, s));
            } else {
                countdownLabel.setText(String.format("Thời gian còn lại: %02d:%02d", m, s));
            }

            // Fraction of the auction duration remaining.
            double progress = Math.min(1.0, (double) seconds / (a.getDurationMinutes() * 60.0));
            progressBar.setProgress(progress);
        }
    }

    @FXML
    private void handleToggleAutoBid() {
        Auction auction = findAuction();
        if (auction == null) {
            UiEffects.showToast(rootPane, "Không tìm thấy phiên đấu giá", UiEffects.ToastType.ERROR, 2400);
            return;
        }

        if (currentAutoBid != null) {
            try {
                appContext.getGateway().cancelAutoBid(auctionId, currentUser.getId());
                UiEffects.showToast(rootPane, "Đã hủy kích hoạt Auto-Bid thành công", UiEffects.ToastType.SUCCESS, 2000);
                reloadAuction();
            } catch (Exception ex) {
                UiEffects.showToast(rootPane, "Lỗi: " + ex.getMessage(), UiEffects.ToastType.ERROR, 2400);
            }
        } else {
            String maxText = autoBidMaxField.getText();
            String incText = autoBidIncrementField.getText();

            if (maxText == null || maxText.trim().isEmpty()) {
                UIAnimations.shakeField(autoBidMaxField);
                UiEffects.showToast(rootPane, "Vui lòng nhập giá tối đa", UiEffects.ToastType.ERROR, 2000);
                return;
            }

            double maxPrice;
            try {
                maxPrice = Double.parseDouble(maxText.trim());
            } catch (NumberFormatException e) {
                UIAnimations.shakeField(autoBidMaxField);
                UiEffects.showToast(rootPane, "Giá tối đa không hợp lệ", UiEffects.ToastType.ERROR, 2000);
                return;
            }

            double increment = 10.0;
            if (incText != null && !incText.trim().isEmpty()) {
                try {
                    increment = Double.parseDouble(incText.trim());
                } catch (NumberFormatException e) {
                    UIAnimations.shakeField(autoBidIncrementField);
                    UiEffects.showToast(rootPane, "Bước nâng không hợp lệ", UiEffects.ToastType.ERROR, 2000);
                    return;
                }
            }

            if (!Double.isFinite(increment) || increment <= 0) {
                UIAnimations.shakeField(autoBidIncrementField);
                UiEffects.showToast(rootPane, "Bước nâng phải lớn hơn 0", UiEffects.ToastType.ERROR, 2000);
                return;
            }

            double minBid = auction.getCurrentPrice() + increment;
            if (!Double.isFinite(maxPrice) || !Double.isFinite(minBid) || maxPrice < minBid) {
                UIAnimations.shakeField(autoBidMaxField);
                UiEffects.showToast(rootPane, String.format(Locale.US, "Giá tối đa phải lớn hơn hoặc bằng $%,.2f", minBid), UiEffects.ToastType.ERROR, 2400);
                return;
            }

            try {
                appContext.getGateway().registerAutoBid(auctionId, currentUser.getId(), maxPrice, increment);
                UiEffects.showToast(rootPane, "Kích hoạt Auto-Bid thành công!", UiEffects.ToastType.SUCCESS, 2000);
                autoBidMaxField.clear();
                autoBidIncrementField.clear();
                reloadAuction();
            } catch (Exception ex) {
                UiEffects.showToast(rootPane, "Lỗi: " + ex.getMessage(), UiEffects.ToastType.ERROR, 2400);
            }
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
            UIAnimations.successBounce(placeBidButton);
            bidAmountField.clear();
            reloadAuction();
        } else {
            UIAnimations.shakeField(bidAmountField);
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
        bidAmountField.setText(String.format(Locale.US, "%.2f", Math.max(base, a.getCurrentPrice()) + delta));
    }

    @FXML
    private void handleRefresh() {
        UiEffects.runWithLoading(rootPane, 400, this::reloadAuction, () ->
                UiEffects.showToast(rootPane, "Đã cập nhật", UiEffects.ToastType.INFO, 1400));
    }

    @FXML
    private void handleBack() throws IOException {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        com.auction.client.ClientEventManager.clearListeners();
        navigator.showHome(currentUser);
    }

    @FXML
    private void handleToggleTheme() {
        if (rootPane != null && rootPane.getScene() != null) {
            com.auction.ui.ThemeManager.toggle(rootPane.getScene());
        }
    }

    private void reloadAuction() {
        Auction a = findAuction();
        if (a != null) {
            renderAuction(a);
            if (a.isOpen() && a.getEndTime() != null && LocalDateTime.now().isBefore(a.getEndTime())
                    && countdownTimeline.getStatus() != javafx.animation.Animation.Status.RUNNING) {
                startCountdown();
            }
        }
    }

    private Auction findAuction() {
        try {
            return appContext.getGateway().listAuctions().stream()
                    .filter(x -> x.getId().equals(auctionId))
                    .findFirst().orElse(null);
        } catch (com.auction.exception.AuctionException exception) {
            UiEffects.showToast(rootPane, exception.getMessage(), UiEffects.ToastType.ERROR, 2400);
            return null;
        }
    }

    private void setMainImage(String path) {
        javafx.scene.image.Image image = ItemImageHelper.load(path, 440, 340);
        if (image != null && !image.isError()) {
            itemImageView.setImage(image);
            imagePlaceholderLabel.setVisible(false);
        } else {
            itemImageView.setImage(null);
            imagePlaceholderLabel.setVisible(true);
        }
    }
}
