package com.nexblocks.authguard.emb.rxjava;

import com.nexblocks.authguard.emb.MessagePublisher;
import com.nexblocks.authguard.emb.MessageSubscriber;
import com.nexblocks.authguard.emb.model.Message;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class RxPublisher implements MessagePublisher {
    private static final Logger LOG = LoggerFactory.getLogger(RxPublisher.class);

    private final String channel;
    private final PublishSubject<Message> subject = PublishSubject.create();

    public RxPublisher(final String channel) {
        this.channel = channel;
    }

    @Override
    public void publish(final Message message) {
        subject.onNext(message);
    }

    @Override
    public void acceptSubscriber(final MessageSubscriber subscriber) {
        subject.observeOn(Schedulers.io())
                .subscribe(safeConsumer(subscriber));
    }

    private Consumer<Message> safeConsumer(final MessageSubscriber subscriber) {
        return message -> {
            try {
                subscriber.onMessage(message.withChannel(channel));
            } catch (Throwable e) {
                LOG.warn("Subscriber {} threw an exception. This violates the message subscriber specifications and " +
                        "needs to be fixed", subscriber.getClass(), e);
            }
        };
    }
}
