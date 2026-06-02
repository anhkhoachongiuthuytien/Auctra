package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.enums.AuctionStatus;
import com.auction.model.auction.Auction;
import com.auction.model.user.Seller;
import com.auction.ui.UIAnimations;
import com.auction.util.ImageStorage;
import com.auction.util.UiEffects;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import com.auction.ui.BadgeFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class SellerController {
    @FXML private StackPane rootPane;
    @FXML private Label welcomeLabel;
    @FXML private Label actionMessageLabel;
    @FXML private Label userInitialsLabel;
    @FXML private Label statSellerTotal;
    @FXML private Label statSellerRunning;
    @FXML private Label statSellerFinished;
    @FXML private Label statSellerRevenue;

    @FXML private ComboBox<String> itemTypeComboBox;
    @FXML private TextField itemNameField;
    @FXML private TextField itemDescriptionField;
    @FXML private TextField startingPriceField;

    @FXML private FlowPane imagePreviewContainer;
    @FXML private VBox dropZone;
    @FXML private Label selectedImageLabel;
    @FXML private Button clearImageBtn;

    @FXML private javafx.scene.layout.FlowPane sellerAuctionGrid;
    private Auction selectedAuction;

    private AppContext appContext;
    private SceneNavigator navigator;
    private Seller currentSeller;

    // Đường dẫn các file ảnh đang chọn (trước khi copy vào storage)
    private List<Path> pendingImageFiles = new java.util.ArrayList<>();

    public void init(AppContext appContext, SceneNavigator navigator, Seller currentSeller) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.currentSeller = currentSeller;

        welcomeLabel.setText("Trang người bán • " + currentSeller.getUsername() + " (" + currentSeller.getEmail() + ")");
        if (userInitialsLabel != null && currentSeller.getUsername() != null && !currentSeller.getUsername().isBlank()) {
            userInitialsLabel.setText(currentSeller.getUsername().substring(0, 1).toUpperCase(Locale.ROOT));
            com.auction.util.UserImageHelper.setupAvatar(userInitialsLabel, currentSeller.getId(), currentSeller.getAvatarPath());
        }
        itemTypeComboBox.setItems(FXCollections.observableArrayList("Art", "Electronics", "Vehicle", "Other"));
        itemTypeComboBox.getSelectionModel().select("Electronics");
        setupImageDrop();
        updateImagePreviews();
        refreshAuctions();
        
        com.auction.client.ClientEventManager.addListener(() -> {
            javafx.application.Platform.runLater(() -> {
                try { refreshAuctions(); } catch (Exception e) {}
            });
        });
        
        showInfo("Tạo một vật phẩm (kèm ảnh) để mở phiên đấu giá mới.");
    }

    // ===== Image picker =====
    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh sản phẩm (có thể chọn nhiều)");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ảnh (*.png, *.jpg, *.jpeg, *.gif)",
                        "*.png", "*.jpg", "*.jpeg", "*.gif"));
        List<File> files = chooser.showOpenMultipleDialog(rootPane.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                addPendingImage(file.toPath());
            }
            updateImagePreviews();
        }
    }

    @FXML
    private void handleClearImage() {
        pendingImageFiles.clear();
        updateImagePreviews();
    }

    private void updateImagePreviews() {
        if (imagePreviewContainer == null) return;
        imagePreviewContainer.getChildren().clear();

        if (pendingImageFiles.isEmpty()) {
            Label placeholder = new Label("Ảnh");
            placeholder.getStyleClass().add("muted-label");
            
            StackPane box = new StackPane(placeholder);
            box.getStyleClass().add("image-preview-box");
            box.setPrefSize(64, 64);
            box.setMinSize(64, 64);
            
            imagePreviewContainer.getChildren().add(box);
            if (selectedImageLabel != null) {
                selectedImageLabel.setText("Chọn nhiều ảnh hoặc kéo thả vào đây. Ảnh đầu tiên sẽ làm ảnh bìa.");
            }
            if (clearImageBtn != null) clearImageBtn.setDisable(true);
        } else {
            for (int i = 0; i < pendingImageFiles.size(); i++) {
                Path path = pendingImageFiles.get(i);
                final int index = i;
                try {
                    ImageView imgView = new ImageView(new Image(path.toUri().toString(), 56, 56, true, true));
                    imgView.setPreserveRatio(true);

                    // Create delete button
                    Button delBtn = new Button("×");
                    delBtn.getStyleClass().add("button-icon-mini");
                    StackPane.setAlignment(delBtn, javafx.geometry.Pos.TOP_RIGHT);
                    delBtn.setOnAction(e -> {
                        pendingImageFiles.remove(index);
                        updateImagePreviews();
                    });

                    StackPane box = new StackPane(imgView, delBtn);
                    box.getStyleClass().add("image-preview-box");
                    box.setPrefSize(64, 64);
                    box.setMinSize(64, 64);
                    decorateCoverImage(box, index);

                    imagePreviewContainer.getChildren().add(box);
                } catch (Exception ex) {
                    Label errLabel = new Label("ERR");
                    errLabel.getStyleClass().add("muted-label");
                    Button delBtn = new Button("×");
                    delBtn.getStyleClass().add("button-icon-mini");
                    StackPane.setAlignment(delBtn, javafx.geometry.Pos.TOP_RIGHT);
                    delBtn.setOnAction(e -> {
                        pendingImageFiles.remove(index);
                        updateImagePreviews();
                    });

                    StackPane box = new StackPane(errLabel, delBtn);
                    box.getStyleClass().add("image-preview-box");
                    box.setPrefSize(64, 64);
                    box.setMinSize(64, 64);
                    decorateCoverImage(box, index);
                    imagePreviewContainer.getChildren().add(box);
                }
            }
            if (selectedImageLabel != null) {
                selectedImageLabel.setText(pendingImageFiles.size()
                        + " ảnh đã chọn. Ảnh đầu tiên sẽ hiển thị trên thẻ sản phẩm.");
            }
            if (clearImageBtn != null) clearImageBtn.setDisable(false);
        }
    }

    private void decorateCoverImage(StackPane box, int index) {
        if (index != 0) {
            return;
        }
        box.getStyleClass().add("image-preview-cover");
        Label coverBadge = new Label("Bìa");
        coverBadge.getStyleClass().add("image-cover-badge");
        StackPane.setAlignment(coverBadge, javafx.geometry.Pos.BOTTOM_LEFT);
        StackPane.setMargin(coverBadge, new javafx.geometry.Insets(0, 0, 4, 4));
        box.getChildren().add(coverBadge);
    }

    private void addPendingImage(Path path) {
        if (path == null || !isSupportedImage(path)) {
            return;
        }
        Path normalized = path.toAbsolutePath().normalize();
        boolean exists = pendingImageFiles.stream()
                .map(p -> p.toAbsolutePath().normalize())
                .anyMatch(normalized::equals);
        if (!exists) {
            pendingImageFiles.add(normalized);
        }
    }

    private boolean isSupportedImage(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg")
                || name.endsWith(".jpeg") || name.endsWith(".gif");
    }

    private void setupImageDrop() {
        if (dropZone == null) {
            return;
        }
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                if (!dropZone.getStyleClass().contains("drop-zone-active")) {
                    dropZone.getStyleClass().add("drop-zone-active");
                }
            }
            event.consume();
        });
        dropZone.setOnDragExited(event -> dropZone.getStyleClass().remove("drop-zone-active"));
        dropZone.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            if (!files.isEmpty()) {
                for (File file : files) {
                    addPendingImage(file.toPath());
                }
                updateImagePreviews();
                UIAnimations.successBounce(dropZone);
            }
            dropZone.getStyleClass().remove("drop-zone-active");
            event.setDropCompleted(!files.isEmpty());
            event.consume();
        });
    }

    // ===== Create auction =====
    @FXML
    private void handleCreateAuction() {
        List<String> savedPaths = new java.util.ArrayList<>();
        for (Path file : pendingImageFiles) {
            try {
                String savedPath = ImageStorage.copyIntoStorage(file);
                savedPaths.add(savedPath);
            } catch (IOException ex) {
                UiEffects.showToast(rootPane, "Không lưu được ảnh: " + ex.getMessage(),
                        UiEffects.ToastType.ERROR, 2400);
                return;
            }
        }
        String imagePath = savedPaths.isEmpty() ? null : String.join(";", savedPaths);

        try {
            double startingPrice = Double.parseDouble(startingPriceField.getText());
            Auction auction = appContext.getGateway().createAuctionForSeller(
                    currentSeller,
                    itemTypeComboBox.getValue(),
                    itemNameField.getText(),
                    itemDescriptionField.getText(),
                    startingPrice,
                    imagePath
            );
            clearItemForm();
            refreshAuctions();
            if (sellerAuctionGrid != null) {
                UIAnimations.successBounce(sellerAuctionGrid);
            }
            showSuccess("Đã tạo vật phẩm và phiên đấu giá " + auction.getId() + " thành công.");
            UiEffects.showToast(rootPane, "Đã tạo đấu giá mới", UiEffects.ToastType.SUCCESS, 1800);
        } catch (NumberFormatException ex) {
            showError("Giá khởi điểm phải là một số hợp lệ.");
            UiEffects.showToast(rootPane, "Giá khởi điểm không hợp lệ",
                    UiEffects.ToastType.ERROR, 2200);
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
            UiEffects.showToast(rootPane, ex.getMessage(), UiEffects.ToastType.ERROR, 2400);
        }
    }

    // ===== Table actions =====
    @FXML
    private void handleStartAuction() {
        Auction auction = selectedAuction;
        if (auction == null) {
            UiEffects.showToast(rootPane, "Vui lòng chọn đấu giá của bạn trước",
                    UiEffects.ToastType.ERROR, 2000);
            return;
        }
        try {
            appContext.getGateway().startAuction(auction.getId());
            refreshAuctions();
            showSuccess("Phiên đấu giá " + auction.getId() + " đang diễn ra.");
            UiEffects.showToast(rootPane, "Đấu giá đã bắt đầu", UiEffects.ToastType.SUCCESS, 1800);
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
            UiEffects.showToast(rootPane, ex.getMessage(), UiEffects.ToastType.ERROR, 2400);
        }
    }

    @FXML
    private void handleFinishAuction() {
        Auction auction = selectedAuction;
        if (auction == null) {
            UiEffects.showToast(rootPane, "Vui lòng chọn đấu giá của bạn trước",
                    UiEffects.ToastType.ERROR, 2000);
            return;
        }
        UiEffects.showConfirmDialog(rootPane,
                "Kết thúc cuộc đấu giá",
                "Bạn có chắc chắn muốn kết thúc cuộc đấu giá \""
                        + auction.getItem().getName() + "\"?",
                "Xác nhận",
                true,
                ok -> {
                    if (!ok) return;
                    try {
                        appContext.getGateway().finishAuction(auction.getId());
                        refreshAuctions();
                        showSuccess("Đã kết thúc phiên đấu giá " + auction.getId() + ".");
                        UiEffects.showToast(rootPane, "Đã kết thúc đấu giá",
                                UiEffects.ToastType.SUCCESS, 1800);
                    } catch (RuntimeException ex) {
                        showError(ex.getMessage());
                        UiEffects.showToast(rootPane, ex.getMessage(),
                                UiEffects.ToastType.ERROR, 2400);
                    }
                });
    }

    @FXML
    private void handleViewDetail() {
        Auction auction = selectedAuction;
        if (auction == null) {
            UiEffects.showToast(rootPane, "Vui lòng chọn một phiên đấu giá",
                    UiEffects.ToastType.ERROR, 2000);
            return;
        }
        try {
            navigator.showAuctionDetail(auction, currentSeller);
        } catch (IOException ex) {
            UiEffects.showToast(rootPane, ex.getMessage(), UiEffects.ToastType.ERROR, 2400);
        }
    }

    @FXML
    private void handleRefresh() {
        UiEffects.runWithLoading(rootPane, 500, this::refreshAuctions, () ->
                UiEffects.showToast(rootPane, "Dữ liệu đã làm mới", UiEffects.ToastType.INFO, 1600));
        showInfo("Đã làm mới danh sách đấu giá của bạn.");
    }

    @FXML
    private void handleBackToLogin() throws IOException {
        navigator.showLogin();
    }

    @FXML
    private void goToProfile() throws IOException {
        navigator.showProfile(currentSeller);
    }

    @FXML
    private void handleToggleTheme() {
        if (rootPane != null && rootPane.getScene() != null) {
            com.auction.ui.ThemeManager.toggle(rootPane.getScene());
        }
    }

    private void hideAllOverlays() {
        if (sellerAuctionGrid == null) return;
        for (javafx.scene.Node node : sellerAuctionGrid.getChildren()) {
            if (node instanceof StackPane container && container.getChildren().size() > 1) {
                container.getChildren().get(1).setVisible(false);
            }
        }
    }

    private void renderGrid(List<Auction> list) {
        if (sellerAuctionGrid == null) return;
        sellerAuctionGrid.getChildren().clear();
        if (list.isEmpty()) {
            sellerAuctionGrid.getChildren().add(createEmptyState(
                    "Chưa có phiên của bạn",
                    "Tạo phiên đầu tiên bằng form phía trên, kèm ảnh để thẻ sản phẩm nổi bật hơn."));
            return;
        }
        for (Auction a : list) {
            StackPane card = createAuctionCard(a);
            sellerAuctionGrid.getChildren().add(card);
        }
    }

    private VBox createEmptyState(String title, String message) {
        VBox empty = new VBox(8);
        empty.getStyleClass().addAll("empty-state", "empty-state-compact");
        empty.setAlignment(javafx.geometry.Pos.CENTER);

        Label icon = new Label("A");
        icon.getStyleClass().add("empty-state-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-state-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("empty-state-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);

        empty.getChildren().addAll(icon, titleLabel, messageLabel);
        return empty;
    }

    private StackPane createAuctionCard(Auction a) {
        StackPane container = new StackPane();
        container.getStyleClass().add("auction-card-container");

        VBox card = new VBox();
        card.getStyleClass().add("auction-card-item");

        if (a == selectedAuction) {
            card.getStyleClass().add("auction-card-selected");
        }

        // Image Box
        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("auction-card-image-box");

        java.util.List<String> imagePaths = a.getItem().getImagePaths();
        String imagePath = imagePaths.isEmpty() ? null : imagePaths.get(0);
        if (com.auction.util.ImageStorage.exists(imagePath)) {
            try {
                javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(new File(imagePath).toURI().toString(), true)
                );
                imgView.setFitWidth(180);
                imgView.setFitHeight(110);
                imgView.setPreserveRatio(true);
                imageBox.getChildren().add(imgView);
            } catch (Exception ex) {
                Label placeholder = new Label("Ảnh");
                placeholder.getStyleClass().add("auction-card-placeholder");
                imageBox.getChildren().add(placeholder);
            }
        } else {
            Label placeholder = new Label("Ảnh");
            placeholder.getStyleClass().add("auction-card-placeholder");
            imageBox.getChildren().add(placeholder);
        }

        // Status Badge
        Label statusBadge = BadgeFactory.create(a.getStatus());
        HBox badgeContainer = new HBox(statusBadge);
        badgeContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Title
        Label titleLabel = new Label(a.getItem().getName());
        titleLabel.getStyleClass().add("auction-card-title");
        titleLabel.setWrapText(true);

        // Price
        Label priceLabel = new Label(String.format(Locale.US, "$%,.2f", a.getCurrentPrice()));
        priceLabel.getStyleClass().add("auction-card-price");

        // Seller
        Label sellerLabel = new Label("Bởi: bạn");
        sellerLabel.getStyleClass().add("auction-card-seller");

        card.getChildren().addAll(imageBox, badgeContainer, titleLabel, priceLabel, sellerLabel);

        // Action Overlay
        VBox overlay = new VBox(8);
        overlay.setAlignment(javafx.geometry.Pos.CENTER);
        overlay.getStyleClass().add("auction-action-overlay");
        overlay.setVisible(false);

        Label overlayTitle = new Label("Thao tác phiên");
        overlayTitle.getStyleClass().add("auction-overlay-title");

        Button detailBtn = new Button("Xem chi tiết");
        detailBtn.getStyleClass().addAll("button-primary", "button-compact");
        detailBtn.setMaxWidth(Double.MAX_VALUE);
        detailBtn.setOnAction(e -> {
            e.consume();
            selectedAuction = a;
            handleViewDetail();
        });

        Button startBtn = new Button("Bắt đầu");
        startBtn.getStyleClass().addAll("button-success", "button-compact");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setOnAction(e -> {
            e.consume();
            selectedAuction = a;
            handleStartAuction();
        });

        Button finishBtn = new Button("Kết thúc");
        finishBtn.getStyleClass().addAll("button-danger", "button-compact");
        finishBtn.setMaxWidth(Double.MAX_VALUE);
        finishBtn.setOnAction(e -> {
            e.consume();
            selectedAuction = a;
            handleFinishAuction();
        });

        Button closeBtn = new Button("Đóng");
        closeBtn.getStyleClass().addAll("button-ghost", "button-compact");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> {
            e.consume();
            overlay.setVisible(false);
        });

        overlay.getChildren().add(overlayTitle);
        if (a.getStatus() == AuctionStatus.OPEN) {
            overlay.getChildren().add(startBtn);
        } else if (a.getStatus() == AuctionStatus.RUNNING) {
            overlay.getChildren().add(finishBtn);
        }
        overlay.getChildren().addAll(detailBtn, closeBtn);

        container.getChildren().addAll(card, overlay);

        // Interaction
        card.setOnMouseClicked(e -> {
            selectedAuction = a;
            hideAllOverlays();
            overlay.setVisible(true);
        });

        return container;
    }

    private void refreshAuctions() {
        List<Auction> sellerAuctions = appContext.getGateway().listAuctionsForSeller(currentSeller.getId());
        updateStats(sellerAuctions);
        renderGrid(sellerAuctions);
    }

    private void updateStats(List<Auction> auctions) {
        long running = auctions.stream().filter(a -> a.getStatus() == AuctionStatus.RUNNING).count();
        long finished = auctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.FINISHED
                        || a.getStatus() == AuctionStatus.PAID
                        || a.getStatus() == AuctionStatus.CANCELED)
                .count();
        double revenue = auctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.PAID)
                .mapToDouble(Auction::getCurrentPrice)
                .sum();

        if (statSellerTotal != null) statSellerTotal.setText(String.valueOf(auctions.size()));
        if (statSellerRunning != null) statSellerRunning.setText(String.valueOf(running));
        if (statSellerFinished != null) statSellerFinished.setText(String.valueOf(finished));
        if (statSellerRevenue != null) statSellerRevenue.setText(String.format(Locale.US, "$%,.0f", revenue));
    }

    private static String shortId(String id) {
        if (id == null) return "";
        return id.length() > 8 ? id.substring(0, 8) + "…" : id;
    }

    private void clearItemForm() {
        itemNameField.clear();
        itemDescriptionField.clear();
        startingPriceField.clear();
        itemTypeComboBox.getSelectionModel().select("Electronics");
        pendingImageFiles.clear();
        updateImagePreviews();
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
