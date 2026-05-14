package com.auction.app;

import com.auction.client.AuctionClientGateway;
import com.auction.client.LocalAuctionClientGateway;
import com.auction.server.AuctionServerFacade;
import com.auction.server.ServerContext;

public class AppContext {
    private final AuctionClientGateway gateway;

    public AppContext() {
        ServerContext serverContext = new ServerContext("jdbc:sqlite:auction-system.db");
        AuctionServerFacade serverFacade = new AuctionServerFacade(serverContext);
        this.gateway = new LocalAuctionClientGateway(serverFacade);
    }

    public AuctionClientGateway getGateway() {
        return gateway;
    }
}
