package com.nexblocks.authguard.service.util;

import java.util.UUID;

public final class ID {
    public static String generate() {
        final UUID uuid = UUID.randomUUID();

        return Long.toHexString(uuid.getMostSignificantBits())
                + Long.toHexString(uuid.getLeastSignificantBits());
    }
}
