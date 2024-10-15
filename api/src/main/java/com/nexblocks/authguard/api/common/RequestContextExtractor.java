package com.nexblocks.authguard.api.common;

import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.ClientBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestContextExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(RequestContextExtractor.class);

    private static final String ACTOR_ATTRIBUTE = "actor";

    public static RequestContextBO extractWithoutIdempotentKey(final Context context) {
        final Object actor = context.attribute(ACTOR_ATTRIBUTE);

        if (actor != null) {
            return extractWithoutIdempotentKey(context, actor);
        }

        return RequestContextBO.builder()
                .source(context.ip())
                .userAgent(context.userAgent())
                .build();
    }

    public static RequestContextBO extractWithoutIdempotentKey(final Context context, final Object actor) {
        if (actor instanceof ClientBO) {
            return RequestContextBO.builder()
                    .source(context.ip())
                    .clientId("" + ((ClientBO) actor).getId())
                    .userAgent(context.userAgent())
                    .build();
        }

        return RequestContextBO.builder()
                .source(context.ip())
                .accountId("" + ((AccountBO) actor).getId())
                .userAgent(context.userAgent())
                .build();
    }
}
