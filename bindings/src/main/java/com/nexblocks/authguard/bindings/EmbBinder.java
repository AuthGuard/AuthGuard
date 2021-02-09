package com.nexblocks.authguard.bindings;

import com.nexblocks.authguard.emb.MessagePublisherFactory;
import com.nexblocks.authguard.emb.MessageSubscriber;
import com.nexblocks.authguard.emb.rxjava.RxPublisherFactory;
import com.google.inject.AbstractModule;
import java.util.Collection;
import java.util.Set;

import com.nexblocks.authguard.injection.ClassSearch;
import com.google.inject.multibindings.Multibinder;

public class EmbBinder extends AbstractModule {

    private final DynamicBinder dynamicBinder;

    public EmbBinder(final Collection<String> searchPackages) {
        dynamicBinder = new DynamicBinder(new ClassSearch(searchPackages));
    }

    @Override
    protected void configure() {
        bind(MessagePublisherFactory.class).to(RxPublisherFactory.class);

        final Set<Class<? extends MessageSubscriber>> subscribersClasses =
                dynamicBinder.findAllBindingsFor(MessageSubscriber.class);

        final Multibinder<MessageSubscriber> subscribersMultibinder = Multibinder.newSetBinder(binder(),
                MessageSubscriber.class);

        subscribersClasses.forEach(subscriber -> subscribersMultibinder.addBinding().to(subscriber));
    }
}
