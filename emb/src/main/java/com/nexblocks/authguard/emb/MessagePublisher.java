package com.nexblocks.authguard.emb;

import com.nexblocks.authguard.emb.model.Message;

public interface MessagePublisher {
    void publish(Message message);
    void acceptSubscriber(MessageSubscriber subscriber);
}
