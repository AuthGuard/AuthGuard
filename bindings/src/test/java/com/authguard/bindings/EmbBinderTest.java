package com.authguard.bindings;

import com.authguard.emb.MessageSubscriber;
import com.authguard.emb.model.Message;
import com.google.inject.Guice;
import com.google.inject.Inject;
import org.junit.jupiter.api.Test;

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
        final Set<MessageSubscriber> subscribers;

        @Inject
        public NeedsSubscribers(final Set<MessageSubscriber> subscribers) {
            this.subscribers = subscribers;
        }
    }

    @Test
    void subscribersInjected() {
        final EmbBinder embBinder = new EmbBinder(Collections.singletonList("com.authguard.bindings"));
        final NeedsSubscribers instance = Guice.createInjector(embBinder)
                .getInstance(NeedsSubscribers.class);

        final Set<MessageSubscriber> subscribers = instance.subscribers;

        assertThat(subscribers).containsOnly(new SomeSubscriber(), new OtherSubscriber());
    }

}