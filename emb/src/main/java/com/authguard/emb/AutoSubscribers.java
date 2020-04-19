package com.authguard.emb;

import com.authguard.emb.annotations.Channel;
import com.google.inject.Inject;

import java.util.Set;
import java.util.stream.Collectors;

public class AutoSubscribers {
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

            messageBus.subscribe(channel.value(), subscriber);
        }
    }
}
