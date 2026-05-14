package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.model.auction.Auction;
import com.auction.model.user.Seller;
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
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

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

    @FXML private ComboBox<String> itemTypeComboBox;
    @FXML private TextField itemNameField;
    @FXML private TextField itemDescriptionField;
    @FXML private TextField startingPriceField;

    @FXML private StackPane imagePreviewPane;
    @FXML private ImageView imagePreview;
    @FXML private Label imagePlaceholderLabel;
    @FXML private Label selectedImageLabel;
    @FXML private Button clearImageBtn;

    @FXML private TableView<Auction> sellerAuctionTable;
    @FXML private TableColumn<Auction, String> auctionIdColumn;
    @FXML private TableColumn<Auction, String> itemNameColumn;
    @FXML private TableColumn<Auction, String> statusColumn;
    @FXML private TableColumn<Auction, Number> currentPriceColumn;

    private AppContext appContext;
    private SceneNavigator navigator;
    private Seller currentSeller;

    // Đường dẫn file ảnh đang chọn (trước khi copy vào storage)
    private Path pendingImageFile;

    public void init(AppContext appContext, SceneNavigator navigator, Seller currentSeller) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.currentSeller = currentSeller;

        welcomeLabel.setText("Trang người bán • " + currentSeller.getUsername() + " (" + currentSeller.getEmail() + ")");
        if (userInitialsLabel != null && currentSeller.getUsername() != null && !currentSeller.getUsername().isBlank()) {
            userInitialsLabel.setText(currentSeller.getUsername().substring(0, 1).toUpperCase(Locale.ROOT));
        }
        itemTypeComboBox.setItems(FXCollections.observableArrayList("Art", "Electronics", "Vehicle"));
        itemTypeComboBox.getSelectionModel().select("Electronics");
        configureTable();
        updateImagePreview();
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
        chooser.setTitle("Chọn ảnh sản phẩm");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ảnh (*.png, *.jpg, *.jpeg, *.gif)",
                        "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            pendingImageFile = file.toPath();
            updateImagePreview();
        }
    }

    @FXML
    private void handleClearImage() {
        pendingImageFile = null;
        updateImagePreview();
    }

    private void updateImagePreview() {
        if (imagePreview == null) return; // image UI not present in current layout
        if (pendingImageFile != null && java.nio.file.Files.exists(pendingImageFile)) {
            try {
                imagePreview.setImage(new Image(pendingImageFile.toUri().toString(), 140, 140, true, true));
                if (imagePlaceholderLabel != null) imagePlaceholderLabel.setVisible(false);
                if (selectedImageLabel != null) selectedImageLabel.setText(pendingImageFile.getFileName().toString());
                if (clearImageBtn != null) clearImageBtn.setDisable(false);
            } catch (Exception ex) {
                imagePreview.setImage(null);
                if (imagePlaceholderLabel != null) imagePlaceholderLabel.setVisible(true);
                if (selectedImageLabel != null) selectedImageLabel.setText("Không đọc được ảnh");
            }
        } else {
            imagePreview.setImage(null);
            if (imagePlaceholderLabel != null) imagePlaceholderLabel.setVisible(true);
            if (selectedImageLabel != null) selectedImageLabel.setText("");
            if (clearImageBtn != null) clearImageBtn.setDisable(true);
        }
    }

    // ===== Create auction =====
    @FXML
    private void handleCreateAuction() {
        String imagePath = null;
        if (pendingImageFile != null) {
            try {
                imagePath = ImageStorage.copyIntoStorage(pendingImageFile);
            } catch (IOException ex) {
                UiEffects.showToast(rootPane, "Không lưu được ảnh: " + ex.getMessage(),
                        UiEffects.ToastType.ERROR, 2400);
                return;
            }
        }

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
        Auction auction = sellerAuctionTable.getSelectionModel().getSelectedItem();
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
        Auction auction = sellerAuctionTable.getSelectionModel().getSelectedItem();
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
        Auction auction = sellerAuctionTable.getSelectionModel().getSelectedItem();
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

    private void configureTable() {
        auctionIdColumn.setCellValueFactory(cell -> new SimpleStringProperty(shortId(cell.getValue().getId())));
        itemNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItem().getName()));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        currentPriceColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getCurrentPrice()));

        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("badge", "badge-open", "badge-running", "badge-finished",
                        "badge-paid", "badge-canceled");
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status);
                    getStyleClass().add("badge");
                    getStyleClass().add(switch (status) {
                        case "OPEN"     -> "badge-open";
                        case "RUNNING"  -> "badge-running";
                        case "FINISHED" -> "badge-finished";
                        case "PAID"     -> "badge-paid";
                        case "CANCELED" -> "badge-canceled";
                        default         -> "badge-open";
                    });
                }
            }
        });

        currentPriceColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? null : String.format(Locale.US, "$%,.2f", n.doubleValue()));
            }
        });

        // Double-click row → mở detail
        sellerAuctionTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Auction> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    try { navigator.showAuctionDetail(row.getItem(), currentSeller); }
                    catch (IOException ignore) { }
                }
            });
            return row;
        });
    }

    private void refreshAuctions() {
        List<Auction> sellerAuctions = appContext.getGateway().listAuctionsForSeller(currentSeller.getId());
        sellerAuctionTable.setItems(FXCollections.observableArrayList(sellerAuctions));
        sellerAuctionTable.setPlaceholder(new javafx.scene.control.Label("Bạn chưa có phiên đấu giá nào"));
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
        pendingImageFile = null;
        updateImagePreview();
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
