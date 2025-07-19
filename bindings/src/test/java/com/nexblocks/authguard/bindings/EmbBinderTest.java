package com.nexblocks.authguard.bindings;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.nexblocks.authguard.emb.MessageSubscriber;
import com.nexblocks.authguard.emb.model.Message;
import com.google.inject.Guice;
import com.google.inject.Inject;
import io.vertx.core.eventbus.EventBus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EmbBinderTest {

    static class SomeSubscriber implements MessageSubscriber {
        @Override
        public void onMessage(final Message message) { }

        @Override
        public boolean equals(final Object obj) {
            return obj.getClass() == this.getClass();
        }
    }

    static class OtherSubscriber implements MessageSubscriber {
        @Override
        public void onMessage(final Message message) { }

        @Override
        public boolean equals(final Object obj) {
            return obj.getClass() == this.getClass();
        }
    }

    static class NeedsSubscribers {
        Set<MessageSubscriber> subscribers;

        @Inject
        public NeedsSubscribers(final Set<MessageSubscriber> subscribers) {
            this.subscribers = subscribers;
        }
    }

    @Test
    void subscribersInjected() {
        EmbBinder embBinder = new EmbBinder(Collections.singletonList("com.nexblocks.authguard.bindings"));
        NeedsSubscribers instance = Guice.createInjector(
                        embBinder, binder -> {
                            binder.bind(EventBus.class).toInstance(Mockito.mock(EventBus.class));
                        })
                .getInstance(NeedsSubscribers.class);

        Set<MessageSubscriber> subscribers = instance.subscribers;

        assertThat(subscribers).containsOnly(new SomeSubscriber(), new OtherSubscriber());
    }

}