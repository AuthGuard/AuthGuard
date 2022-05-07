package com.nexblocks.authguard.emb;

import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;

import java.time.Instant;
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

    public static Message phoneNumberVerification(final Object object) {
        return basicMessage(EventType.PHONE_NUMBER_VERIFICATION, object);
    }

    public static Message auth(final Object object) {
        return basicMessage(EventType.AUTHENTICATION, object);
    }

    public static Message resetTokenGenerated(final Object object) {
        return basicMessage(EventType.RESET_TOKEN_GENERATED, object);
    }

    public static Message basicMessage(final EventType eventType, final Object object) {
        return Message.builder()
                .eventType(eventType)
                .timestamp(Instant.now())
                .bodyType(object.getClass())
                .messageBody(object)
                .build();
    }
}
