package com.authguard.emb.rxjava;

import com.authguard.emb.MessagePublisher;
import com.authguard.emb.MessagePublisherFactory;

public class RxPublisherFactory implements MessagePublisherFactory {
    @Override
    public MessagePublisher create() {
        return new RxPublisher();
    }
}
