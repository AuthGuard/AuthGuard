package com.authguard.service.config;

import com.authguard.service.exceptions.ServiceException;

import java.time.Duration;

public class ConfigParser {
    public static Duration parseDuration(final String str) {
        final int amount = Integer.parseInt(str.substring(0, str.length() - 1));
        final char unit = str.charAt(str.length() - 1);

        switch (unit) {
            case 's':
                return Duration.ofSeconds(amount);
            case 'm':
                return Duration.ofMinutes(amount);
            case 'h':
                return Duration.ofHours(amount);
            case 'd':
                return Duration.ofDays(amount);
            default:
                throw new ServiceException("Unable to parse " + str);
        }
    }
}
