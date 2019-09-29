package org.auther.service.impl.jwt;

import com.auth0.jwt.algorithms.Algorithm;
import org.auther.service.exceptions.ServiceException;

import java.time.Duration;

class JwtConfigParser {
    static Algorithm parseAlgorithm(final String algorithmName, final String key) {
        switch (algorithmName) {
            case "HMAC256":
                return Algorithm.HMAC256(key);

            case "HMAC512":
                return Algorithm.HMAC512(key);

            default:
                throw new ServiceException("Unsupported algorithm " + algorithmName);
        }
    }

    static Duration parseDuration(final String str) {
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
