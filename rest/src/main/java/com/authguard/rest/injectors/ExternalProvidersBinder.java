package com.authguard.rest.injectors;

import com.authguard.external.email.EmailProvider;
import com.authguard.external.sms.SmsProvider;
import com.authguard.injection.ClassSearch;
import com.google.inject.AbstractModule;

import java.util.Collection;

public class ExternalProvidersBinder extends AbstractModule {
    private final DynamicBinder dynamicBinder;

    public ExternalProvidersBinder(final Collection<String> searchPackages) {
        dynamicBinder = new DynamicBinder(new ClassSearch(searchPackages));
    }

    @Override
    protected void configure() {
        bind(SmsProvider.class).to(dynamicBinder.findBindingsFor(SmsProvider.class));
        bind(EmailProvider.class).to(dynamicBinder.findBindingsFor(EmailProvider.class));
    }
}
