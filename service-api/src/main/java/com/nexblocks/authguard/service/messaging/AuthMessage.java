package com.nexblocks.authguard.service.messaging;

import com.nexblocks.authguard.service.model.EntityType;

import java.util.Objects;

public class AuthMessage {
    private final String exchangeFrom;
    private final String exchangeTo;
    private final EntityType entityType;
    private final Long entityId;
    private final boolean successful;
    private final Throwable exception;

    public AuthMessage(final String exchangeFrom, final String exchangeTo, final EntityType entityType,
                       final Long entityId, final boolean successful, final Throwable exception) {
        this.exchangeFrom = exchangeFrom;
        this.exchangeTo = exchangeTo;
        this.entityType = entityType;
        this.entityId = entityId;
        this.successful = successful;
        this.exception = exception;
    }

    public static AuthMessage success(final String exchangeFrom, final String exchangeTo, final EntityType entityType,
                                      final Long entityId) {
        return new AuthMessage(exchangeFrom, exchangeTo, entityType, entityId, true, null);
    }

    public static AuthMessage failure(final String exchangeFrom, final String exchangeTo, final EntityType entityType,
                                      final Long entityId, final Throwable cause) {
        return new AuthMessage(exchangeFrom, exchangeTo, entityType, entityId, true, cause);
    }

    public static AuthMessage failure(final String exchangeFrom, final String exchangeTo, final EntityType entityType,
                                      final Throwable cause) {
        return new AuthMessage(exchangeFrom, exchangeTo, entityType, null, true, cause);
    }

    public static AuthMessage failure(final String exchangeFrom, final String exchangeTo, final Throwable cause) {
        return new AuthMessage(exchangeFrom, exchangeTo, null, null, true, cause);
    }

    public String getExchangeFrom() {
        return exchangeFrom;
    }

    public String getExchangeTo() {
        return exchangeTo;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public Throwable getException() {
        return exception;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthMessage)) return false;
        final AuthMessage that = (AuthMessage) o;
        return successful == that.successful &&
                Objects.equals(exchangeFrom, that.exchangeFrom) &&
                Objects.equals(exchangeTo, that.exchangeTo) &&
                Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exchangeFrom, exchangeTo, entityId, successful);
    }

    @Override
    public String toString() {
        return "AuthMessage{" +
                "exchangeFrom='" + exchangeFrom + '\'' +
                ", exchangeTo='" + exchangeTo + '\'' +
                ", accountId='" + entityId + '\'' +
                ", successful=" + successful +
                '}';
    }
}
