package com.authguard.basic;

import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.codes.ErrorCode;

import java.util.Base64;

public class TokensUtils {
    public static String[] parseAuthorization(final String authorization) {
        final String[] parts = authorization.split("\\s");

        if (parts.length != 2) {
            throw new ServiceException(ErrorCode.INVALID_AUTHORIZATION_FORMAT, "Invalid format for authorization value");
        }

        return parts;
    }

    public static String[] decodeAndSplitBasic(final String base64) {
        return new String(Base64.getDecoder().decode(base64)).split(":");
    }
}
