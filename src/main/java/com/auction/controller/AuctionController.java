package com.auction.controller;
import com.auction.model.user.User;
import com.auction.service.AuctionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import com.auction.model.item.Item;
import com.auction.model.auction.Auction;

public class AuctionController {

    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> colName;
    @FXML private TableColumn<Auction, String> colPrice;
    @FXML private TableColumn<Auction, String> colTime;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private Button btnDetail;
    @FXML private Button btnBid;
    @FXML private Label lblUsername;

    private AuctionService auctionService;
    private ObservableList<Auction> auctionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getItem() != null
                ? cell.getValue().getItem().getName() : "N/A"));

        colPrice.setCellValueFactory(cell ->
                new SimpleStringProperty(String.format("%,.0f VND", cell.getValue().getCurrentPrice())));

        colStatus.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().isOpen() ? "Opening" : "Closed"));

        colTime.setCellValueFactory(cell ->
                new SimpleStringProperty("--"));
        // bind date
        auctionTable.setItems(auctionList);
        auctionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean selected = newVal != null; // nếu khác null thì true
            btnDetail.setDisable(!selected); // nếu ko ấn btn thì btn bị vô hiệu
            btnBid.setDisable(!selected ||!newVal.isOpen()); // neu ko chọn or phiên đấu giá dong thi nut bi vo hieu
            // LoadAuction();
        });
        User currentUser = new
    }
}