package org.auther.rest.injectors;

import com.google.inject.AbstractModule;
import org.auther.emb.MessagePublisher;
import org.auther.injection.ClassSearch;

import java.util.Collection;

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
