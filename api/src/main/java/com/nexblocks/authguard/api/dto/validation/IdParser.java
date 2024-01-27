package com.nexblocks.authguard.api.dto.validation;

import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;

public class IdParser {
    public static Long from(final String idString) {
        try {
            return Long.parseLong(idString);
        } catch (NumberFormatException ex) {
            throw new ServiceException(ErrorCode.INVALID_REQUEST_VALUE, "Value '" + idString + "' is not a valid ID");
        }
    }
}
