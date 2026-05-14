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

    // Đăng ký seller với mật khẩu hợp lệ phải tạo id và lưu user vào hệ thống.
    @Test
    void testRegisterSellerSuccess() {
        Seller seller = authService.registerSeller("seller", "seller@test.com", VALID_PASSWORD);

        assertNotNull(seller.getId());
        assertEquals("seller@test.com", seller.getEmail());
    }

    // Đăng ký bidder với mật khẩu hợp lệ phải tạo đúng loại user.
    @Test
    void testRegisterBidderSuccess() {
        Bidder bidder = authService.registerBidder("bidder", "bidder@test.com", VALID_PASSWORD);

        assertNotNull(bidder.getId());
        assertEquals("bidder@test.com", bidder.getEmail());
    }

    // Email trùng lặp vẫn phải bị từ chối kể cả khi đã có hỗ trợ mật khẩu.
    @Test
    void testRegisterWithDuplicateEmailThrowsAuthenticationException() {
        authService.registerSeller("seller", "dup@test.com", VALID_PASSWORD);

        assertThrows(AuthenticationException.class, () ->
                authService.registerBidder("bidder", "dup@test.com", VALID_PASSWORD));
    }

    // Tên đăng nhập để trống phải bị chặn trước khi hash mật khẩu hoặc lưu dữ liệu.
    @Test
    void testRegisterWithBlankUsernameThrowsValidationException() {
        assertThrows(ValidationException.class, () ->
                authService.registerSeller(" ", "seller@test.com", VALID_PASSWORD));
    }

    // Mật khẩu ngắn hơn quy định tối thiểu phải bị từ chối.
    @Test
    void testRegisterWithShortPasswordThrowsValidationException() {
        assertThrows(ValidationException.class, () ->
                authService.registerSeller("seller", "seller@test.com", "short"));
    }

    // Đăng nhập với email/mật khẩu đúng phải trả về user đã lưu.
    @Test
    void testLoginSuccess() {
        authService.registerBidder("bidder", "bidder@test.com", VALID_PASSWORD);

        assertEquals("bidder@test.com", authService.login("bidder@test.com", VALID_PASSWORD).getEmail());
    }

    // Email không tồn tại trong hệ thống phải trả lỗi xác thực.
    @Test
    void testLoginWithUnknownEmailThrowsAuthenticationException() {
        assertThrows(AuthenticationException.class, () ->
                authService.login("missing@test.com", VALID_PASSWORD));
    }

    // Mật khẩu sai phải bị từ chối kể cả khi email tồn tại.
    @Test
    void testLoginWithWrongPasswordThrowsAuthenticationException() {
        authService.registerBidder("bidder", "bidder@test.com", VALID_PASSWORD);

        assertThrows(AuthenticationException.class, () ->
                authService.login("bidder@test.com", "wrongpass"));
    }

    // Mật khẩu để trống phải bị chặn ở tầng validation.
    @Test
    void testLoginWithBlankPasswordThrowsValidationException() {
        assertThrows(ValidationException.class, () -> authService.login("bidder@test.com", " "));
    }

    // Phương thức kiểm tra email phải báo đúng khi email đã được đăng ký.
    @Test
    void testEmailExists() {
        authService.registerAdmin("admin", "admin@test.com", VALID_PASSWORD);

        assertTrue(authService.emailExists("admin@test.com"));
    }
}
