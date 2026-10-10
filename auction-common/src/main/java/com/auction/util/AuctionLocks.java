package com.auction.util;

import com.auction.exception.ValidationException;

/** Stable, bounded locks shared by every operation on an auction in this JVM. */
public final class AuctionLocks {
    private static final Object[] LOCKS = new Object[256];

    static {
        for (int i = 0; i < LOCKS.length; i++) {
            LOCKS[i] = new Object();
        }
    }

    private AuctionLocks() {
    }

    public static Object forId(String id) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("Mã phiên đấu giá không được để trống");
        }
        return LOCKS[Math.floorMod(id.hashCode(), LOCKS.length)];
    }
}
