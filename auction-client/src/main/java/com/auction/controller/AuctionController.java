package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.enums.AuctionStatus;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.presentation.AuctionListViewModel;
import com.auction.util.UiEffects;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

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

    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> idColumn;
    @FXML private TableColumn<Auction, String> itemColumn;
    @FXML private TableColumn<Auction, String> sellerColumn;
    @FXML private TableColumn<Auction, String> statusColumn;
    @FXML private TableColumn<Auction, Number> priceColumn;
    @FXML private TableColumn<Auction, Void> actionColumn;

    @FXML private TextField bidAmountField;
    @FXML private TextField searchField;

    @FXML private Button refreshButton;
    @FXML private Button finishButton;
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
        configureTable();
        configureFilterAndSearch();
        refreshTable();
        configureActionsVisibility();
        
        com.auction.client.ClientEventManager.addListener(() -> {
            javafx.application.Platform.runLater(() -> {
                try { refreshTable(); } catch (Exception e) {}
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
    }

    private void configureActionsVisibility() {
        boolean isBidder = "Bidder".equals(currentUser.getClass().getSimpleName());
        if (bidAmountField != null) bidAmountField.setDisable(!isBidder);
        if (finishButton != null) {
            finishButton.setVisible(!isBidder);
            finishButton.setManaged(!isBidder);
        }
        actionMessageLabel.setText("Nhấn đúp vào phiên đấu giá để xem chi tiết.");
    }

    // ===== Filter + search =====
    private void configureFilterAndSearch() {
        filteredList = new FilteredList<>(masterList, a -> true);
        auctionTable.setItems(filteredList);
        auctionTable.setPlaceholder(new Label("Không có phiên đấu giá nào trong danh sách"));

        searchField.textProperty().addListener((o, old, val) -> applyFilters());

        // Esc → clear search
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                searchField.clear();
            }
        });

        // Enter trong ô nhập giá → đặt giá nhanh
        if (bidAmountField != null) {
            bidAmountField.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    handlePlaceBid();
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
            b.getStyleClass().removeAll("filter-tab", "filter-tab-active");
            b.getStyleClass().add(b == activeTab ? "filter-tab-active" : "filter-tab");
        }
        applyFilters();
    }

    // ===== Table config =====
    private void configureTable() {
        idColumn.setCellValueFactory(cell -> new SimpleStringProperty(shortId(cell.getValue().getId())));
        itemColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItem().getName()));
        sellerColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSeller().getUsername()));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        priceColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getCurrentPrice()));

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

        priceColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? null : String.format(Locale.US, "$%,.2f", n.doubleValue()));
            }
        });

        if (actionColumn != null) {
            actionColumn.setCellFactory(col -> new TableCell<>() {
                private final Button detailsBtn = new Button("Chi tiết");
                private final Button placeBidBtn = new Button("Đặt giá");
                private final HBox box = new HBox(8, detailsBtn, placeBidBtn);

                {
                    detailsBtn.getStyleClass().addAll("button-ghost", "button-row");
                    placeBidBtn.getStyleClass().addAll("button-success", "button-row");
                    detailsBtn.setOnAction(e -> onDetailsClicked(getTableRow().getItem()));
                    placeBidBtn.setOnAction(e -> onPlaceBidRow(getTableRow().getItem()));
                }

                @Override
                protected void updateItem(Void v, boolean empty) {
                    super.updateItem(v, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        Auction a = getTableRow().getItem();
                        boolean biddable = a.getStatus() == AuctionStatus.RUNNING
                                && "Bidder".equals(currentUser.getClass().getSimpleName());
                        placeBidBtn.setDisable(!biddable);
                        setGraphic(box);
                    }
                }
            });
        }

        auctionTable.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    onDetailsClicked(row.getItem());
                }
            });
            row.selectedProperty().addListener((o, old, sel) -> {
                if (sel && row.getItem() != null) {
                    Auction a = row.getItem();
                    if (selectedAuctionLabel != null) {
                        selectedAuctionLabel.setText("Đã chọn: " + a.getItem().getName()
                                + " • giá hiện tại $" + String.format(Locale.US, "%,.2f", a.getCurrentPrice()));
                    }
                }
            });
            return row;
        });
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

    private void onPlaceBidRow(Auction a) {
        if (a == null) return;
        auctionTable.getSelectionModel().select(a);
        handlePlaceBid();
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
    private void handlePlaceBid() {
        if (bidAmountField == null) return;
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiEffects.showToast(rootPane, "Vui lòng chọn một cuộc đấu giá", UiEffects.ToastType.ERROR, 2000);
            return;
        }
        AuctionListViewModel.ActionResult result =
                viewModel.placeBid(currentUser, selected, bidAmountField.getText());
        if (result.success()) {
            UiEffects.showToast(rootPane,
                    "Đặt thầu thành công: " + bidAmountField.getText(),
                    UiEffects.ToastType.SUCCESS, 2000);
            bidAmountField.clear();
            refreshTable();
        } else {
            UiEffects.showToast(rootPane, result.message(), UiEffects.ToastType.ERROR, 2400);
        }
        actionMessageLabel.setText(result.message());
    }

    @FXML
    private void handleFinishAuction() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiEffects.showToast(rootPane, "Vui lòng chọn cuộc đấu giá cần kết thúc",
                    UiEffects.ToastType.ERROR, 2000);
            return;
        }
        UiEffects.showConfirmDialog(rootPane,
                "Kết thúc cuộc đấu giá",
                "Bạn có chắc chắn muốn kết thúc cuộc đấu giá \""
                        + selected.getItem().getName() + "\"? Hành động này không thể hoàn tác.",
                "Xác nhận",
                true,
                ok -> {
                    if (!ok) return;
                    AuctionListViewModel.ActionResult r = viewModel.finishAuction(selected);
                    if (r.success()) {
                        UiEffects.showToast(rootPane, "Đã kết thúc đấu giá",
                                UiEffects.ToastType.SUCCESS, 1800);
                        refreshTable();
                    } else {
                        UiEffects.showToast(rootPane, r.message(), UiEffects.ToastType.ERROR, 2400);
                    }
                    actionMessageLabel.setText(r.message());
                });
    }

    // ===== Data refresh =====
    private void refreshTable() {
        List<Auction> auctions = viewModel.loadAuctions();
        masterList.setAll(auctions);
        applyFilters();
    }

    private static String shortId(String id) {
        if (id == null) return "";
        return id.length() > 8 ? id.substring(0, 8) + "…" : id;
    }
}
