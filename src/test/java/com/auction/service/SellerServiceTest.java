package com.auction.service;

import com.auction.dao.memory.InMemoryAuctionDao;
import com.auction.dao.memory.InMemoryItemDao;
import com.auction.enums.ItemType;
import com.auction.exception.AuctionException;
import com.auction.exception.ValidationException;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SellerServiceTest {

    private SellerService sellerService;
    private Seller seller;

    @BeforeEach
    void setUp() {
        sellerService = new SellerService(new InMemoryItemDao(), new InMemoryAuctionDao());
        seller = new Seller("S001", "seller", "seller@test.com");
    }

    // Seller phải tạo được item hợp lệ với id và thông tin cơ bản đầy đủ.
    @Test
    void testCreateItemSuccess() {
        Item item = sellerService.createItem(ItemType.ART, "Painting", "Oil painting", 500.0);

        assertNotNull(item.getId());
        assertEquals("Painting", item.getName());
    }

    // Giá khởi điểm không hợp lệ phải bị tầng service từ chối.
    @Test
    void testCreateItemWithInvalidPriceThrowsValidationException() {
        assertThrows(ValidationException.class, () ->
                sellerService.createItem(ItemType.ART, "Painting", "Oil painting", 0));
    }

    // Khi có item hợp lệ, seller phải tạo được auction gắn đúng item và người bán.
    @Test
    void testCreateAuctionSuccess() {
        Item item = sellerService.createItem(ItemType.VEHICLE, "Sedan", "Used sedan", 10000.0);

        Auction auction = sellerService.createAuction(item, seller);

        assertEquals(item, auction.getItem());
        assertEquals(seller, auction.getSeller());
    }

    // Không được tạo auction nếu item đầu vào là null.
    @Test
    void testCreateAuctionWithNullItemThrowsAuctionException() {
        assertThrows(AuctionException.class, () ->
                sellerService.createAuction(null, seller));
    }
}
