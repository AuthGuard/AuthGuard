package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface Event extends Entity {
    String getEventType();
    String getChannel();
    String getEventEntityType();
    Long getEntityId();
    String getEntitySnapshot();
    String getActorType();
    String getActor();


    @Override
    @Value.Derived
    default String getEntityType() {
        return "Event";
    }
}
