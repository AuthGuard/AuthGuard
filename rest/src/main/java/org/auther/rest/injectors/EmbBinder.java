package org.auther.rest.injectors;

import com.google.inject.AbstractModule;
import org.auther.emb.MessagePublisher;
import org.auther.emb.mock.MockConsoleMessagePublisher;

public class EmbBinder extends AbstractModule {
    @Override
    protected void configure() {
        bind(MessagePublisher.class).to(MockConsoleMessagePublisher.class);
    }
}
