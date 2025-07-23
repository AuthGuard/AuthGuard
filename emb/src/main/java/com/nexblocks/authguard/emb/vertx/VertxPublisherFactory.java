package com.nexblocks.authguard.emb.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.emb.MessagePublisher;
import com.nexblocks.authguard.emb.MessagePublisherFactory;
import com.nexblocks.authguard.emb.model.Message;
import io.vertx.core.eventbus.EventBus;

public class VertxPublisherFactory implements MessagePublisherFactory {
    private final EventBus eventBus;

    @Inject
    public VertxPublisherFactory(final EventBus eventBus) {
        this.eventBus = eventBus;

        this.eventBus.registerDefaultCodec(Message.class, new EmbMessageCodec());
    }

    @Override
    public MessagePublisher create(final String channel) {
        return new VertxPublisher(channel, eventBus);
    }
}
