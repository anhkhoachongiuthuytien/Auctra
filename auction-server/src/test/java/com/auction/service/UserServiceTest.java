package com.auction.service;

import com.auction.dao.memory.InMemoryUserDao;
import com.auction.exception.AuthenticationException;
import com.auction.exception.ValidationException;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private InMemoryUserDao userDao;
    private UserService userService;
    private Bidder bidder;
    private Seller seller;
    private Admin admin;

    @BeforeEach
    void setUp() {
        userDao = new InMemoryUserDao();
        userService = new UserService(userDao);

        bidder = new Bidder("B001", "bidder", "bidder@test.com");
        seller = new Seller("S001", "seller", "seller@test.com");
        admin = new Admin("A001", "admin", "admin@test.com");

        userDao.save(bidder);
        userDao.save(seller);
        userDao.save(admin);
    }

    @Test
    void testGetUserByIdSuccess() {
        User result = userService.getUserById("B001");
        assertNotNull(result);
        assertEquals("bidder", result.getUsername());
    }

    @Test
    void testGetUserByIdNotFound() {
        assertThrows(AuthenticationException.class, () -> userService.getUserById("NON_EXIST"));
    }

    @Test
    void testGetUserByEmailSuccess() {
        User result = userService.getUserByEmail("seller@test.com");
        assertNotNull(result);
        assertEquals("S001", result.getId());
    }

    @Test
    void testGetUserByEmailNotFound() {
        assertThrows(AuthenticationException.class, () -> userService.getUserByEmail("nonexist@test.com"));
    }

    @Test
    void testGetAllUsers() {
        List<User> users = userService.getAllUsers();
        assertEquals(3, users.size());
    }

    @Test
    void testExistsByEmail() {
        assertTrue(userService.existsByEmail("admin@test.com"));
        assertFalse(userService.existsByEmail("missing@test.com"));
    }

    @Test
    void testUpdateUserBasic() {
        User updated = userService.updateUser("B001", "new_bidder", "new_bidder@test.com");
        assertEquals("new_bidder", updated.getUsername());
        assertEquals("new_bidder@test.com", updated.getEmail());
    }

    @Test
    void testUpdateUserValidationErrors() {
        // Empty ID
        assertThrows(ValidationException.class, () -> userService.updateUser("", "user", "email@test.com"));
        assertThrows(ValidationException.class, () -> userService.updateUser(null, "user", "email@test.com"));

        // Empty username
        assertThrows(ValidationException.class, () -> userService.updateUser("B001", "", "email@test.com"));
        assertThrows(ValidationException.class, () -> userService.updateUser("B001", null, "email@test.com"));

        // Empty email
        assertThrows(ValidationException.class, () -> userService.updateUser("B001", "user", ""));
        assertThrows(ValidationException.class, () -> userService.updateUser("B001", "user", null));
    }

    @Test
    void testUpdateUserNotFound() {
        assertThrows(AuthenticationException.class, () -> userService.updateUser("NON_EXIST", "user", "email@test.com"));
    }

    @Test
    void testUpdateUserDuplicateEmail() {
        // Update bidder email to seller's email
        assertThrows(AuthenticationException.class, () -> userService.updateUser("B001", "bidder", "seller@test.com"));
    }

    @Test
    void testUpdateUserSameEmailSuccess() {
        // Bidder updates profile without changing email
        User updated = userService.updateUser("B001", "bidder_new_name", "bidder@test.com");
        assertEquals("bidder_new_name", updated.getUsername());
        assertEquals("bidder@test.com", updated.getEmail());
    }

    @Test
    void testUpdateBidderSpecificFields() {
        User updated = userService.updateUser("B001", "bidder", "bidder@test.com",
                "123 Main St", "0987654321", null, null, null);
        assertTrue(updated instanceof Bidder);
        Bidder updatedBidder = (Bidder) updated;
        assertEquals("123 Main St", updatedBidder.getShippingAddress());
        assertEquals("0987654321", updatedBidder.getPhoneNumber());
    }

    @Test
    void testUpdateSellerSpecificFields() {
        User updated = userService.updateUser("S001", "seller", "seller@test.com",
                null, null, "My Store", "We sell items", null);
        assertTrue(updated instanceof Seller);
        Seller updatedSeller = (Seller) updated;
        assertEquals("My Store", updatedSeller.getStoreName());
        assertEquals("We sell items", updatedSeller.getStoreDescription());
    }

    @Test
    void testUpdateAdminSpecificFields() {
        User updated = userService.updateUser("A001", "admin", "admin@test.com",
                null, null, null, null, "Operations", "avatar.png");
        assertTrue(updated instanceof Admin);
        Admin updatedAdmin = (Admin) updated;
        assertEquals("Operations", updatedAdmin.getDepartment());
        assertEquals("avatar.png", updatedAdmin.getAvatarPath());
    }
}
