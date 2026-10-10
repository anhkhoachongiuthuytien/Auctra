package com.auction.client;

import com.auction.protocol.AuctionRequest;
import com.auction.protocol.AuctionResponse;
import com.auction.protocol.RequestType;
import com.auction.server.AuctionRequestHandler;
import com.auction.server.AuctionServerFacade;
import javafx.application.Platform;

public class LocalAuctionClientGateway extends AbstractAuctionClientGateway {
    private final AuctionRequestHandler handler;

    public LocalAuctionClientGateway(AuctionServerFacade facade) { handler = new AuctionRequestHandler(facade); }

    @Override
    protected AuctionResponse exchange(AuctionRequest request) {
        AuctionResponse response = handler.process(request);
        if (response.isSuccess() && isMutation(request.getType())) {
            try { Platform.runLater(ClientEventManager::fireUpdate); }
            catch (IllegalStateException exception) { ClientEventManager.fireUpdate(); }
        }
        return response;
    }

    private static boolean isMutation(RequestType type) {
        return switch (type) {
            case CREATE_AUCTION, START_AUCTION, FINISH_AUCTION, CANCEL_AUCTION, MARK_AUCTION_PAID,
                    PLACE_BID, REGISTER_AUTO_BID, CANCEL_AUTO_BID, UPDATE_USER, UPDATE_ITEM_IMAGE -> true;
            default -> false;
        };
    }
}
