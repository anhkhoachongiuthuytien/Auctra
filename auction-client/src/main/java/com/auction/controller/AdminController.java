package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.enums.AuctionStatus;
import com.auction.model.auction.Auction;
import com.auction.model.user.Admin;
import com.auction.model.user.User;
import com.auction.ui.BadgeFactory;
import com.auction.util.ItemImageHelper;
import com.auction.util.UiEffects;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AdminController {
    @FXML private StackPane rootPane;
    @FXML private Label welcomeLabel;
    @FXML private Label topBarGreeting;
    @FXML private Label actionMessageLabel;
    @FXML private Label userInitialsLabel;
    @FXML private Label sidebarUserNameLabel;
    @FXML private Label sidebarUserMetaLabel;

    @FXML private TextField globalSearchField;

    // Navigation buttons
    @FXML private Button navDashboard;
    @FXML private Button navUsers;
    @FXML private Button navAuctions;

    // Sections
    @FXML private VBox dashboardSection;
    @FXML private VBox usersSection;
    @FXML private VBox auctionsSection;
    @FXML private BarChart<String, Number> opsChart;

    // Stats
    @FXML private Label statTotalUsers;
    @FXML private Label statRunningAuctions;
    @FXML private Label statPaidAuctions;
    @FXML private Label statRevenue;

    // Search
    @FXML private TextField userSearchField;
    @FXML private TextField auctionSearchField;

    // User Grid
    @FXML private FlowPane userGrid;

    // Auction Grid
    @FXML private FlowPane auctionGrid;
    private Auction selectedAuction;

    private AppContext appContext;
    private SceneNavigator navigator;
    private Admin currentAdmin;

    private final ObservableList<User> userMasterList = FXCollections.observableArrayList();
    private final ObservableList<Auction> auctionMasterList = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;
    private FilteredList<Auction> filteredAuctions;

    public void init(AppContext appContext, SceneNavigator navigator, Admin currentAdmin) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.currentAdmin = currentAdmin;

        welcomeLabel.setText("Xin chào, " + currentAdmin.getUsername());
        if (topBarGreeting != null) {
            topBarGreeting.setText("Xin chào, " + currentAdmin.getUsername());
        }
        if (userInitialsLabel != null && currentAdmin.getUsername() != null
                && !currentAdmin.getUsername().isBlank()) {
            userInitialsLabel.setText(currentAdmin.getUsername().substring(0, 1).toUpperCase(Locale.ROOT));
            com.auction.util.UserImageHelper.setupAvatar(userInitialsLabel, currentAdmin.getId(), currentAdmin.getAvatarPath());
        }
        if (sidebarUserNameLabel != null) {
            sidebarUserNameLabel.setText(currentAdmin.getUsername());
        }
        if (sidebarUserMetaLabel != null) {
            sidebarUserMetaLabel.setText(currentAdmin.getEmail());
        }
        configureAuctionTable();
        configureSearch();
        refreshData();
        setupOpsChart();
        
        com.auction.client.ClientEventManager.addListener(() -> {
            javafx.application.Platform.runLater(() -> {
                try { refreshData(); } catch (Exception e) {}
            });
        });
        
        showInfo("Quản trị viên có thể xem danh sách người dùng và quản lý vòng đời đấu giá.");
    }

    private void setupOpsChart() {
        if (opsChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Giao dịch");
            series.getData().add(new XYChart.Data<>("T1", 120));
            series.getData().add(new XYChart.Data<>("T2", 280));
            series.getData().add(new XYChart.Data<>("T3", 190));
            series.getData().add(new XYChart.Data<>("T4", 450));
            series.getData().add(new XYChart.Data<>("T5", 340));
            series.getData().add(new XYChart.Data<>("T6", 520));
            opsChart.getData().setAll(series);
        }
    }

    // ========= Section navigation =========
    @FXML
    private void handleShowDashboard() {
        switchSection(navDashboard);
        dashboardSection.setVisible(true);   dashboardSection.setManaged(true);
        usersSection.setVisible(false);      usersSection.setManaged(false);
        auctionsSection.setVisible(false);   auctionsSection.setManaged(false);
    }

    @FXML
    private void handleShowUsers() {
        switchSection(navUsers);
        dashboardSection.setVisible(false);  dashboardSection.setManaged(false);
        usersSection.setVisible(true);       usersSection.setManaged(true);
        auctionsSection.setVisible(false);   auctionsSection.setManaged(false);
    }

    @FXML
    private void handleShowAuctions() {
        switchSection(navAuctions);
        dashboardSection.setVisible(false);  dashboardSection.setManaged(false);
        usersSection.setVisible(false);      usersSection.setManaged(false);
        auctionsSection.setVisible(true);    auctionsSection.setManaged(true);
    }

    private void switchSection(Button active) {
        for (Button b : new Button[]{navDashboard, navUsers, navAuctions}) {
            b.getStyleClass().removeAll("nav-link", "nav-link-active");
            b.getStyleClass().add(b == active ? "nav-link-active" : "nav-link");
        }
    }

    // ========= Search =========
    private void configureSearch() {
        filteredUsers = new FilteredList<>(userMasterList, u -> true);
        filteredUsers.addListener((javafx.collections.ListChangeListener<User>) c -> renderUserGrid());

        filteredAuctions = new FilteredList<>(auctionMasterList, a -> true);
        filteredAuctions.addListener((javafx.collections.ListChangeListener<Auction>) c -> renderGrid());

        userSearchField.textProperty().addListener((o, old, val) -> {
            String q = val == null ? "" : val.trim().toLowerCase(Locale.ROOT);
            filteredUsers.setPredicate(u -> q.isEmpty()
                    || u.getUsername().toLowerCase(Locale.ROOT).contains(q)
                    || u.getEmail().toLowerCase(Locale.ROOT).contains(q));
        });

        auctionSearchField.textProperty().addListener((o, old, val) -> {
            String q = val == null ? "" : val.trim().toLowerCase(Locale.ROOT);
            filteredAuctions.setPredicate(a -> {
                if (q.isEmpty()) return true;
                String item = a.getItem() == null ? "" : a.getItem().getName().toLowerCase(Locale.ROOT);
                String seller = a.getSeller() == null ? "" : a.getSeller().getUsername().toLowerCase(Locale.ROOT);
                return item.contains(q) || seller.contains(q);
            });
        });

        // Global search — apply vào cả hai filtered list
        if (globalSearchField != null) {
            globalSearchField.textProperty().addListener((o, old, val) -> {
                if (userSearchField != null) userSearchField.setText(val);
                if (auctionSearchField != null) auctionSearchField.setText(val);
            });
        }
    }

    // ========= Actions =========
    @FXML
    private void handleRefresh() {
        UiEffects.runWithLoading(rootPane, 500, this::refreshData, () ->
                UiEffects.showToast(rootPane, "Dữ liệu đã làm mới", UiEffects.ToastType.INFO, 1600));
    }

    @FXML
    private void handleCancelAuction() {
        Auction auction = selectedAuction;
        if (auction == null) {
            UiEffects.showToast(rootPane, "Vui lòng chọn một cuộc đấu giá", UiEffects.ToastType.ERROR, 2000);
            return;
        }
        UiEffects.showConfirmDialog(rootPane,
                "Huỷ cuộc đấu giá",
                "Bạn có chắc chắn muốn huỷ cuộc đấu giá \""
                        + auction.getItem().getName() + "\"?",
                "Xác nhận", true,
                ok -> {
                    if (!ok) return;
                    try {
                        appContext.getGateway().cancelAuction(auction.getId());
                        refreshData();
                        showSuccess("Đã huỷ phiên đấu giá " + auction.getId() + ".");
                        UiEffects.showToast(rootPane, "Đã huỷ đấu giá", UiEffects.ToastType.SUCCESS, 1800);
                    } catch (RuntimeException ex) {
                        showError(ex.getMessage());
                        UiEffects.showToast(rootPane, ex.getMessage(), UiEffects.ToastType.ERROR, 2400);
                    }
                });
    }

    @FXML
    private void handleMarkPaid() {
        Auction auction = selectedAuction;
        if (auction == null) {
            UiEffects.showToast(rootPane, "Vui lòng chọn một cuộc đấu giá", UiEffects.ToastType.ERROR, 2000);
            return;
        }
        try {
            appContext.getGateway().markAuctionPaid(auction.getId());
            refreshData();
            showSuccess("Đã đánh dấu đã thanh toán cho phiên " + auction.getId() + ".");
            UiEffects.showToast(rootPane, "Đã đánh dấu PAID", UiEffects.ToastType.SUCCESS, 1800);
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
            UiEffects.showToast(rootPane, ex.getMessage(), UiEffects.ToastType.ERROR, 2400);
        }
    }

    @FXML
    private void handleBackToLogin() throws IOException {
        navigator.showLogin();
    }

    @FXML
    private void handleToggleTheme() {
        if (rootPane != null && rootPane.getScene() != null) {
            com.auction.ui.ThemeManager.toggle(rootPane.getScene());
        }
    }

    @FXML private void goToHome()       throws IOException { /* admin home = dashboard này */ }
    @FXML private void goToAuctions()   throws IOException { navigator.showAuctionList(currentAdmin); }
    @FXML private void goToMyAuctions() throws IOException { /* đang ở đây */ }
    @FXML private void goToProfile()    throws IOException { navigator.showProfile(currentAdmin); }

    // ========= Grid / detail rendering =========
    private void renderUserGrid() {
        if (userGrid == null) return;
        userGrid.getChildren().clear();
        if (filteredUsers == null || filteredUsers.isEmpty()) {
            userGrid.getChildren().add(createEmptyState(
                    "Chưa có người dùng",
                    "Dữ liệu hiện tại không có người dùng nào khớp với tìm kiếm."));
            return;
        }
        for (User u : filteredUsers) {
            VBox card = new VBox(12);
            card.getStyleClass().add("auction-card-item");
            card.setAlignment(javafx.geometry.Pos.CENTER);
            card.setPadding(new javafx.geometry.Insets(20));

            // Avatar
            StackPane avatarPane = new StackPane();
            avatarPane.getStyleClass().add("admin-user-avatar");
            avatarPane.setPrefSize(64, 64);
            avatarPane.setMinSize(64, 64);
            avatarPane.setMaxSize(64, 64);

            String initial = u.getUsername() == null || u.getUsername().isBlank() ? "U" : u.getUsername().substring(0, 1).toUpperCase(Locale.ROOT);
            Label initialLabel = new Label(initial);
            initialLabel.getStyleClass().add("admin-user-avatar-text");
            initialLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            avatarPane.getChildren().add(initialLabel);

            com.auction.util.UserImageHelper.setupAvatar(initialLabel, u.getId(), u.getAvatarPath());

            // Info
            Label nameLabel = new Label(u.getUsername());
            nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
            nameLabel.setWrapText(true);
            nameLabel.setAlignment(javafx.geometry.Pos.CENTER);

            Label emailLabel = new Label(u.getEmail());
            emailLabel.setStyle("-fx-font-size: 12.5px; -fx-text-fill: -text-muted;");
            emailLabel.setWrapText(true);
            emailLabel.setAlignment(javafx.geometry.Pos.CENTER);

            String role = u.getClass().getSimpleName();
            Label roleLabel = new Label(role.toUpperCase(Locale.ROOT));
            roleLabel.getStyleClass().clear();
            if ("ADMIN".equalsIgnoreCase(role)) {
                roleLabel.getStyleClass().add("role-admin");
            } else if ("SELLER".equalsIgnoreCase(role)) {
                roleLabel.getStyleClass().add("role-seller");
            } else {
                roleLabel.getStyleClass().add("role-bidder");
            }

            card.getChildren().addAll(avatarPane, nameLabel, emailLabel, roleLabel);
            card.setOnMouseClicked(e -> showUserDetailPopup(u));

            userGrid.getChildren().add(card);
        }
    }

    private void showUserDetailPopup(User u) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-backdrop");

        VBox card = new VBox(24);
        card.getStyleClass().add("google-modal-card");
        card.setMaxWidth(520);
        card.setPadding(new javafx.geometry.Insets(28));
        StackPane.setAlignment(card, javafx.geometry.Pos.CENTER);

        // Header Avatar and Name
        HBox header = new HBox(18);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        StackPane avatarPane = new StackPane();
        avatarPane.getStyleClass().add("profile-avatar-large");
        avatarPane.setPrefSize(80, 80);
        avatarPane.setMinSize(80, 80);
        avatarPane.setMaxSize(80, 80);

        String initial = u.getUsername() == null || u.getUsername().isBlank() ? "U" : u.getUsername().substring(0, 1).toUpperCase(Locale.ROOT);
        Label avatarLabel = new Label(initial);
        avatarLabel.getStyleClass().add("profile-avatar-text");
        avatarPane.getChildren().add(avatarLabel);
        com.auction.util.UserImageHelper.setupAvatar(avatarLabel, u.getId(), u.getAvatarPath());

        VBox nameRoleBox = new VBox(6);
        Label nameLbl = new Label(u.getUsername());
        nameLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
        
        String role = u.getClass().getSimpleName();
        Label roleLabel = new Label(role.toUpperCase(Locale.ROOT));
        roleLabel.getStyleClass().clear();
        if ("ADMIN".equalsIgnoreCase(role)) {
            roleLabel.getStyleClass().add("role-admin");
        } else if ("SELLER".equalsIgnoreCase(role)) {
            roleLabel.getStyleClass().add("role-seller");
        } else {
            roleLabel.getStyleClass().add("role-bidder");
        }
        
        nameRoleBox.getChildren().addAll(nameLbl, roleLabel);
        header.getChildren().addAll(avatarPane, nameRoleBox);

        // Information fields
        VBox fieldsContainer = new VBox(2);
        fieldsContainer.setStyle("-fx-background-color: -surface-2; -fx-background-radius: 12; -fx-padding: 8;");
        
        addDetailRow(fieldsContainer, "Mã người dùng", u.getId());
        addDetailRow(fieldsContainer, "Email", u.getEmail());
        
        if (u instanceof com.auction.model.user.Bidder bidder) {
            addDetailRow(fieldsContainer, "Số điện thoại", bidder.getPhoneNumber() != null && !bidder.getPhoneNumber().isBlank() ? bidder.getPhoneNumber() : "Chưa cập nhật");
            addDetailRow(fieldsContainer, "Địa chỉ giao hàng", bidder.getShippingAddress() != null && !bidder.getShippingAddress().isBlank() ? bidder.getShippingAddress() : "Chưa cập nhật");
        } else if (u instanceof com.auction.model.user.Seller seller) {
            addDetailRow(fieldsContainer, "Tên cửa hàng", seller.getStoreName() != null && !seller.getStoreName().isBlank() ? seller.getStoreName() : "Chưa cập nhật");
            addDetailRow(fieldsContainer, "Mô tả cửa hàng", seller.getStoreDescription() != null && !seller.getStoreDescription().isBlank() ? seller.getStoreDescription() : "Chưa cập nhật");
        } else if (u instanceof com.auction.model.user.Admin admin) {
            addDetailRow(fieldsContainer, "Phòng ban", admin.getDepartment() != null && !admin.getDepartment().isBlank() ? admin.getDepartment() : "Chưa cập nhật");
        }

        // Close Button
        HBox buttonRow = new HBox(12);
        buttonRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("Đóng");
        closeBtn.getStyleClass().addAll("button-primary", "button-row");
        closeBtn.setPrefWidth(100);
        buttonRow.getChildren().add(closeBtn);

        card.getChildren().addAll(header, fieldsContainer, buttonRow);
        overlay.getChildren().add(card);

        rootPane.getChildren().add(overlay);

        // Transitions (fade in & scale in)
        overlay.setOpacity(0);
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), overlay);
        fadeIn.setToValue(1);

        card.setScaleX(0.9);
        card.setScaleY(0.9);
        javafx.animation.ScaleTransition scaleIn = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(250), card);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        scaleIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        new javafx.animation.ParallelTransition(fadeIn, scaleIn).play();

        Runnable closeModal = () -> {
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), overlay);
            fadeOut.setToValue(0);

            javafx.animation.ScaleTransition scaleOut = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(180), card);
            scaleOut.setToX(0.95);
            scaleOut.setToY(0.95);

            javafx.animation.ParallelTransition fadeScaleOut = new javafx.animation.ParallelTransition(fadeOut, scaleOut);
            fadeScaleOut.setOnFinished(e -> rootPane.getChildren().remove(overlay));
            fadeScaleOut.play();
        };

        closeBtn.setOnAction(e -> closeModal.run());
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                closeModal.run();
            }
        });
    }

    private void addDetailRow(VBox container, String key, String value) {
        HBox row = new HBox();
        row.getStyleClass().add("meta-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 12 8;");

        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("meta-key");
        keyLabel.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 13px;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("meta-value");
        valueLabel.setStyle("-fx-text-fill: -text-primary; -fx-font-weight: bold; -fx-font-size: 13px;");
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(300);

        row.getChildren().addAll(keyLabel, spacer, valueLabel);
        container.getChildren().add(row);
    }

    private void configureAuctionTable() {
        // Replaced by Grid Card View, no table configuration needed
    }

    private void refreshData() {
        List<User> users = appContext.getGateway().listUsers();
        List<Auction> auctions = appContext.getGateway().listAuctions();
        userMasterList.setAll(users);
        auctionMasterList.setAll(auctions);
        updateStats(users, auctions);
        renderGrid();
        renderUserGrid();
    }

    // ========= Grid rendering =========
    // ========= Grid rendering =========
    private void hideAllOverlays() {
        if (auctionGrid == null) return;
        for (javafx.scene.Node node : auctionGrid.getChildren()) {
            if (node instanceof StackPane container && container.getChildren().size() > 1) {
                container.getChildren().get(1).setVisible(false);
            }
        }
    }

    private void renderGrid() {
        if (auctionGrid == null) return;
        auctionGrid.getChildren().clear();
        if (filteredAuctions.isEmpty()) {
            auctionGrid.getChildren().add(createEmptyState(
                    "Chưa có phiên đấu giá",
                    "Dữ liệu hiện tại không có phiên nào khớp với tìm kiếm."));
            return;
        }
        for (Auction a : filteredAuctions) {
            StackPane card = createAuctionCard(a);
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
        javafx.scene.image.ImageView imgView = ItemImageHelper.createImageView(imagePath, 180, 110);
        if (imgView != null) {
            imageBox.getChildren().add(imgView);
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
            try {
                navigator.showAuctionDetail(a, currentAdmin);
            } catch (IOException ex) {
                UiEffects.showToast(rootPane, "Không mở được chi tiết: " + ex.getMessage(), UiEffects.ToastType.ERROR, 2400);
            }
        });

        Button cancelBtn = new Button("Hủy phiên");
        cancelBtn.getStyleClass().addAll("button-danger", "button-compact");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> {
            e.consume();
            selectedAuction = a;
            handleCancelAuction();
        });

        Button paidBtn = new Button("Thanh toán");
        paidBtn.getStyleClass().addAll("button-success", "button-compact");
        paidBtn.setMaxWidth(Double.MAX_VALUE);
        paidBtn.setOnAction(e -> {
            e.consume();
            selectedAuction = a;
            handleMarkPaid();
        });

        Button closeBtn = new Button("Đóng");
        closeBtn.getStyleClass().addAll("button-ghost", "button-compact");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> {
            e.consume();
            overlay.setVisible(false);
        });

        overlay.getChildren().add(overlayTitle);
        if (a.getStatus() == AuctionStatus.OPEN || a.getStatus() == AuctionStatus.RUNNING) {
            overlay.getChildren().add(cancelBtn);
        } else if (a.getStatus() == AuctionStatus.FINISHED) {
            overlay.getChildren().add(paidBtn);
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

    private void selectAuction(Auction a) {
        selectedAuction = a;
        renderGrid();
    }

    private void updateStats(List<User> users, List<Auction> auctions) {
        long running = auctions.stream().filter(a -> a.getStatus() == AuctionStatus.RUNNING).count();
        long paid = auctions.stream().filter(a -> a.getStatus() == AuctionStatus.PAID).count();
        double revenue = auctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.PAID)
                .mapToDouble(Auction::getCurrentPrice)
                .sum();

        statTotalUsers.setText(String.valueOf(users.size()));
        statRunningAuctions.setText(String.valueOf(running));
        statPaidAuctions.setText(String.valueOf(paid));
        statRevenue.setText(String.format(Locale.US, "$%,.0f", revenue));
    }

    private static String shortId(String id) {
        if (id == null) return "";
        return id.length() > 8 ? id.substring(0, 8) + "…" : id;
    }

    private void showInfo(String message) {
        actionMessageLabel.setText(message);
        actionMessageLabel.getStyleClass().removeAll("error-label", "success-label");
        if (!actionMessageLabel.getStyleClass().contains("info-label")) {
            actionMessageLabel.getStyleClass().add("info-label");
        }
        UiEffects.autoHideLabel(actionMessageLabel, 6000);
    }

    private void showSuccess(String message) {
        actionMessageLabel.setText(message);
        actionMessageLabel.getStyleClass().removeAll("error-label", "info-label");
        if (!actionMessageLabel.getStyleClass().contains("success-label")) {
            actionMessageLabel.getStyleClass().add("success-label");
        }
        UiEffects.autoHideLabel(actionMessageLabel, 5000);
    }

    private void showError(String message) {
        actionMessageLabel.setText(message);
        actionMessageLabel.getStyleClass().removeAll("success-label", "info-label");
        if (!actionMessageLabel.getStyleClass().contains("error-label")) {
            actionMessageLabel.getStyleClass().add("error-label");
        }
        UiEffects.autoHideLabel(actionMessageLabel, 6000);
    }
}
