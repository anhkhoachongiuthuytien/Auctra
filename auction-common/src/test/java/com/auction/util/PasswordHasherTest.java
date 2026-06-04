package com.auction.util;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<PasswordHasher> constructor = PasswordHasher.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (InvocationTargetException e) {
            // expected
        } catch (InstantiationException | IllegalAccessException e) {
            fail("Failed to instantiate PasswordHasher");
        }
    }

    @Test
    void testHashFormat() {
        String password = "mySecurePassword";
        String hashed = PasswordHasher.hash(password);
        assertNotNull(hashed);
        
        String[] parts = hashed.split(":");
        assertEquals(3, parts.length);
        assertEquals("65536", parts[0]); // ITERATIONS
        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(parts[1])); // salt
        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(parts[2])); // hash
    }

    @Test
    void testHashMatches() {
        String password = "mySecurePassword";
        String hashed = PasswordHasher.hash(password);
        
        assertTrue(PasswordHasher.matches(password, hashed));
        assertFalse(PasswordHasher.matches("wrongPassword", hashed));
    }

    @Test
    void testMatchesEdgeCases() {
        String password = "mySecurePassword";
        String hashed = PasswordHasher.hash(password);

        assertFalse(PasswordHasher.matches(password, null));
        assertFalse(PasswordHasher.matches(password, ""));
        assertFalse(PasswordHasher.matches(password, "   "));

        assertThrows(IllegalStateException.class, () -> PasswordHasher.matches(password, "invalid_hash_no_colons"));
        assertThrows(IllegalStateException.class, () -> PasswordHasher.matches(password, "65536:only_two_parts"));
        assertThrows(IllegalStateException.class, () -> PasswordHasher.matches(password, "65536:three:parts:too:many"));
    }

    @Test
    void testConstantTimeEqualsDifferentLengths() {
        // Since matches decodes base64, we can test different lengths mismatch
        String rawPassword = "password";
        String hashedWithShortHash = "65536:c2FsdHNhbHRzYWx0c2FsdA==:c2hvcnQ="; // valid base64 but shorter hash
        assertFalse(PasswordHasher.matches(rawPassword, hashedWithShortHash));
    }
}
