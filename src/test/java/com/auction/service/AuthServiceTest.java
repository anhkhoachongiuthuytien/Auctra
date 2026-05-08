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

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(new InMemoryUserDao());
    }

    // Đăng ký seller hợp lệ phải sinh id và lưu đúng email.
    @Test
    void testRegisterSellerSuccess() {
        Seller seller = authService.registerSeller("seller", "seller@test.com");

        assertNotNull(seller.getId());
        assertEquals("seller@test.com", seller.getEmail());
    }

    // Đăng ký bidder hợp lệ phải tạo đúng kiểu user và dữ liệu cơ bản.
    @Test
    void testRegisterBidderSuccess() {
        Bidder bidder = authService.registerBidder("bidder", "bidder@test.com");

        assertNotNull(bidder.getId());
        assertEquals("bidder@test.com", bidder.getEmail());
    }

    // Email trùng phải bị chặn để giữ tính duy nhất của tài khoản.
    @Test
    void testRegisterWithDuplicateEmailThrowsAuthenticationException() {
        authService.registerSeller("seller", "dup@test.com");

        assertThrows(AuthenticationException.class, () ->
                authService.registerBidder("bidder", "dup@test.com"));
    }

    // Username rỗng phải bị validation ngay từ đầu.
    @Test
    void testRegisterWithBlankUsernameThrowsValidationException() {
        assertThrows(ValidationException.class, () ->
                authService.registerSeller(" ", "seller@test.com"));
    }

    // Login với email đã đăng ký phải trả về đúng user tương ứng.
    @Test
    void testLoginSuccess() {
        authService.registerBidder("bidder", "bidder@test.com");

        assertEquals("bidder@test.com", authService.login("bidder@test.com").getEmail());
    }

    // Email chưa tồn tại phải dẫn đến lỗi xác thực.
    @Test
    void testLoginWithUnknownEmailThrowsAuthenticationException() {
        assertThrows(AuthenticationException.class, () ->
                authService.login("missing@test.com"));
    }

    // Email rỗng không được phép đi qua tầng login.
    @Test
    void testLoginWithBlankEmailThrowsValidationException() {
        assertThrows(ValidationException.class, () -> authService.login(" "));
    }

    // Hàm emailExists phải phản ánh đúng trạng thái dữ liệu đã đăng ký.
    @Test
    void testEmailExists() {
        authService.registerAdmin("admin", "admin@test.com");

        assertTrue(authService.emailExists("admin@test.com"));
    }
}
