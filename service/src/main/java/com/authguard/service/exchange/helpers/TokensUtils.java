package com.authguard.service.exchange.helpers;

import com.authguard.service.exceptions.ServiceException;

import java.util.Base64;

public class TokensUtils {
    public static String[] parseAuthorization(final String authorization) {
        final String[] parts = authorization.split("\\s");

        if (parts.length != 2) {
            throw new ServiceException("Invalid format for authorization value");
        }

        return parts;
    }

    public static String[] decodeAndSplitBasic(final String base64) {
        return new String(Base64.getDecoder().decode(base64)).split(":");
    }
}
