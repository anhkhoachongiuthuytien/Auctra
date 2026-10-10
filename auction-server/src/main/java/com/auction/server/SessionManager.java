package com.auction.server;

import com.auction.exception.AuthenticationException;
import com.auction.model.user.User;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side sessions are bound to the account and its current password hash. */
public class SessionManager {
    private static final Duration LIFETIME = Duration.ofMinutes(30);
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final AuctionServerFacade facade;
    private final Clock clock;

    public SessionManager(AuctionServerFacade facade) { this(facade, Clock.systemUTC()); }

    SessionManager(AuctionServerFacade facade, Clock clock) {
        this.facade = facade;
        this.clock = clock;
    }

    public String issue(User user) {
        Instant now = clock.instant();
        sessions.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(user.getId(), facade.getPasswordHash(user), now.plus(LIFETIME)));
        return token;
    }

    public User requireUser(String token) {
        Session session = token == null ? null : sessions.get(token);
        if (session == null || !clock.instant().isBefore(session.expiresAt())) {
            revoke(token);
            throw new AuthenticationException("Phiên đăng nhập hết hạn; vui lòng đăng nhập lại");
        }
        User user = facade.getUser(session.userId());
        if (!session.passwordHash().equals(facade.getPasswordHash(user))) {
            revoke(token);
            throw new AuthenticationException("Mật khẩu đã thay đổi; vui lòng đăng nhập lại");
        }
        return user;
    }

    public void revoke(String token) { if (token != null) { sessions.remove(token); } }

    private record Session(String userId, String passwordHash, Instant expiresAt) { }
}
