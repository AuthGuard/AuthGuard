package com.nexblocks.authguard.emb;

import com.nexblocks.authguard.emb.annotations.Channel;
import com.nexblocks.authguard.emb.model.Message;
import com.google.common.collect.ImmutableSet;
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

    @Test
    void subscribe() {
        final MessageBus messageBus = Mockito.mock(MessageBus.class);
        final ToBeSubscribed toBeSubscribed = new ToBeSubscribed();
        final NotToBeSubscribed notToBeSubscribed = new NotToBeSubscribed();

        final AutoSubscribers autoSubscribers = new AutoSubscribers(messageBus,
                ImmutableSet.of(toBeSubscribed, notToBeSubscribed));

        autoSubscribers.subscribe();

        Mockito.verify(messageBus, Mockito.times(1))
                .subscribe("test", toBeSubscribed);

        Mockito.verify(messageBus, Mockito.never()).subscribe(Mockito.any(), Mockito.eq(notToBeSubscribed));
    }
}