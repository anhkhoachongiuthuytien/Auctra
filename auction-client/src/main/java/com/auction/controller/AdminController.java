package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.enums.AuctionStatus;
import com.auction.model.auction.Auction;
import com.auction.model.user.Admin;
import com.auction.model.user.User;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AdminController {
    @FXML private StackPane rootPane;
    @FXML private Label welcomeLabel;
    @FXML private Label topBarGreeting;
    @FXML private Label actionMessageLabel;
    @FXML private Label userInitialsLabel;

    @FXML private TextField globalSearchField;

    // Navigation buttons
    @FXML private Button navDashboard;
    @FXML private Button navUsers;
    @FXML private Button navAuctions;

    // Sections
    @FXML private VBox dashboardSection;
    @FXML private VBox usersSection;
    @FXML private VBox auctionsSection;

    // Stats
    @FXML private Label statTotalUsers;
    @FXML private Label statRunningAuctions;
    @FXML private Label statPaidAuctions;
    @FXML private Label statRevenue;

    // Search
    @FXML private TextField userSearchField;
    @FXML private TextField auctionSearchField;

    // User table
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> userIdColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;

    // Auction table
    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> auctionIdColumn;
    @FXML private TableColumn<Auction, String> itemColumn;
    @FXML private TableColumn<Auction, String> sellerColumn;
    @FXML private TableColumn<Auction, String> statusColumn;
    @FXML private TableColumn<Auction, Number> currentPriceColumn;

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
        }
        configureUserTable();
        configureAuctionTable();
        configureSearch();
        refreshData();
        
        com.auction.client.ClientEventManager.addListener(() -> {
            javafx.application.Platform.runLater(() -> {
                try { refreshData(); } catch (Exception e) {}
            });
        });
        
        showInfo("Quản trị viên có thể xem danh sách người dùng và quản lý vòng đời đấu giá.");
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
        userTable.setItems(filteredUsers);
        userTable.setPlaceholder(new Label("Không có người dùng nào"));

        filteredAuctions = new FilteredList<>(auctionMasterList, a -> true);
        auctionTable.setItems(filteredAuctions);
        auctionTable.setPlaceholder(new Label("Không có phiên đấu giá nào"));

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
        Auction auction = auctionTable.getSelectionModel().getSelectedItem();
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
        Auction auction = auctionTable.getSelectionModel().getSelectedItem();
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

    @FXML private void goToHome()       throws IOException { /* admin home = dashboard này */ }
    @FXML private void goToAuctions()   throws IOException { navigator.showAuctionList(currentAdmin); }
    @FXML private void goToMyAuctions() throws IOException { /* đang ở đây */ }
    @FXML private void goToProfile()    throws IOException { navigator.showProfile(currentAdmin); }

    // ========= Table config =========
    private void configureUserTable() {
        userIdColumn.setCellValueFactory(cell -> new SimpleStringProperty(shortId(cell.getValue().getId())));
        usernameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        emailColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEmail()));
        roleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getClass().getSimpleName()));
    }

    private void configureAuctionTable() {
        auctionIdColumn.setCellValueFactory(cell -> new SimpleStringProperty(shortId(cell.getValue().getId())));
        itemColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItem().getName()));
        sellerColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSeller().getUsername()));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        currentPriceColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getCurrentPrice()));

        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("badge", "badge-open", "badge-running",
                        "badge-finished", "badge-paid", "badge-canceled");
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
    }

    private void refreshData() {
        List<User> users = appContext.getGateway().listUsers();
        List<Auction> auctions = appContext.getGateway().listAuctions();
        userMasterList.setAll(users);
        auctionMasterList.setAll(auctions);
        updateStats(users, auctions);
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
