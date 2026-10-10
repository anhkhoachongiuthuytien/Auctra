package com.auction.protocol;

import java.io.ObjectInputFilter;

/** Only protocol DTOs and their collection containers may cross the socket. */
public final class ProtocolFilter {
    private ProtocolFilter() {
    }

    public static ObjectInputFilter create() {
        return info -> {
            if (info.depth() > 30 || info.arrayLength() > 1_000_000) {
                return ObjectInputFilter.Status.REJECTED;
            }
            Class<?> type = info.serialClass();
            if (type == null) { return ObjectInputFilter.Status.UNDECIDED; }
            while (type.isArray()) { type = type.getComponentType(); }
            String name = type.getName();
            if (type.isPrimitive() || name.startsWith("com.auction.protocol.")
                    || name.equals("com.auction.model.auction.AutoBidConfig")
                    || name.equals("java.lang.String") || name.equals("java.lang.Enum")
                    || name.equals("java.lang.Object") || name.equals("java.util.ArrayList")
                    || name.equals("java.util.HashMap") || name.equals("java.util.Map$Entry")) {
                return ObjectInputFilter.Status.ALLOWED;
            }
            return ObjectInputFilter.Status.REJECTED;
        };
    }
}
