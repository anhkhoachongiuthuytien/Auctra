package com.auction.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdGeneratorTest {

    @Test
    void testGenerateId() {
        String id1 = IdGenerator.generateId();
        String id2 = IdGenerator.generateId();
        
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
        assertEquals(36, id1.length()); // Standard UUID length
    }
}
