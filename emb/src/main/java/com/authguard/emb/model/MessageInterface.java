package com.authguard.emb.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.OffsetDateTime;

@Value.Immutable
@MOStyle
@JsonSerialize(as = Message.class)
@JsonDeserialize(as = Message.class)
public interface MessageInterface<T> {
    OffsetDateTime getTimestamp();
    EventType getEventType();
    Class<?> getBodyType();
    T getMessageBody();
}
