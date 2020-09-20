package com.authguard.emb;

import com.authguard.emb.model.EventType;
import com.authguard.emb.model.Message;

import java.time.OffsetDateTime;

public class Messages {
    public static Message created(final Object object) {
        return basicMessage(EventType.ENTITY_CREATED, object);
    }

    public static Message updated(final Object object) {
        return basicMessage(EventType.ENTITY_UPDATED, object);
    }

    public static Message deleted(final Object object) {
        return basicMessage(EventType.ENTITY_DELETED, object);
    }

    public static Message otpGenerated(final Object object) {
        return basicMessage(EventType.OTP_GENERATED, object);
    }

    public static Message passwordlessGenerated(final Object object) {
        return basicMessage(EventType.PASSWORDLESS_GENERATED, object);
    }

    public static Message emailVerification(final Object object) {
        return basicMessage(EventType.EMAIL_VERIFICATION, object);
    }

    private static Message basicMessage(final EventType eventType, final Object object) {
        return Message.builder()
                .eventType(eventType)
                .timestamp(OffsetDateTime.now())
                .bodyType(object.getClass())
                .messageBody(object)
                .build();
    }
}
