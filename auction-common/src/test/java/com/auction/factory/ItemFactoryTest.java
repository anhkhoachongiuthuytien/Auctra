package com.auction.factory;

import com.auction.enums.ItemType;
import com.auction.model.item.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    void testCreateItemWithEnumSuccess() {
        Item art = ItemFactory.createItem(ItemType.ART, "1", "Painting", "Oil painting", 100.0);
        assertTrue(art instanceof Art);
        assertEquals("Painting", art.getName());
        assertEquals(100.0, art.getStartingPrice());

        Item elec = ItemFactory.createItem(ItemType.ELECTRONICS, "2", "Phone", "Smartphone", 500.0);
        assertTrue(elec instanceof Electronics);

        Item veh = ItemFactory.createItem(ItemType.VEHICLE, "3", "Car", "Sedan", 15000.0);
        assertTrue(veh instanceof Vehicle);

        Item other = ItemFactory.createItem(ItemType.OTHER, "4", "Book", "Novel", 10.0);
        assertTrue(other instanceof Other);
    }

    @Test
    void testCreateItemWithEnumNull() {
        assertThrows(IllegalArgumentException.class, () -> 
            ItemFactory.createItem((ItemType) null, "1", "Name", "Desc", 10.0)
        );
    }

    @Test
    void testCreateItemWithStringSuccess() {
        Item art1 = ItemFactory.createItem("ART", "1", "Painting", "Oil painting", 100.0);
        assertTrue(art1 instanceof Art);

        Item art2 = ItemFactory.createItem("Art", "1", "Painting", "Oil painting", 100.0);
        assertTrue(art2 instanceof Art);

        Item elec = ItemFactory.createItem("Electronics", "2", "Phone", "Smartphone", 500.0);
        assertTrue(elec instanceof Electronics);
    }

    @Test
    void testCreateItemWithStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> 
            ItemFactory.createItem("INVALID_TYPE", "1", "Name", "Desc", 10.0)
        );
        assertThrows(IllegalArgumentException.class, () -> 
            ItemFactory.createItem("", "1", "Name", "Desc", 10.0)
        );
        assertThrows(IllegalArgumentException.class, () -> 
            ItemFactory.createItem((String) null, "1", "Name", "Desc", 10.0)
        );
    }
}
