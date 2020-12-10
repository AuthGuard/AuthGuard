package com.authguard.bindings;

import com.authguard.config.ConfigContext;
import com.authguard.dal.*;

import java.util.Collection;
import com.authguard.injection.ClassSearch;
import com.google.inject.AbstractModule;

public class DalBinder extends AbstractModule {
    private final ConfigContext configContext;
    private final DynamicBinder dynamicBinder;

    public DalBinder(final ConfigContext configContext, final Collection<String> searchPackages) {
        this.configContext = configContext;
        this.dynamicBinder = new DynamicBinder(new ClassSearch(searchPackages));
    }

    @Override
    public void configure() {
        // essential bindings
        bind(CredentialsRepository.class).to(dynamicBinder.findBindingsFor(CredentialsRepository.class));
        bind(CredentialsAuditRepository.class).to(dynamicBinder.findBindingsFor(CredentialsAuditRepository.class));
        bind(AccountsRepository.class).to(dynamicBinder.findBindingsFor(AccountsRepository.class));
        bind(ApplicationsRepository.class).to(dynamicBinder.findBindingsFor(ApplicationsRepository.class));
        bind(ApiKeysRepository.class).to(dynamicBinder.findBindingsFor(ApiKeysRepository.class));
        bind(PermissionsRepository.class).to(dynamicBinder.findBindingsFor(PermissionsRepository.class));
        bind(RolesRepository.class).to(dynamicBinder.findBindingsFor(RolesRepository.class));
        bind(AccountTokensRepository.class).to(dynamicBinder.findBindingsFor(AccountTokensRepository.class));
        bind(IdempotentRecordsRepository.class).to(dynamicBinder.findBindingsFor(IdempotentRecordsRepository.class));
        bind(ExchangeAttemptsRepository.class).to(dynamicBinder.findBindingsFor(ExchangeAttemptsRepository.class));
        bind(AccountLocksRepository.class).to(dynamicBinder.findBindingsFor(AccountLocksRepository.class));

        // optional bindings
        if (configContext.get("otp") != null) {
            bind(OtpRepository.class).to(dynamicBinder.findBindingsFor(OtpRepository.class));
        }

        if (configContext.get("sessions") != null) {
            bind(SessionsRepository.class).to(dynamicBinder.findBindingsFor(SessionsRepository.class));
        }
    }
}
