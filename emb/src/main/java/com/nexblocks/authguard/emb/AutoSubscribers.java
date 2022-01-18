package com.nexblocks.authguard.emb;

import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.emb.annotations.Channel;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class AutoSubscribers {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final MessageBus messageBus;
    private final Set<MessageSubscriber> subscribers;
    private final Set<String> allowedSubscribers;

    @Inject
    public AutoSubscribers(final MessageBus messageBus, final Set<MessageSubscriber> subscribers,
                           final @Named("emb") ConfigContext channelsConfig) {
        this.messageBus = messageBus;
        this.subscribers = subscribersWithChannels(subscribers);

        this.allowedSubscribers = Optional.ofNullable(channelsConfig.getAsCollection("subscribers", String.class))
                .map(l -> (Set<String>) new HashSet<>(l))
                .orElseGet(Collections::emptySet);
    }

    private Set<MessageSubscriber> subscribersWithChannels(final Set<MessageSubscriber> subscribers) {
        return subscribers.stream()
                .filter(subscriber -> subscriber.getClass().getAnnotation(Channel.class) != null)
                .collect(Collectors.toSet());
    }

    public void subscribe() {
        if (allowedSubscribers.isEmpty()) {
            log.info("No subscribers were allowed in the configuration");

            return;
        }

        for (final MessageSubscriber subscriber : subscribers) {
            if (!allowedSubscribers.contains(subscriber.getClass().getCanonicalName())) {
                log.info("Subscriber {} is not in the allowed list", subscriber.getClass().getCanonicalName());

                continue;
            }

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
