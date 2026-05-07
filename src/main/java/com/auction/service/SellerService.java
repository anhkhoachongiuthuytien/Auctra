package com.auction.service;

import com.auction.dao.AuctionDao;
import com.auction.dao.ItemDao;
import com.auction.enums.ItemType;
import com.auction.exception.AuctionException;
import com.auction.factory.ItemFactory;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Seller;
import com.auction.util.IdGenerator;

public class SellerService {
    private final ItemDao itemDao;
    private final AuctionDao auctionDao;

    public SellerService(ItemDao itemDao, AuctionDao auctionDao) {
        this.itemDao = itemDao;
        this.auctionDao = auctionDao;
    }

    public Item createItem(String type, String name, String description, double startingPrice) {
        return createItem(ItemType.fromString(type), name, description, startingPrice);
    }

    public Item createItem(ItemType type, String name, String description, double startingPrice) {
        String id = IdGenerator.generateId();
        Item item = ItemFactory.createItem(type, id, name, description, startingPrice);
        itemDao.save(item);
        return item;
    }

    public Auction createAuction(Item item, Seller seller) {
        if (item == null) {
            throw new AuctionException("Item not found");
        }
        if (seller == null) {
            throw new AuctionException("Seller not found");
        }

        Auction auction = new Auction(IdGenerator.generateId(), item, seller);
        auctionDao.save(auction);
        return auction;
    }

    public Item getItemById(String itemId) {
        Item item = itemDao.findById(itemId);
        if (item == null) {
            throw new AuctionException("Item not found");
        }
        return item;
    }
}
