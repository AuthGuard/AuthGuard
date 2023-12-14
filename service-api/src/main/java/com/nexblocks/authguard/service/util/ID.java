package com.nexblocks.authguard.service.util;

import de.mkammerer.snowflakeid.SnowflakeIdGenerator;

import java.util.Random;
import java.util.UUID;

public final class ID {
    private static int generatorId = new Random().nextInt(31);
    private static SnowflakeIdGenerator generator = SnowflakeIdGenerator.createDefault(generatorId);

    public static String generateSimplifiedUuid() {
        final UUID uuid = UUID.randomUUID();

        return Long.toHexString(uuid.getMostSignificantBits())
                + Long.toHexString(uuid.getLeastSignificantBits());
    }

    public static long generate() {
        return generator.next();
    }
}
