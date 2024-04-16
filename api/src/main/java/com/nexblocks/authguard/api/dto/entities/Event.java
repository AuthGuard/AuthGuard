package com.nexblocks.authguard.api.dto.entities;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = EventDTO.class)
public interface Event {
    String getId();
    Instant getCreatedAt();
    String getEventType();
    String getChannel();
    String getEventEntityType();
    Long getEntityId();
    String getEntitySnapshot();
    String getActorType();
    String getActor();
}
