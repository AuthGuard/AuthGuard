package org.auther.emb.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.OffsetDateTime;

@Value.Immutable
@MOStyle
@JsonSerialize(as = MessageMO.class)
@JsonDeserialize(as = MessageMO.class)
public interface Message<T> {
    OffsetDateTime getTimestamp();
    EventType getEventType();
    T getMessageBody();
}
