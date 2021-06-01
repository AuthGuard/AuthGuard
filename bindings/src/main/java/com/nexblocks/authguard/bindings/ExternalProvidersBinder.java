package com.nexblocks.authguard.bindings;

import com.google.inject.AbstractModule;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.external.email.EmailProvider;
import com.nexblocks.authguard.external.sms.SmsProvider;
import com.nexblocks.authguard.injection.ClassSearch;

import java.util.Collection;
import java.util.Optional;

public class ExternalProvidersBinder extends AbstractModule {
    private final ConfigContext configContext;
    private final DynamicBinder dynamicBinder;

    public ExternalProvidersBinder(final ConfigContext configContext, final Collection<String> searchPackages) {
        this.configContext = configContext;
        dynamicBinder = new DynamicBinder(new ClassSearch(searchPackages));
    }

    @Override
    protected void configure() {
        final ConfigContext subContext = configContext.getSubContext("external");

        Optional.ofNullable(subContext)
                .map(context -> context.getAsBoolean("email"))
                .filter(Boolean::booleanValue)
                .ifPresent(ignored -> bindAndRegister(EmailProvider.class));

        Optional.ofNullable(subContext)
                .map(context -> context.getAsBoolean("sms"))
                .filter(Boolean::booleanValue)
                .ifPresent(ignored -> bindAndRegister(SmsProvider.class));
    }

    private <T> void bindAndRegister(final Class<T> clazz) {
        final Class<? extends T> binding = dynamicBinder.findBindingsFor(clazz);

        bind(clazz).to(binding);

        PluginsRegistry.register(binding);
    }
}
