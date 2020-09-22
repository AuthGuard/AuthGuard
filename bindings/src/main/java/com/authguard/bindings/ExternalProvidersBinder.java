package com.authguard.bindings;

import com.authguard.config.ConfigContext;
import com.authguard.external.email.EmailProvider;
import com.authguard.external.sms.SmsProvider;
import com.authguard.injection.ClassSearch;
import com.google.inject.AbstractModule;

import java.util.Collection;

public class ExternalProvidersBinder extends AbstractModule {
    private final ConfigContext configContext;
    private final DynamicBinder dynamicBinder;

    public ExternalProvidersBinder(final ConfigContext configContext, final Collection<String> searchPackages) {
        this.configContext = configContext;
        dynamicBinder = new DynamicBinder(new ClassSearch(searchPackages));
    }

    @Override
    protected void configure() {
        if (configContext.get("verification") != null) {
            bind(SmsProvider.class).to(dynamicBinder.findBindingsFor(SmsProvider.class));
            bind(EmailProvider.class).to(dynamicBinder.findBindingsFor(EmailProvider.class));
        }
    }
}
