package com.nexblocks.authguard.api.common;

import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import io.javalin.http.Context;

import java.util.Collections;
import java.util.Optional;

public class IdempotencyHeader {
    public static final String HEADER_NAME = "X-IdempotentKey";

    public static String getKeyOrFail(final Context context) {
        return Optional.ofNullable(context.header(HEADER_NAME))
                .orElseThrow(() -> new RequestValidationException(Collections.singletonList(
                        new Violation("Header X-IdempotentKey", ViolationType.MISSING_REQUIRED_VALUE)
                )));
    }
}
