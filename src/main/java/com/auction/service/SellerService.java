package com.auction.service;
import com.auction.dao.ItemDao;
import com.auction.model.item.Item;
import com.auction.model.user.Seller;
import com.auction.model.auction.Auction;

public class SellerService {
    private ItemDao itemDao;
    private AuctionService auctionService;

    public SellerService(ItemDao itemDao, AuctionService auctionService) {
        this.itemDao = itemDao;
        this.auctionService = auctionService;
    }
    public Item createItem(String name, double price) {
        Item newItem = new Item(name, price);
        itemDao.save(newItem);
        return newItem;
    }
    public Auction createAuction(Item item, Seller seller) {
        return auctionService.createAuction(item,seller);
    }
}