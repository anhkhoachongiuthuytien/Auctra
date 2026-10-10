package com.auction.server;

import com.auction.exception.AuthorizationException;
import com.auction.exception.AuctionException;
import com.auction.exception.ValidationException;
import com.auction.model.auction.Auction;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.protocol.AuctionEvent;
import com.auction.protocol.AuctionRequest;
import com.auction.protocol.AuctionResponse;
import com.auction.protocol.DtoMapper;
import com.auction.protocol.ErrorCode;
import com.auction.protocol.ProtocolErrors;
import com.auction.protocol.RequestType;
import java.util.ArrayList;

/** The same authenticated request boundary is used by LOCAL and SOCKET gateways. */
public class AuctionRequestHandler {
    private final AuctionServerFacade facade;
    private final SessionManager sessions;

    public AuctionRequestHandler(AuctionServerFacade facade) {
        this.facade = facade;
        this.sessions = new SessionManager(facade);
    }

    public User requireUser(String token) { return sessions.requireUser(token); }

    public AuctionResponse process(AuctionRequest request) {
        try {
            if (request == null || request.getType() == null) { throw new ValidationException("Yêu cầu không hợp lệ"); }
            return dispatch(request);
        } catch (AuctionException exception) {
            return AuctionResponse.error(ProtocolErrors.codeFor(exception), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return AuctionResponse.error(ErrorCode.VALIDATION, "Tham số không hợp lệ: " + exception.getMessage());
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            return AuctionResponse.error(ErrorCode.INTERNAL, "Server không thể xử lý yêu cầu; vui lòng thử lại");
        }
    }

    private AuctionResponse dispatch(AuctionRequest request) {
        RequestType type = request.getType();
        String token = request.get("sessionToken");
        switch (type) {
            case LOGIN: {
                // A failed login must not leave the previously logged-in identity active.
                sessions.revoke(token);
                User user = facade.login(request.get("email"), request.get("password"));
                return AuctionResponse.authenticated(DtoMapper.toDto(user), sessions.issue(user), null);
            }
            case REGISTER: {
                User user = facade.register(request.get("username"), request.get("email"),
                        request.get("password"), request.get("role"));
                sessions.revoke(token);
                broadcast("USER_REGISTERED", null);
                String recoveryCode = user.getRecoveryCode();
                user.setRecoveryCode(null);
                return AuctionResponse.authenticated(DtoMapper.toDto(user), sessions.issue(user), recoveryCode);
            }
            case GET_REGISTRATION_ROLES:
                return AuctionResponse.ok(new ArrayList<>(facade.getAvailableRegistrationRoles()));
            case RESET_PASSWORD:
                facade.resetPassword(request.get("email"), request.get("recoveryCode"), request.get("newPassword"));
                return AuctionResponse.ok();
            case LOGOUT:
                sessions.revoke(token);
                return AuctionResponse.ok();
            default:
                break;
        }
        User actor = sessions.requireUser(token);
        String auctionId = request.get("auctionId");
        switch (type) {
            case SUBSCRIBE_UPDATES:
                return AuctionResponse.ok();
            case LIST_AUCTIONS:
                return AuctionResponse.ok(new ArrayList<>(facade.listAuctions().stream().map(DtoMapper::toDto).toList()));
            case LIST_AUCTIONS_FOR_SELLER:
                requireRole(actor, Seller.class);
                requireSelf(actor, request.get("sellerId"));
                return AuctionResponse.ok(new ArrayList<>(facade.listAuctionsForSeller(actor.getId()).stream()
                        .map(DtoMapper::toDto).toList()));
            case CREATE_AUCTION: {
                requireRole(actor, Seller.class);
                requireSelf(actor, request.get("sellerId"));
                int duration = request.get("durationMinutes") == null ? 5 : Integer.parseInt(request.get("durationMinutes"));
                Auction auction = facade.createAuctionForSeller((Seller) actor, request.get("itemType"),
                        request.get("itemName"), request.get("itemDescription"), request.getDouble("startingPrice"),
                        request.get("imagePath"), duration);
                broadcast("AUCTION_CREATED", auction.getId());
                return AuctionResponse.ok(DtoMapper.toDto(auction));
            }
            case UPDATE_ITEM_IMAGE: {
                boolean ownsItem = actor instanceof Seller && facade.listAuctionsForSeller(actor.getId()).stream()
                        .anyMatch(a -> a.getItem().getId().equals(request.get("itemId")));
                if (!(actor instanceof Admin) && !ownsItem) { throw denied(); }
                facade.updateItemImagePath(request.get("itemId"), request.get("imagePath"));
                broadcast("AUCTION_IMAGE_UPDATED", null);
                return AuctionResponse.ok();
            }
            case START_AUCTION:
                requireOwnerOrAdmin(actor, auctionId);
                facade.startAuction(auctionId);
                broadcast("AUCTION_STARTED", auctionId);
                return AuctionResponse.ok();
            case FINISH_AUCTION:
                requireOwnerOrAdmin(actor, auctionId);
                facade.finishAuction(auctionId);
                broadcast("AUCTION_FINISHED", auctionId);
                return AuctionResponse.ok();
            case CANCEL_AUCTION:
                requireOwnerOrAdmin(actor, auctionId);
                facade.cancelAuction(auctionId);
                broadcast("AUCTION_CANCELED", auctionId);
                return AuctionResponse.ok();
            case MARK_AUCTION_PAID:
                requireRole(actor, Admin.class);
                facade.markAuctionPaid(auctionId);
                broadcast("AUCTION_PAID", auctionId);
                return AuctionResponse.ok();
            case PLACE_BID:
                requireRole(actor, Bidder.class);
                requireSelf(actor, request.get("bidderId"));
                facade.placeBid(auctionId, (Bidder) actor, request.getDouble("amount"));
                broadcast("NEW_BID", auctionId);
                return AuctionResponse.ok();
            case LIST_USERS:
                requireRole(actor, Admin.class);
                return AuctionResponse.ok(new ArrayList<>(facade.listUsers().stream().map(DtoMapper::toDto).toList()));
            case REGISTER_AUTO_BID:
                requireRole(actor, Bidder.class);
                requireSelf(actor, request.get("bidderId"));
                facade.registerAutoBid(auctionId, actor.getId(), request.getDouble("maxPrice"), request.getDouble("increment"));
                broadcast("NEW_BID", auctionId);
                return AuctionResponse.ok();
            case CANCEL_AUTO_BID:
                requireRole(actor, Bidder.class);
                requireSelf(actor, request.get("bidderId"));
                facade.cancelAutoBid(auctionId, actor.getId());
                broadcast("NEW_BID", auctionId);
                return AuctionResponse.ok();
            case GET_AUTO_BID_STATUS:
                requireRole(actor, Bidder.class);
                requireSelf(actor, request.get("bidderId"));
                return AuctionResponse.ok(facade.getAutoBid(auctionId, actor.getId()));
            case UPDATE_USER: {
                requireSelf(actor, request.get("userId"));
                User user = facade.updateUser(actor.getId(), request.get("username"), request.get("email"),
                        request.get("shippingAddress"), request.get("phoneNumber"), request.get("storeName"),
                        request.get("storeDescription"), request.get("department"), request.get("avatarPath"));
                broadcast("USER_UPDATED", null);
                return AuctionResponse.ok(DtoMapper.toDto(user));
            }
            default:
                throw new ValidationException("Loại yêu cầu không được hỗ trợ");
        }
    }

    private void requireOwnerOrAdmin(User actor, String auctionId) {
        Auction auction = facade.getAuction(auctionId);
        if (!(actor instanceof Admin) && (!(actor instanceof Seller) || !actor.getId().equals(auction.getSeller().getId()))) {
            throw denied();
        }
    }

    private static void requireRole(User actor, Class<? extends User> role) {
        if (!role.isInstance(actor)) { throw denied(); }
    }

    private static void requireSelf(User actor, String id) {
        if (id == null || !actor.getId().equals(id)) { throw denied(); }
    }

    private static AuthorizationException denied() { return new AuthorizationException("Bạn không có quyền thực hiện thao tác này"); }

    private static void broadcast(String type, String id) { BroadcastManager.broadcast(new AuctionEvent(type, id)); }
}
