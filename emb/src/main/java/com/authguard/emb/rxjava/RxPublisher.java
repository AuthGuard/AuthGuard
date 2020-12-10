package com.authguard.emb.rxjava;

import com.authguard.emb.MessagePublisher;
import com.authguard.emb.MessageSubscriber;
import com.authguard.emb.model.Message;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RxPublisher implements MessagePublisher {
    private static final Logger LOG = LoggerFactory.getLogger(RxPublisher.class);

    private PublishSubject<Message> subject = PublishSubject.create();

    @Override
    public void publish(final Message message) {
        subject.onNext(message);
    }

    @Override
    public void acceptSubscriber(final MessageSubscriber subscriber) {
        subject.subscribe(safeConsumer(subscriber));
    }

    private Consumer<Message> safeConsumer(MessageSubscriber subscriber) {
        return message -> {
            try {
                subscriber.onMessage(message);
            } catch (Throwable e) {
                LOG.warn("Subscriber {} threw an exception. This violates the message subscriber specifications and " +
                        "needs to be fixed", subscriber.getClass(), e);
            }
        };
    }
}
