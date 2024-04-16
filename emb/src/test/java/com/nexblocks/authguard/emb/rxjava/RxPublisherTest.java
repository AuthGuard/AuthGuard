package com.nexblocks.authguard.emb.rxjava;

import com.nexblocks.authguard.emb.MessageSubscriber;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RxPublisherTest {

    static class Subscriber implements MessageSubscriber {
        List<String> received = new ArrayList<>();

        @Override
        public void onMessage(final Message message) {
            if (message.getBodyType() == String.class) {
                received.add((String) message.getMessageBody());
            }
        }
    }

    static class ExceptionSubscriber implements MessageSubscriber {
        List<String> received = new ArrayList<>();

        @Override
        public void onMessage(final Message message) {
            if (message.getBodyType() == String.class) {
                received.add((String) message.getMessageBody());
            }

            throw new RuntimeException("Fail");
        }
    }

    @Test
    void publish() throws InterruptedException {
        final RxPublisher publisher = new RxPublisher("tests");
        final Subscriber normalSubscriber = new Subscriber();
        final Subscriber errorSubscriber = new Subscriber();

        publisher.acceptSubscriber(normalSubscriber);
        publisher.acceptSubscriber(errorSubscriber);

        for (int i = 0; i < 5; i++) {
            String event = "event_" + i;

            publisher.publish(Message.builder()
                    .eventType(EventType.ENTITY_CREATED)
                    .timestamp(Instant.now())
                    .bodyType(String.class)
                    .messageBody(event)
                    .build());
        }

        TimeUnit.SECONDS.sleep(1);

        assertThat(normalSubscriber.received).containsExactly("event_0", "event_1", "event_2", "event_3", "event_4");
        assertThat(errorSubscriber.received).containsExactly("event_0", "event_1", "event_2", "event_3", "event_4");
    }

    @Test
    void publishException() throws InterruptedException {
        final RxPublisher publisher = new RxPublisher("tests");
        final ExceptionSubscriber errorSubscriber = new ExceptionSubscriber();

        publisher.acceptSubscriber(errorSubscriber);

        for (int i = 0; i < 5; i++) {
            String event = "event_" + i;

            publisher.publish(Message.builder()
                    .eventType(EventType.ENTITY_CREATED)
                    .timestamp(Instant.now())
                    .bodyType(String.class)
                    .messageBody(event)
                    .build());
        }

        TimeUnit.SECONDS.sleep(1);

        // we're testing here that an exception didn't terminate the message stream
        assertThat(errorSubscriber.received).containsExactly("event_0", "event_1", "event_2", "event_3", "event_4");
    }
}