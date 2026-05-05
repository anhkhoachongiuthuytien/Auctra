package com.auction.controller;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import com.auction.model.item.Item;
import com.auction.model.auction.Auction;

public class AuctionController {

    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> colName;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, String> colTime;

    @FXML private Button btnDetail;
    @FXML private Button btnBid;

    @FXML
    public void initialize() {
        colName.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            return new SimpleStringProperty(item != null ? item.getName() : "Không rõ");
        });
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colTime.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().isOpen() ? "Đang mở" : "Đã đóng");
        });

        // Event "Xem chi tiết"
        btnDetail.setOnAction(event -> {
            Auction selectedData = auctionTable.getSelectionModel().getSelectedItem();
            if (selectedData != null) {
                System.out.println("Bạn đang muốn xem: " + selectedData.getItem().getName());
            } else {
                System.out.println("Vui lòng click chọn 1 sản phẩm trong bảng trước!");
            }
        });
    }
}