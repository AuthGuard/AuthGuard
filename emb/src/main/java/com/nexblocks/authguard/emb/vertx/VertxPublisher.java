package com.nexblocks.authguard.emb.vertx;

import com.nexblocks.authguard.emb.MessagePublisher;
import com.nexblocks.authguard.emb.MessageSubscriber;
import com.nexblocks.authguard.emb.model.Message;
import io.vertx.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VertxPublisher implements MessagePublisher {
    private static final Logger LOG = LoggerFactory.getLogger(VertxPublisher.class);
    private final String channel;
    private final EventBus eventBus;

    public VertxPublisher(final String channel, final EventBus eventBus) {
        this.channel = channel;
        this.eventBus = eventBus;
    }

    @Override
    public void publish(final Message<?> message) {
        eventBus.publish(channel, message);
    }

    @Override
    public void acceptSubscriber(final MessageSubscriber subscriber) {
        MessageSubscriber wrapped = safeConsumer(subscriber);
        eventBus.consumer(channel, vertxMessage -> wrapped.onMessage((Message<?>) vertxMessage.body()));
    }

    private MessageSubscriber safeConsumer(final MessageSubscriber subscriber) {
        return message -> {
            try {
                subscriber.onMessage(message.withChannel(channel));
            } catch (Throwable e) {
                LOG.warn("Subscriber {} threw an exception", subscriber.getClass(), e);
            }
        };
    }
}
