package com.authguard.emb;

import com.authguard.config.ConfigContext;
import com.authguard.emb.model.Message;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class MessageBus {
    private final Map<String, MessagePublisher> channels;
    private final MessagePublisherFactory factory;
    private final boolean createIfMissing;

    @Inject
    public MessageBus(final MessagePublisherFactory factory,
                      final @Named("emb") ConfigContext channelsConfig) {
        final ImmutableMap.Builder<String, MessagePublisher> channelsMapBuilder = ImmutableMap.builder();
        final Collection<String> channelsNames = channelsConfig.getAsCollection("channels", String.class);

        if (channelsNames != null) {
            channelsNames
                    .forEach(channel -> channelsMapBuilder.put(channel, factory.create()));

            this.channels = channelsNames.stream()
                    .collect(Collectors.toMap(Function.identity(), ignored -> factory.create()));

           this.createIfMissing = false;
           this.factory = null; // won't be needed
        } else {
            this.channels = new HashMap<>();
            this.createIfMissing = true;
            this.factory = factory;
        }
    }

    public void publish(final String channel, final Message message) {
        final MessagePublisher publisher = Optional.ofNullable(get(channel))
                .orElseThrow(() -> new IllegalArgumentException("Cannot publish to non-existing channel " + channel));

        publisher.publish(message);
    }

    public void subscribe(final String channel, final MessageSubscriber subscriber) {
        final MessagePublisher publisher = getNullable(channel);

        if (publisher == null) {
            throw new IllegalArgumentException("Cannot subscribe to non-existing channel " + channel);
        }

        publisher.acceptSubscriber(subscriber);
    }

    public MessagePublisher get(final String channel) {
        return createIfMissing ? getOrCreateIfMissing(channel) : getNullable(channel);
    }

    public MessagePublisher getNullable(final String channel) {
        return channels.get(channel);
    }

    private MessagePublisher getOrCreateIfMissing(final String channel) {
        final MessagePublisher publisher = channels.get(channel);

        if (publisher == null) {
            final MessagePublisher createdPublisher = factory.create();

            channels.put(channel, factory.create());

            return createdPublisher;
        } else {
            return publisher;
        }
    }
}
