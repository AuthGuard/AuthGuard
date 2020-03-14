package com.authguard.service.impl.jwt;

import com.auth0.jwt.algorithms.Algorithm;
import com.authguard.service.exceptions.ServiceException;

public class JwtConfigParser {
    public static Algorithm parseAlgorithm(final String algorithmName, final String key) {
        switch (algorithmName) {
            case "HMAC256":
                return Algorithm.HMAC256(key);

            case "HMAC512":
                return Algorithm.HMAC512(key);

            default:
                throw new ServiceException("Unsupported algorithm " + algorithmName);
        }
    }

}
