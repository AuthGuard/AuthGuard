package com.authguard.emb;

import com.authguard.emb.model.MessageMO;

public interface MessagePublisher {
    void publish(MessageMO message);
}
