package com.nexblocks.authguard.service.events;

import com.google.inject.Inject;
import com.nexblocks.authguard.emb.MessageSubscriber;
import com.nexblocks.authguard.emb.annotations.Channel;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.service.EventsService;
import com.nexblocks.authguard.service.model.Entity;
import com.nexblocks.authguard.service.model.EventBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Channel("roles")
@Channel("permissions")
@Channel("accounts")
@Channel("apps")
@Channel("clients")
public class EventsRecorder implements MessageSubscriber {
    private static final Logger LOG = LoggerFactory.getLogger(EventsRecorder.class);

    private final EventsService eventsService;

    @Inject
    public EventsRecorder(EventsService eventsService) {
        this.eventsService = eventsService;
    }

    @Override
    public void onMessage(final Message message) {
        EventBO.Builder event = EventBO.builder()
                .domain(message.getDomain())
                .eventType(message.getEventType().name())
                .channel(message.getChannel());

        if (message.getEventType() == EventType.ENTITY_CREATED
                || message.getEventType() == EventType.ENTITY_UPDATED
                || message.getEventType() == EventType.ENTITY_DELETED) {
            populateEntity(message, event);
        }

        eventsService.create(event.build()).join();
    }

    private void populateEntity(final Message message, final EventBO.Builder event) {
        if (message.getBodyType() == null || !Entity.class.isAssignableFrom(message.getBodyType())) {
            LOG.error("Invalid entity event body type. Type {} is not assignable to Entity class",
                    message.getBodyType());
        }

        Entity entity = (Entity) message.getMessageBody();

        event.eventEntityType(entity.getEntityType());
        event.entityId(entity.getId());
        // TODO this may reveal sensitive information, we gotta address this first
//        event.entitySnapshot(entity.toString());
    }
}
