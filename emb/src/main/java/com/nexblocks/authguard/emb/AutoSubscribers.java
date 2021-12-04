package com.nexblocks.authguard.emb;

import com.nexblocks.authguard.emb.annotations.Channel;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class AutoSubscribers {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final MessageBus messageBus;
    private final Set<MessageSubscriber> subscribers;

    @Inject
    public AutoSubscribers(final MessageBus messageBus, final Set<MessageSubscriber> subscribers) {
        this.messageBus = messageBus;
        this.subscribers = subscribersWithChannels(subscribers);
    }

    private Set<MessageSubscriber> subscribersWithChannels(final Set<MessageSubscriber> subscribers) {
        return subscribers.stream()
                .filter(subscriber -> subscriber.getClass().getAnnotation(Channel.class) != null)
                .collect(Collectors.toSet());
    }

    public void subscribe() {
        for (final MessageSubscriber subscriber : subscribers) {
            final Channel channel = subscriber.getClass().getAnnotation(Channel.class);

            try {
                messageBus.subscribe(channel.value(), subscriber);

                log.info("Auto-subscribed {} to channel {}",
                        subscriber.getClass().getSimpleName(), channel.value());
            } catch (final Exception e) {
                log.warn("Failed to subscribe {} to channel {}. Reason: {}",
                        subscriber.getClass().getSimpleName(), channel.value(), e.getMessage());
            }
        }
    }
}
