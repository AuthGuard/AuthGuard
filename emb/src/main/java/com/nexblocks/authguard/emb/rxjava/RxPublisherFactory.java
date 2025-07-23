package com.nexblocks.authguard.emb.rxjava;

import com.nexblocks.authguard.emb.MessagePublisher;
import com.nexblocks.authguard.emb.MessagePublisherFactory;

@Deprecated
public class RxPublisherFactory implements MessagePublisherFactory {
    @Override
    public MessagePublisher create(final String channel) {
        return new RxPublisher(channel);
    }
}
