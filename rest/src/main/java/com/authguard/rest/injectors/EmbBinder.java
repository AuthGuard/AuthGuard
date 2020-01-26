package com.authguard.rest.injectors;

import com.google.inject.AbstractModule;
import java.util.Collection;
import com.authguard.emb.MessagePublisher;
import com.authguard.injection.ClassSearch;

public class EmbBinder extends AbstractModule {

    private final DynamicBinder dynamicBinder;

    public EmbBinder(final Collection<String> searchPackages) {
        dynamicBinder = new DynamicBinder(new ClassSearch(searchPackages));
    }

    @Override
    protected void configure() {
        bind(MessagePublisher.class).to(dynamicBinder.findBindingsFor(MessagePublisher.class));
    }
}
