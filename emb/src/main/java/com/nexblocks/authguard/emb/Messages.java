package com.nexblocks.authguard.emb;

import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;

import java.time.Instant;
import java.time.OffsetDateTime;

public class Messages {
    public static Message created(final Object object, final String domain) {
        return basicMessage(EventType.ENTITY_CREATED, object, domain);
    }

    public static Message updated(final Object object, final String domain) {
        return basicMessage(EventType.ENTITY_UPDATED, object, domain);
    }

    public static Message deleted(final Object object, final String domain) {
        return basicMessage(EventType.ENTITY_DELETED, object, domain);
    }

    public static Message otpGenerated(final Object object, final String domain) {
        return basicMessage(EventType.OTP_GENERATED, object, domain);
    }

    public static Message passwordlessGenerated(final Object object, final String domain) {
        return basicMessage(EventType.PASSWORDLESS_GENERATED, object, domain);
    }

    public static Message emailVerification(final Object object, final String domain) {
        return basicMessage(EventType.EMAIL_VERIFICATION, object, domain);
    }

    public static Message phoneNumberVerification(final Object object, final String domain) {
        return basicMessage(EventType.PHONE_NUMBER_VERIFICATION, object, domain);
    }

    public static Message auth(final Object object, final String domain) {
        return basicMessage(EventType.AUTHENTICATION, object, domain);
    }

    public static Message resetTokenGenerated(final Object object, final String domain) {
        return basicMessage(EventType.RESET_TOKEN_GENERATED, object, domain);
    }

    public static Message basicMessage(final EventType eventType, final Object object,
                                       final String domain) {
        return Message.builder()
                .domain(domain)
                .eventType(eventType)
                .timestamp(Instant.now())
                .bodyType(object.getClass())
                .messageBody(object)
                .build();
    }
}
