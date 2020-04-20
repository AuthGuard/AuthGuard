package com.authguard.emb.rxjava;

import com.authguard.emb.MessagePublisher;
import com.authguard.emb.MessageSubscriber;
import com.authguard.emb.model.Message;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class RxPublisher implements MessagePublisher {
    private PublishSubject<Message> subject = PublishSubject.create();

    @Override
    public void publish(final Message message) {
        subject.onNext(message);
    }

    @Override
    public void acceptSubscriber(final MessageSubscriber subscriber) {
        subject.subscribe(subscriber::onMessage);
    }
}
