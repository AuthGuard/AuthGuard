package com.nexblocks.authguard.api.common;

import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import io.vertx.ext.web.RoutingContext;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    public static Long getLongCursor(final RoutingContext context) {
        List<String> queryParam = context.queryParam("cursor");

        if (queryParam == null) {
            return null;
        }

        try {
            return queryParam.stream()
                    .findFirst()
                    .map(Long::parseLong)
                    .orElse(null);
        } catch (Exception e) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("cursor", ViolationType.INVALID_VALUE)
            ));
        }
    }

    public static Instant parseInstantCursor(final RoutingContext context) {
        List<String> queryParam = context.queryParam("cursor");

        if (queryParam == null) {
            return null;
        }

        try {
            return queryParam.stream()
                    .findFirst()
                    .map(cursor -> Instant.ofEpochMilli(Long.parseLong(cursor)))
                    .orElse(null);
        } catch (Exception e) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("cursor", ViolationType.INVALID_VALUE)
            ));
        }
    }
}
