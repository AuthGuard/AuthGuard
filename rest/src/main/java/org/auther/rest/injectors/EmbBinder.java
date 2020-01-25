package org.auther.rest.injectors;

import com.google.inject.AbstractModule;
import org.auther.emb.MessagePublisher;
import org.auther.injection.ClassSearch;
import org.reflections.Reflections;

public class EmbBinder extends AbstractModule {

    private final DynamicBinder dynamicBinder;

    public EmbBinder(final String classSearchPrefix) {
        dynamicBinder = new DynamicBinder(new ClassSearch(new Reflections(classSearchPrefix)));
    }

    @Override
    protected void configure() {
        bind(MessagePublisher.class).to(dynamicBinder.findBindingsFor(MessagePublisher.class));
    }
}
