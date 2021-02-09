package com.nexblocks.authguard.basic;

import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;

public class TokensUtils {
    public static String[] parseAuthorization(final String authorization) {
        final String[] parts = authorization.split("\\s");

        if (parts.length != 2) {
            throw new ServiceException(ErrorCode.INVALID_AUTHORIZATION_FORMAT, "Invalid format for authorization value");
        }

        return parts;
    }
}
