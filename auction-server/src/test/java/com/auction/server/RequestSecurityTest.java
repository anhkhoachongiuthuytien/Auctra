package com.auction.server;

import com.auction.dao.sqlite.SqliteUserDao;
import com.auction.db.DatabaseManager;
import com.auction.exception.AuthenticationException;
import com.auction.model.user.User;
import com.auction.protocol.AuctionRequest;
import com.auction.protocol.AuctionResponse;
import com.auction.protocol.ErrorCode;
import com.auction.protocol.RequestType;
import com.auction.protocol.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestSecurityTest {
    @TempDir Path directory;
    private ServerContext context;
    private AuctionServerFacade facade;
    private AuctionRequestHandler handler;
    private String jdbcUrl;

    @BeforeEach
    void setUp() {
        jdbcUrl = "jdbc:sqlite:" + directory.resolve("security.db");
        context = new ServerContext(jdbcUrl, false);
        facade = new AuctionServerFacade(context);
        handler = new AuctionRequestHandler(facade);
    }

    private AuctionResponse register(String username, String role) {
        AuctionResponse response = handler.process(new AuctionRequest(RequestType.REGISTER).put("username", username)
                .put("email", username + "@test.local").put("password", "password123").put("role", role));
        assertTrue(response.isSuccess(), response.getMessage());
        return response;
    }

    @Test
    void sensitiveOperationsRequireAuthentication() {
        for (RequestType type : new RequestType[] {RequestType.LIST_USERS, RequestType.PLACE_BID,
                RequestType.UPDATE_USER, RequestType.FINISH_AUCTION, RequestType.REGISTER_AUTO_BID,
                RequestType.GET_AUTO_BID_STATUS, RequestType.SUBSCRIBE_UPDATES}) {
            assertEquals(ErrorCode.AUTHENTICATION, handler.process(new AuctionRequest(type)).getErrorCode());
        }
    }

    @Test
    void bidAndProfileCannotImpersonateAnotherUserAndUserListIsAdminOnly() {
        AuctionResponse owner = register("owner", "Bidder");
        AuctionResponse other = register("other", "Bidder");
        String token = owner.getSessionToken();
        String otherId = ((UserDto) other.getData()).getId();
        AuctionResponse bid = handler.process(new AuctionRequest(RequestType.PLACE_BID).put("sessionToken", token)
                .put("bidderId", otherId).put("amount", "100"));
        assertEquals(ErrorCode.AUTHORIZATION, bid.getErrorCode());
        AuctionResponse profile = handler.process(new AuctionRequest(RequestType.UPDATE_USER).put("sessionToken", token)
                .put("userId", otherId).put("username", "changed").put("email", "changed@test.local"));
        assertEquals(ErrorCode.AUTHORIZATION, profile.getErrorCode());
        assertEquals("other", facade.getUser(otherId).getUsername());
        assertEquals(ErrorCode.AUTHORIZATION,
                handler.process(new AuctionRequest(RequestType.LIST_USERS).put("sessionToken", token)).getErrorCode());
    }

    @Test
    void sellersCannotManageAnotherSellersAuction() {
        AuctionResponse owner = register("owner", "Seller");
        AuctionResponse other = register("other", "Seller");
        String ownerId = ((UserDto) owner.getData()).getId();
        var auction = facade.createAuctionForSeller((com.auction.model.user.Seller) facade.getUser(ownerId),
                "Electronics", "Laptop", "description", 100);
        for (RequestType type : new RequestType[] {RequestType.START_AUCTION, RequestType.FINISH_AUCTION,
                RequestType.CANCEL_AUCTION}) {
            assertEquals(ErrorCode.AUTHORIZATION, handler.process(new AuctionRequest(type)
                    .put("sessionToken", other.getSessionToken()).put("auctionId", auction.getId())).getErrorCode());
        }
    }

    @Test
    void recoveryRequiresSecretCodeIsSingleUseSurvivesRestartAndRevokesOldSessions() {
        AuctionResponse registered = register("recover", "Bidder");
        String code = registered.getRecoveryCode();
        assertNotNull(code);
        AuctionResponse weakReset = handler.process(new AuctionRequest(RequestType.RESET_PASSWORD)
                .put("email", "recover@test.local").put("recoveryCode", "recover").put("newPassword", "changed123"));
        assertEquals(ErrorCode.AUTHENTICATION, weakReset.getErrorCode());
        SqliteUserDao dao = new SqliteUserDao(new DatabaseManager(jdbcUrl));
        assertFalse(dao.findRecoveryCodeHashByEmail("recover@test.local").contains(code));
        AuctionRequestHandler restarted = new AuctionRequestHandler(new AuctionServerFacade(new ServerContext(jdbcUrl, false)));
        AuctionRequest reset = new AuctionRequest(RequestType.RESET_PASSWORD).put("email", "recover@test.local")
                .put("recoveryCode", code).put("newPassword", "changed123");
        assertTrue(restarted.process(reset).isSuccess());
        assertEquals(ErrorCode.AUTHENTICATION, restarted.process(reset).getErrorCode());
        assertEquals(ErrorCode.AUTHENTICATION, handler.process(new AuctionRequest(RequestType.LIST_AUCTIONS)
                .put("sessionToken", registered.getSessionToken())).getErrorCode());
        AuctionResponse login = restarted.process(new AuctionRequest(RequestType.LOGIN)
                .put("email", "recover@test.local").put("password", "changed123"));
        assertTrue(login.isSuccess());
        assertNull(login.getRecoveryCode());
    }

    @Test
    void adminMayListUsersButRecoveryCodesAreNeverReturnedInUserDtos() {
        context.getAuthService().registerAdmin("admin", "admin@test.local", "password123");
        AuctionResponse login = handler.process(new AuctionRequest(RequestType.LOGIN)
                .put("email", "admin@test.local").put("password", "password123"));
        AuctionResponse list = handler.process(new AuctionRequest(RequestType.LIST_USERS).put("sessionToken", login.getSessionToken()));
        assertTrue(list.isSuccess());
        assertNull(list.getRecoveryCode());
    }

    @Test
    void logoutAndSessionExpiryRevokeAccess() {
        AuctionResponse registered = register("session", "Bidder");
        handler.process(new AuctionRequest(RequestType.LOGOUT).put("sessionToken", registered.getSessionToken()));
        assertEquals(ErrorCode.AUTHENTICATION, handler.process(new AuctionRequest(RequestType.LIST_AUCTIONS)
                .put("sessionToken", registered.getSessionToken())).getErrorCode());
        class MutableClock extends Clock {
            Instant now = Instant.parse("2026-10-10T00:00:00Z");
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return now; }
        }
        MutableClock clock = new MutableClock();
        SessionManager sessions = new SessionManager(facade, clock);
        User user = facade.login("session@test.local", "password123");
        String token = sessions.issue(user);
        assertEquals(user.getId(), sessions.requireUser(token).getId());
        clock.now = clock.now.plusSeconds(1800);
        assertThrows(AuthenticationException.class, () -> sessions.requireUser(token));
    }
}
