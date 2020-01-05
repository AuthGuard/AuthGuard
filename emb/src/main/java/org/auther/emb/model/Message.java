package org.auther.emb.model;

import org.immutables.value.Value;

import java.time.OffsetDateTime;

@Value.Immutable
@MOStyle
public interface Message<T> {
    OffsetDateTime getTimestamp();
    EventType getEventType();
    T getMessageBody();
}
