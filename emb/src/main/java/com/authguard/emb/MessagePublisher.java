package com.authguard.emb;

import com.authguard.emb.model.Message;

public interface MessagePublisher {
    void publish(Message message);
    void acceptSubscriber(MessageSubscriber subscriber);
}
