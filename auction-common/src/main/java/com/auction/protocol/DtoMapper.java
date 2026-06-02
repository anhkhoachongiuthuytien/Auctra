package com.auction.protocol;

import com.auction.enums.AuctionStatus;
import com.auction.enums.ItemType;
import com.auction.model.auction.Auction;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
import com.auction.model.item.Other;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Chuyển đổi giữa domain model và DTO dùng cho truyền qua socket.
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    // ===== User ===== //

    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAvatarPath(user.getAvatarPath());

        if (user instanceof Admin) {
            dto.setRole("Admin");
            dto.setDepartment(((Admin) user).getDepartment());
        } else if (user instanceof Seller) {
            dto.setRole("Seller");
            dto.setStoreName(((Seller) user).getStoreName());
            dto.setStoreDescription(((Seller) user).getStoreDescription());
        } else if (user instanceof Bidder) {
            dto.setRole("Bidder");
            dto.setShippingAddress(((Bidder) user).getShippingAddress());
            dto.setPhoneNumber(((Bidder) user).getPhoneNumber());
        } else {
            dto.setRole("Unknown");
        }
        return dto;
    }

    public static User toUser(UserDto dto) {
        if (dto == null) {
            return null;
        }
        User user;
        switch (dto.getRole()) {
            case "Admin":
                Admin admin = new Admin(dto.getId(), dto.getUsername(), dto.getEmail());
                admin.setDepartment(dto.getDepartment());
                user = admin;
                break;
            case "Seller":
                Seller seller = new Seller(dto.getId(), dto.getUsername(), dto.getEmail());
                seller.setStoreName(dto.getStoreName());
                seller.setStoreDescription(dto.getStoreDescription());
                user = seller;
                break;
            case "Bidder":
                Bidder bidder = new Bidder(dto.getId(), dto.getUsername(), dto.getEmail());
                bidder.setShippingAddress(dto.getShippingAddress());
                bidder.setPhoneNumber(dto.getPhoneNumber());
                user = bidder;
                break;
            default:
                throw new IllegalArgumentException("Vai trò không xác định: " + dto.getRole());
        }
        user.setAvatarPath(dto.getAvatarPath());
        return user;
    }

    // ===== Auction ===== //

    public static AuctionDto toDto(Auction auction) {
        if (auction == null) {
            return null;
        }
        AuctionDto dto = new AuctionDto();
        dto.setId(auction.getId());
        dto.setCurrentPrice(auction.getCurrentPrice());
        dto.setStatus(auction.getStatus().name());

        Item item = auction.getItem();
        if (item != null) {
            dto.setItemId(item.getId());
            dto.setItemName(item.getName());
            dto.setItemDescription(item.getDescription());
            dto.setStartingPrice(item.getStartingPrice());
            dto.setImagePath(item.getImagePath());
            dto.setItemType(getItemTypeName(item));
        }

        Seller seller = auction.getSeller();
        if (seller != null) {
            dto.setSellerId(seller.getId());
            dto.setSellerName(seller.getUsername());
            dto.setSellerEmail(seller.getEmail());
        }

        Bidder winner = auction.getWinner();
        if (winner != null) {
            dto.setWinnerId(winner.getId());
            dto.setWinnerName(winner.getUsername());
            dto.setWinnerEmail(winner.getEmail());
        }

        List<BidDto> bidDtos = new ArrayList<>();
        for (BidTransaction bt : auction.getBids()) {
            bidDtos.add(toDto(bt));
        }
        dto.setBids(bidDtos);

        if (auction.getEndTime() != null) {
            dto.setEndTime(auction.getEndTime().toString());
        }

        return dto;
    }

    public static Auction toAuction(AuctionDto dto) {
        if (dto == null) {
            return null;
        }
        Item item = createItem(dto.getItemType(), dto.getItemId(), dto.getItemName(),
                dto.getItemDescription(), dto.getStartingPrice(), dto.getImagePath());

        Seller seller = new Seller(dto.getSellerId(), dto.getSellerName(), dto.getSellerEmail());

        Auction auction = new Auction(dto.getId(), item, seller);

        Bidder winner = null;
        if (dto.getWinnerId() != null) {
            winner = new Bidder(dto.getWinnerId(), dto.getWinnerName(), dto.getWinnerEmail());
        }

        List<BidTransaction> restoredBids = new ArrayList<>();
        if (dto.getBids() != null) {
            for (BidDto bd : dto.getBids()) {
                Bidder bidder = new Bidder(bd.getBidderId(), bd.getBidderName(), null);
                restoredBids.add(new BidTransaction(bidder, bd.getAmount()));
            }
        }

        java.time.LocalDateTime endTime = dto.getEndTime() != null 
                ? java.time.LocalDateTime.parse(dto.getEndTime()) 
                : java.time.LocalDateTime.now().plusMinutes(5);

        auction.restoreState(
                AuctionStatus.valueOf(dto.getStatus()),
                dto.getCurrentPrice(),
                winner,
                restoredBids,
                endTime
        );

        return auction;
    }

    // ===== BidTransaction ===== //

    public static BidDto toDto(BidTransaction bt) {
        if (bt == null) {
            return null;
        }
        BidDto dto = new BidDto();
        if (bt.getBidder() != null) {
            dto.setBidderId(bt.getBidder().getId());
            dto.setBidderName(bt.getBidder().getUsername());
        }
        dto.setAmount(bt.getAmount());
        dto.setBidTime(bt.getBidTime() != null ? bt.getBidTime().toString() : null);
        return dto;
    }

    // ===== Helpers ===== //

    private static String getItemTypeName(Item item) {
        if (item instanceof Art) {
            return "Art";
        }
        if (item instanceof Electronics) {
            return "Electronics";
        }
        if (item instanceof Vehicle) {
            return "Vehicle";
        }
        if (item instanceof Other) {
            return "Other";
        }
        return "Electronics";
    }

    private static Item createItem(String typeName, String id, String name, String desc,
                                   double startingPrice, String imagePath) {
        Item item;
        if ("Art".equalsIgnoreCase(typeName)) {
            item = new Art(id, name, desc, startingPrice);
        } else if ("Vehicle".equalsIgnoreCase(typeName)) {
            item = new Vehicle(id, name, desc, startingPrice);
        } else if ("Other".equalsIgnoreCase(typeName)) {
            item = new Other(id, name, desc, startingPrice);
        } else {
            item = new Electronics(id, name, desc, startingPrice);
        }
        item.setImagePath(imagePath);
        return item;
    }
}
