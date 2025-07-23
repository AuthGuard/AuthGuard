package com.nexblocks.authguard.emb;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.emb.model.Message;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class MessageBus {
    private static final Logger LOG = LoggerFactory.getLogger(MessageBus.class);

    private final Map<String, MessagePublisher> channels;
    private final MessagePublisherFactory factory;
    private final boolean createIfMissing;

    private final List<MessageSubscriber> globalSubscribers;

    @Inject
    public MessageBus(final MessagePublisherFactory factory,
                      final @Named("emb") ConfigContext channelsConfig) {
        ImmutableMap.Builder<String, MessagePublisher> channelsMapBuilder = ImmutableMap.builder();
        Collection<String> channelsNames = channelsConfig.getAsCollection("channels", String.class);

        if (channelsNames != null) {
            channelsNames
                    .forEach(channel -> channelsMapBuilder.put(channel, factory.create(channel)));

            this.channels = channelsNames.stream()
                    .collect(Collectors.toMap(Function.identity(), factory::create));

           this.createIfMissing = false;
           this.factory = null; // won't be needed
        } else {
            this.channels = new HashMap<>();
            this.createIfMissing = true;
            this.factory = factory;
        }

        this.globalSubscribers = new ArrayList<>();
    }

    public void publish(final String channel, final Message<?> message) {
        MessagePublisher publisher = get(channel);

        if (publisher == null) {
            LOG.debug("Attempt to publish to non-existing channel {}", channel);
        } else {
            publisher.publish(message);
        }
    }

    public void subscribe(final String channel, final MessageSubscriber subscriber) {
        if (isGlobal(channel)) {
            subscribeToAll(subscriber);
        } else {
            subscribeToChannel(channel, subscriber);
        }
    }

    public MessagePublisher get(final String channel) {
        return createIfMissing ? getOrCreateIfMissing(channel) : getNullable(channel);
    }

    public MessagePublisher getNullable(final String channel) {
        return channels.get(channel);
    }

    private MessagePublisher getOrCreateIfMissing(final String channel) {
        MessagePublisher publisher = channels.get(channel);

        if (publisher == null) {
            final MessagePublisher createdPublisher = factory.create(null);

            channels.put(channel, factory.create(null));

            updateGlobalSubscribers(createdPublisher);

            return createdPublisher;
        } else {
            return publisher;
        }
    }

    private boolean isGlobal(final String channel) {
        return channel.equals("*");
    }

    private void subscribeToChannel(final String channel, final MessageSubscriber subscriber) {
        MessagePublisher publisher = getNullable(channel);

        if (publisher == null) {
            throw new IllegalArgumentException("Cannot subscribe to non-existing channel " + channel);
        }

        publisher.acceptSubscriber(subscriber);
    }

    private void subscribeToAll(final MessageSubscriber subscriber) {
        channels.values().forEach(publisher -> publisher.acceptSubscriber(subscriber));
        globalSubscribers.add(subscriber); // so that we can add new channels if new channels were to be created
    }

    private void updateGlobalSubscribers(final MessagePublisher publisher) {
        globalSubscribers.forEach(publisher::acceptSubscriber);
    }
}
