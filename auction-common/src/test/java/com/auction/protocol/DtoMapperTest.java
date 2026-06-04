package com.auction.protocol;

import com.auction.enums.AuctionStatus;
import com.auction.model.auction.Auction;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.*;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoMapperTest {

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<DtoMapper> constructor = DtoMapper.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (InvocationTargetException e) {
            // expected
        } catch (InstantiationException | IllegalAccessException e) {
            fail("Failed to instantiate DtoMapper");
        }
    }

    @Test
    void testUserToDtoNull() {
        assertNull(DtoMapper.toDto((User) null));
    }

    @Test
    void testUserToDtoAdmin() {
        Admin admin = new Admin("A1", "adminUser", "admin@test.com");
        admin.setDepartment("Security");
        admin.setAvatarPath("/img/admin.png");

        UserDto dto = DtoMapper.toDto(admin);
        assertNotNull(dto);
        assertEquals("A1", dto.getId());
        assertEquals("adminUser", dto.getUsername());
        assertEquals("admin@test.com", dto.getEmail());
        assertEquals("Admin", dto.getRole());
        assertEquals("Security", dto.getDepartment());
        assertEquals("/img/admin.png", dto.getAvatarPath());
    }

    @Test
    void testUserToDtoSeller() {
        Seller seller = new Seller("S1", "sellerUser", "seller@test.com");
        seller.setStoreName("Tech Shop");
        seller.setStoreDescription("We sell electronics");

        UserDto dto = DtoMapper.toDto(seller);
        assertNotNull(dto);
        assertEquals("Seller", dto.getRole());
        assertEquals("Tech Shop", dto.getStoreName());
        assertEquals("We sell electronics", dto.getStoreDescription());
    }

    @Test
    void testUserToDtoBidder() {
        Bidder bidder = new Bidder("B1", "bidderUser", "bidder@test.com");
        bidder.setShippingAddress("123 Street");
        bidder.setPhoneNumber("123456789");

        UserDto dto = DtoMapper.toDto(bidder);
        assertNotNull(dto);
        assertEquals("Bidder", dto.getRole());
        assertEquals("123 Street", dto.getShippingAddress());
        assertEquals("123456789", dto.getPhoneNumber());
    }

    @Test
    void testUserToDtoUnknown() {
        User unknown = new User("U1", "user", "user@test.com") {};
        UserDto dto = DtoMapper.toDto(unknown);
        assertNotNull(dto);
        assertEquals("Unknown", dto.getRole());
    }

    @Test
    void testToUserNull() {
        assertNull(DtoMapper.toUser(null));
    }

    @Test
    void testToUserAdmin() {
        UserDto dto = new UserDto();
        dto.setId("A1");
        dto.setUsername("adminUser");
        dto.setEmail("admin@test.com");
        dto.setRole("Admin");
        dto.setDepartment("HR");
        dto.setAvatarPath("path");

        User user = DtoMapper.toUser(dto);
        assertTrue(user instanceof Admin);
        assertEquals("HR", ((Admin) user).getDepartment());
        assertEquals("path", user.getAvatarPath());
    }

    @Test
    void testToUserSeller() {
        UserDto dto = new UserDto();
        dto.setId("S1");
        dto.setUsername("sellerUser");
        dto.setEmail("seller@test.com");
        dto.setRole("Seller");
        dto.setStoreName("Shop");
        dto.setStoreDescription("Desc");

        User user = DtoMapper.toUser(dto);
        assertTrue(user instanceof Seller);
        assertEquals("Shop", ((Seller) user).getStoreName());
        assertEquals("Desc", ((Seller) user).getStoreDescription());
    }

    @Test
    void testToUserBidder() {
        UserDto dto = new UserDto();
        dto.setId("B1");
        dto.setUsername("bidderUser");
        dto.setEmail("bidder@test.com");
        dto.setRole("Bidder");
        dto.setShippingAddress("Address");
        dto.setPhoneNumber("Phone");

        User user = DtoMapper.toUser(dto);
        assertTrue(user instanceof Bidder);
        assertEquals("Address", ((Bidder) user).getShippingAddress());
        assertEquals("Phone", ((Bidder) user).getPhoneNumber());
    }

    @Test
    void testToUserInvalidRole() {
        UserDto dto = new UserDto();
        dto.setRole("Invalid");
        assertThrows(IllegalArgumentException.class, () -> DtoMapper.toUser(dto));
    }

    @Test
    void testAuctionToDtoNull() {
        assertNull(DtoMapper.toDto((Auction) null));
    }

    @Test
    void testAuctionToDtoAndBack() {
        Art art = new Art("Item1", "Mona Lisa", "Painting", 1000.0);
        art.setImagePath("mona.jpg");
        Seller seller = new Seller("S1", "Leonardo", "leo@da-vinci.com");
        Auction auction = new Auction("Auc1", art, seller);
        
        auction.start();
        Bidder bidder1 = new Bidder("B1", "Michelangelo", "mike@artist.com");
        auction.addBid(new BidTransaction(bidder1, 1100.0));
        
        AuctionDto dto = DtoMapper.toDto(auction);
        assertNotNull(dto);
        assertEquals("Auc1", dto.getId());
        assertEquals(1100.0, dto.getCurrentPrice());
        assertEquals("RUNNING", dto.getStatus());
        assertEquals("Item1", dto.getItemId());
        assertEquals("Mona Lisa", dto.getItemName());
        assertEquals("Art", dto.getItemType());
        assertEquals("mona.jpg", dto.getImagePath());
        assertEquals("Leonardo", dto.getSellerName());
        assertEquals("Michelangelo", dto.getWinnerName());
        assertEquals(1, dto.getBids().size());

        // Restore to Auction
        Auction restored = DtoMapper.toAuction(dto);
        assertNotNull(restored);
        assertEquals(auction.getId(), restored.getId());
        assertEquals(auction.getCurrentPrice(), restored.getCurrentPrice());
        assertEquals(auction.getStatus(), restored.getStatus());
        assertEquals(auction.getItem().getName(), restored.getItem().getName());
        assertTrue(restored.getItem() instanceof Art);
        assertEquals("mona.jpg", restored.getItem().getImagePath());
        assertEquals(auction.getSeller().getUsername(), restored.getSeller().getUsername());
        assertEquals(auction.getWinner().getUsername(), restored.getWinner().getUsername());
        assertEquals(1, restored.getBids().size());
        assertEquals("Michelangelo", restored.getBids().get(0).getBidder().getUsername());
    }

    @Test
    void testAuctionToDtoItemTypes() {
        // Test Vehicle
        Vehicle vehicle = new Vehicle("V1", "Tesla Model 3", "Electric vehicle", 35000.0);
        Seller seller = new Seller("S1", "Seller", "seller@test.com");
        Auction auctionV = new Auction("AucV", vehicle, seller);
        AuctionDto dtoV = DtoMapper.toDto(auctionV);
        assertEquals("Vehicle", dtoV.getItemType());
        Auction restoredV = DtoMapper.toAuction(dtoV);
        assertTrue(restoredV.getItem() instanceof Vehicle);

        // Test Other
        Other other = new Other("O1", "Novel", "Book", 10.0);
        Auction auctionO = new Auction("AucO", other, seller);
        AuctionDto dtoO = DtoMapper.toDto(auctionO);
        assertEquals("Other", dtoO.getItemType());
        Auction restoredO = DtoMapper.toAuction(dtoO);
        assertTrue(restoredO.getItem() instanceof Other);

        // Test Electronics
        Electronics electronics = new Electronics("E1", "iPhone 15", "Phone", 999.0);
        Auction auctionE = new Auction("AucE", electronics, seller);
        AuctionDto dtoE = DtoMapper.toDto(auctionE);
        assertEquals("Electronics", dtoE.getItemType());
        Auction restoredE = DtoMapper.toAuction(dtoE);
        assertTrue(restoredE.getItem() instanceof Electronics);
    }

    @Test
    void testAuctionToDtoFallbackAndNulls() {
        // Auction without item, seller, winner, bids
        Auction auction = new Auction("AucEmpty", null, null);
        AuctionDto dto = DtoMapper.toDto(auction);
        assertNotNull(dto);
        assertNull(dto.getItemId());
        assertNull(dto.getSellerId());
        assertNull(dto.getWinnerId());
        assertTrue(dto.getBids().isEmpty());

        // Map back
        Auction restored = DtoMapper.toAuction(dto);
        assertNotNull(restored.getItem());
        assertTrue(restored.getItem() instanceof Electronics);
        assertNull(restored.getItem().getId());
        assertNull(restored.getSeller().getId()); // default constructor fallback
        assertNull(restored.getWinner());
        assertTrue(restored.getBids().isEmpty());
    }

    @Test
    void testToAuctionNull() {
        assertNull(DtoMapper.toAuction(null));
    }

    @Test
    void testBidToDtoNull() {
        assertNull(DtoMapper.toDto((BidTransaction) null));
    }

    @Test
    void testBidToDtoNoBidder() {
        BidTransaction bt = new BidTransaction(null, 150.0);
        BidDto dto = DtoMapper.toDto(bt);
        assertNotNull(dto);
        assertNull(dto.getBidderId());
        assertNull(dto.getBidderName());
        assertEquals(150.0, dto.getAmount());
    }
}
