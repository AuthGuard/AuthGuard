package com.nexblocks.authguard.emb.rxjava;

import com.nexblocks.authguard.emb.MessagePublisher;
import com.nexblocks.authguard.emb.MessagePublisherFactory;

public class RxPublisherFactory implements MessagePublisherFactory {
    @Override
    public MessagePublisher create() {
        return new RxPublisher();
    }
}
