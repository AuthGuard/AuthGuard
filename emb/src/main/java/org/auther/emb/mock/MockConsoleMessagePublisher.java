package org.auther.emb.mock;

import org.auther.emb.MessagePublisher;
import org.auther.emb.model.MessageMO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockConsoleMessagePublisher implements MessagePublisher {
    private static final Logger LOG = LoggerFactory.getLogger(MockConsoleMessagePublisher.class);

    @Override
    public void publish(final MessageMO message) {
        LOG.info("Event published: {}", message);
    }
}
