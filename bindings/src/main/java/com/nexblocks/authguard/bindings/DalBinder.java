package com.nexblocks.authguard.bindings;

import com.google.inject.AbstractModule;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountLocksRepository;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.cache.OtpRepository;
import com.nexblocks.authguard.dal.cache.SessionsRepository;
import com.nexblocks.authguard.dal.persistence.*;
import com.nexblocks.authguard.injection.ClassSearch;

import java.util.Collection;

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
        bindAndRegister(CredentialsRepository.class);
        bindAndRegister(CredentialsAuditRepository.class);
        bindAndRegister(AccountsRepository.class);
        bindAndRegister(ApplicationsRepository.class);
        bindAndRegister(ApiKeysRepository.class);
        bindAndRegister(PermissionsRepository.class);
        bindAndRegister(RolesRepository.class);
        bindAndRegister(AccountTokensRepository.class);
        bindAndRegister(SessionsRepository.class);
        bindAndRegister(IdempotentRecordsRepository.class);
        bindAndRegister(ExchangeAttemptsRepository.class);
        bindAndRegister(AccountLocksRepository.class);

        // optional bindings
        if (configContext.get("otp") != null) {
            bindAndRegister(OtpRepository.class);
        }

        if (configContext.get("sessions") != null) {
            bindAndRegister(SessionsRepository.class);
        }
    }
    
    private <T> void bindAndRegister(final Class<T> clazz) {
        final Class<? extends T> binding = dynamicBinder.findBindingsFor(clazz);
        
        bind(clazz).to(binding);
        
        PluginsRegistry.register(binding);
    }
}
