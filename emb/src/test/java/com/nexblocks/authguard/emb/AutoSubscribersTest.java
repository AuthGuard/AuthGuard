package com.nexblocks.authguard.emb;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.config.JacksonConfigContext;
import com.nexblocks.authguard.emb.annotations.Channel;
import com.nexblocks.authguard.emb.model.Message;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AutoSubscribersTest {

    @Channel("test")
    static class ToBeSubscribed implements MessageSubscriber {
        @Override
        public void onMessage(final Message message) { }
    }

    static class NotToBeSubscribed implements MessageSubscriber {
        @Override
        public void onMessage(final Message message) { }
    }

    @Channel("test")
    static class NotAllowed implements MessageSubscriber {
        @Override
        public void onMessage(final Message message) { }
    }

    @Test
    void subscribe() {
        final ObjectNode configRoot = new ObjectNode(JsonNodeFactory.instance);
        final ArrayNode allowed = new ArrayNode(JsonNodeFactory.instance)
                .add("com.nexblocks.authguard.emb.AutoSubscribersTest.ToBeSubscribed")
                .add("com.nexblocks.authguard.emb.AutoSubscribersTest.NotToBeSubscribed");

        configRoot.set("subscribers", allowed);

        final MessageBus messageBus = Mockito.mock(MessageBus.class);
        final ToBeSubscribed toBeSubscribed = new ToBeSubscribed();
        final NotToBeSubscribed notToBeSubscribed = new NotToBeSubscribed();
        final ConfigContext configContext = new JacksonConfigContext(configRoot);

        final AutoSubscribers autoSubscribers = new AutoSubscribers(messageBus,
                ImmutableSet.of(toBeSubscribed, notToBeSubscribed), configContext);

        autoSubscribers.subscribe();

        Mockito.verify(messageBus, Mockito.times(1))
                .subscribe("test", toBeSubscribed);

        Mockito.verify(messageBus, Mockito.never()).subscribe(Mockito.any(), Mockito.eq(notToBeSubscribed));
    }
}