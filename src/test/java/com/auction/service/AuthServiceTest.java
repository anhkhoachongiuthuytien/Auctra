package com.auction.service;

import com.auction.dao.memory.InMemoryUserDao;
import com.auction.exception.AuthenticationException;
import com.auction.exception.ValidationException;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    private static final String VALID_PASSWORD = "demo12345";

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(new InMemoryUserDao());
    }

    // Registering a seller with a valid password should create an id and persist the user.
    @Test
    void testRegisterSellerSuccess() {
        Seller seller = authService.registerSeller("seller", "seller@test.com", VALID_PASSWORD);

        assertNotNull(seller.getId());
        assertEquals("seller@test.com", seller.getEmail());
    }

    // Registering a bidder with a valid password should create the correct user type.
    @Test
    void testRegisterBidderSuccess() {
        Bidder bidder = authService.registerBidder("bidder", "bidder@test.com", VALID_PASSWORD);

        assertNotNull(bidder.getId());
        assertEquals("bidder@test.com", bidder.getEmail());
    }

    // Duplicate emails must still be rejected even after password support is added.
    @Test
    void testRegisterWithDuplicateEmailThrowsAuthenticationException() {
        authService.registerSeller("seller", "dup@test.com", VALID_PASSWORD);

        assertThrows(AuthenticationException.class, () ->
                authService.registerBidder("bidder", "dup@test.com", VALID_PASSWORD));
    }

    // Blank usernames should fail before any password hashing or persistence happens.
    @Test
    void testRegisterWithBlankUsernameThrowsValidationException() {
        assertThrows(ValidationException.class, () ->
                authService.registerSeller(" ", "seller@test.com", VALID_PASSWORD));
    }

    // Passwords shorter than the minimum policy must be rejected.
    @Test
    void testRegisterWithShortPasswordThrowsValidationException() {
        assertThrows(ValidationException.class, () ->
                authService.registerSeller("seller", "seller@test.com", "short"));
    }

    // Logging in with the correct email/password pair should return the persisted user.
    @Test
    void testLoginSuccess() {
        authService.registerBidder("bidder", "bidder@test.com", VALID_PASSWORD);

        assertEquals("bidder@test.com", authService.login("bidder@test.com", VALID_PASSWORD).getEmail());
    }

    // Unknown emails should still fail authentication.
    @Test
    void testLoginWithUnknownEmailThrowsAuthenticationException() {
        assertThrows(AuthenticationException.class, () ->
                authService.login("missing@test.com", VALID_PASSWORD));
    }

    // Incorrect passwords must fail even if the email exists.
    @Test
    void testLoginWithWrongPasswordThrowsAuthenticationException() {
        authService.registerBidder("bidder", "bidder@test.com", VALID_PASSWORD);

        assertThrows(AuthenticationException.class, () ->
                authService.login("bidder@test.com", "wrongpass"));
    }

    // Blank passwords must be blocked at validation time.
    @Test
    void testLoginWithBlankPasswordThrowsValidationException() {
        assertThrows(ValidationException.class, () -> authService.login("bidder@test.com", " "));
    }

    // The helper should still report whether an email is already in use.
    @Test
    void testEmailExists() {
        authService.registerAdmin("admin", "admin@test.com", VALID_PASSWORD);

        assertTrue(authService.emailExists("admin@test.com"));
    }
}
