package com.nexblocks.authguard.emb;

import com.nexblocks.authguard.emb.model.Message;

@FunctionalInterface
public interface MessageSubscriber {
    void onMessage(Message message);
}
