package com.auction.model.auction;
 import java.time.LocalDateTime;
 import com.auction.model.user.Bidder;

public class BidTransaction {
    private Bidder bidder;
    private double amount;
    private LocalDateTime bidTime;
    public BidTransaction( Bidder bidder, double amount){
        this.bidder = bidder;
        this.amount = amount;
        this.bidTime = LocalDateTime.now();

    }
    public Bidder getBidder(){return bidder;}
    public double getAmount(){return amount;}

    public LocalDateTime getBidTime() {return bidTime;}
}