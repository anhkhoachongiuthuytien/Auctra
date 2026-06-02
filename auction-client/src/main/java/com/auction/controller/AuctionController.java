package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.enums.AuctionStatus;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.presentation.AuctionListViewModel;
import com.auction.ui.BadgeFactory;
import com.auction.util.UiEffects;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AuctionController {

    // ===== FXML bindings =====
    @FXML private StackPane rootPane;
    @FXML private Label welcomeLabel;
    @FXML private Label summaryLabel;
    @FXML private Label actionMessageLabel;
    @FXML private Label selectedAuctionLabel;
    @FXML private Label userInitialsLabel;

    @FXML private FlowPane auctionGrid;

    @FXML private TextField topbarSearchField;
    @FXML private TextField searchField;

    @FXML private Button refreshButton;
    @FXML private Button tabAll;
    @FXML private Button tabActive;
    @FXML private Button tabFinished;

    // ===== State =====
    private AppContext appContext;
    private SceneNavigator navigator;
    private User currentUser;
    private AuctionListViewModel viewModel;

    private final javafx.collections.ObservableList<Auction> masterList = FXCollections.observableArrayList();
    private FilteredList<Auction> filteredList;
    private String activeFilter = "ACTIVE"; // ALL | ACTIVE | FINISHED

    public void init(AppContext appContext, SceneNavigator navigator, User currentUser) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.currentUser = currentUser;
        this.viewModel = new AuctionListViewModel(appContext.getGateway());

        setupUserBadge();
        configureFilterAndSearch();
        refreshTable();
        configureActionsVisibility();

        com.auction.client.ClientEventManager.addListener(() -> {
            javafx.application.Platform.runLater(() -> {
                try {
                    refreshTable();
                } catch (Exception e) {
                    // Ignore background refresh errors
                }
            });
        });

        welcomeLabel.setText(viewModel.getWelcomeMessage(currentUser));
    }

    // ===== Role / user setup =====
    private void setupUserBadge() {
        String name = currentUser.getUsername();
        userInitialsLabel.setText(name == null || name.isBlank()
                ? "U"
                : name.substring(0, 1).toUpperCase(Locale.ROOT));
        com.auction.util.UserImageHelper.setupAvatar(userInitialsLabel, currentUser.getId(), currentUser.getAvatarPath());
    }

    private void configureActionsVisibility() {
        actionMessageLabel.setText("Bấm vào hình ảnh hoặc thẻ sản phẩm để thực hiện đặt giá hoặc xem chi tiết.");
    }

    // ===== Filter + search =====
    private void configureFilterAndSearch() {
        filteredList = new FilteredList<>(masterList, a -> true);
        filteredList.addListener((javafx.collections.ListChangeListener<Auction>) c -> renderGrid());

        searchField.textProperty().addListener((o, old, val) -> applyFilters());
        if (topbarSearchField != null) {
            topbarSearchField.textProperty().addListener((o, old, val) -> {
                if (!java.util.Objects.equals(searchField.getText(), val)) {
                    searchField.setText(val);
                }
            });
            searchField.textProperty().addListener((o, old, val) -> {
                if (!java.util.Objects.equals(topbarSearchField.getText(), val)) {
                    topbarSearchField.setText(val);
                }
            });
        }

        // Esc → clear search
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                searchField.clear();
            }
        });
        if (topbarSearchField != null) {
            topbarSearchField.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    topbarSearchField.clear();
                }
            });
        }
    }

    private void applyFilters() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        filteredList.setPredicate(a -> {
            if (!matchesFilter(a)) return false;
            if (q.isEmpty()) return true;
            String item = a.getItem() == null ? "" : a.getItem().getName().toLowerCase(Locale.ROOT);
            String seller = a.getSeller() == null ? "" : a.getSeller().getUsername().toLowerCase(Locale.ROOT);
            return item.contains(q) || seller.contains(q);
        });
        summaryLabel.setText("Hiển thị " + filteredList.size() + " / " + masterList.size() + " phiên đấu giá.");
    }

    private boolean matchesFilter(Auction a) {
        AuctionStatus s = a.getStatus();
        return switch (activeFilter) {
            case "ALL"      -> true;
            case "ACTIVE"   -> s == AuctionStatus.OPEN || s == AuctionStatus.RUNNING;
            case "FINISHED" -> s == AuctionStatus.FINISHED || s == AuctionStatus.PAID || s == AuctionStatus.CANCELED;
            default -> true;
        };
    }

    @FXML private void handleFilterAll()      { switchFilter("ALL",      tabAll); }
    @FXML private void handleFilterActive()   { switchFilter("ACTIVE",   tabActive); }
    @FXML private void handleFilterFinished() { switchFilter("FINISHED", tabFinished); }

    private void switchFilter(String name, Button activeTab) {
        activeFilter = name;
        for (Button b : new Button[]{tabAll, tabActive, tabFinished}) {
            if (b != null) {
                b.getStyleClass().removeAll("filter-tab", "filter-tab-active");
                b.getStyleClass().add(b == activeTab ? "filter-tab-active" : "filter-tab");
            }
        }
        applyFilters();
    }

    // ===== Grid rendering =====
    private void renderGrid() {
        auctionGrid.getChildren().clear();
        if (filteredList.isEmpty()) {
            auctionGrid.getChildren().add(createEmptyState(
                    "Chưa có phiên phù hợp",
                    "Thử đổi bộ lọc hoặc xoá nội dung tìm kiếm để xem thêm phiên đấu giá."));
            return;
        }
        for (Auction a : filteredList) {
            VBox card = createAuctionCard(a);
            auctionGrid.getChildren().add(card);
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

    private VBox createAuctionCard(Auction a) {
        VBox card = new VBox();
        card.getStyleClass().add("auction-card-item");

        // Image Box
        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("auction-card-image-box");

        java.util.List<String> imagePaths = a.getItem().getImagePaths();
        String imagePath = imagePaths.isEmpty() ? null : imagePaths.get(0);
        if (com.auction.util.ImageStorage.exists(imagePath)) {
            try {
                ImageView imgView = new ImageView(new Image(new File(imagePath).toURI().toString(), true));
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
        Label sellerLabel = new Label("Bởi: " + a.getSeller().getUsername());
        sellerLabel.getStyleClass().add("auction-card-seller");

        card.getChildren().addAll(imageBox, badgeContainer, titleLabel, priceLabel, sellerLabel);

        // Click interaction
        card.setOnMouseClicked(e -> onDetailsClicked(a));

        return card;
    }

    private void onDetailsClicked(Auction a) {
        if (a == null) return;
        try {
            navigator.showAuctionDetail(a, currentUser);
        } catch (IOException ex) {
            UiEffects.showToast(rootPane, "Không mở được màn hình chi tiết: " + ex.getMessage(),
                    UiEffects.ToastType.ERROR, 2400);
        }
    }

    // ===== Actions =====
    @FXML
    private void handleRefresh() {
        UiEffects.runWithLoading(rootPane, 600, this::refreshTable, () ->
                UiEffects.showToast(rootPane, "Dữ liệu đã làm mới", UiEffects.ToastType.INFO, 1600));
    }

    @FXML
    private void handleBackToLogin() throws IOException {
        navigator.showLogin();
    }

    @FXML private void goToHome()       throws IOException { navigator.showHome(currentUser); }
    @FXML private void goToAuctions()   throws IOException { /* đang ở đây */ }
    @FXML private void goToMyAuctions() throws IOException { navigator.showMyAuctions(currentUser); }
    @FXML private void goToProfile()    throws IOException { navigator.showProfile(currentUser); }

    @FXML
    private void handleToggleTheme() {
        if (rootPane != null && rootPane.getScene() != null) {
            com.auction.ui.ThemeManager.toggle(rootPane.getScene());
        }
    }

    // ===== Data refresh =====
    private void refreshTable() {
        List<Auction> auctions = viewModel.loadAuctions();
        masterList.setAll(auctions);
        applyFilters();
    }
}
