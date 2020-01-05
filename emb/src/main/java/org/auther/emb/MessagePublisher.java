package org.auther.emb;

import org.auther.emb.model.MessageMO;

public interface MessagePublisher {
    void publish(MessageMO message);
}
