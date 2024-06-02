package com.nexblocks.authguard.rest.util;

import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.rest.exceptions.RequestValidationException;

import java.time.Instant;
import java.util.Collections;

public class Cursors {
    public static Instant parseInstantCursor(final Long cursor) {
        if (cursor == null) {
            return null;
        }

        try {
            return Instant.ofEpochMilli(cursor);
        } catch (Exception e) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("cursor", ViolationType.INVALID_VALUE)
            ));
        }
    }
}
