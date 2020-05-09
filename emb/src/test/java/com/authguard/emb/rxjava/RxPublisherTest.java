package com.authguard.emb.rxjava;

import com.authguard.emb.MessageSubscriber;
import com.authguard.emb.model.EventType;
import com.authguard.emb.model.Message;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RxPublisherTest {

    static class subscriber implements MessageSubscriber {
        List<String> received = new ArrayList<>();

        @Override
        public void onMessage(final Message message) {
            if (message.getBodyType() == String.class) {
                received.add((String) message.getMessageBody());
            }
        }
    }

    @Test
    void publish() throws InterruptedException {
        final RxPublisher publisher = new RxPublisher();
        final subscriber normalSubscriber = new subscriber();
        final subscriber errorSubscriber = new subscriber();

        publisher.acceptSubscriber(normalSubscriber);
        publisher.acceptSubscriber(errorSubscriber);

        for (int i = 0; i < 5; i++) {
            String event = "event_" + i;

            publisher.publish(Message.builder()
                    .eventType(EventType.ENTITY_CREATED)
                    .timestamp(OffsetDateTime.now())
                    .bodyType(String.class)
                    .messageBody(event)
                    .build());
        }

        TimeUnit.SECONDS.sleep(1);

        assertThat(normalSubscriber.received).containsExactly("event_0", "event_1", "event_2", "event_3", "event_4");
        assertThat(errorSubscriber.received).containsExactly("event_0", "event_1", "event_2", "event_3", "event_4");
    }
}