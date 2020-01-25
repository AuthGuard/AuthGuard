package org.auther.rest.injectors;

import com.google.inject.AbstractModule;
import org.auther.dal.*;
import org.auther.injection.ClassSearch;
import org.reflections.Reflections;

public class DalBinder extends AbstractModule {

    private final DynamicBinder dynamicBinder;

    public DalBinder(final String classSearchPrefix) {
        dynamicBinder = new DynamicBinder(new ClassSearch(new Reflections(classSearchPrefix)));
    }

    @Override
    public void configure() {
        bind(CredentialsRepository.class).to(dynamicBinder.findBindingsFor(CredentialsRepository.class));
        bind(CredentialsAuditRepository.class).to(dynamicBinder.findBindingsFor(CredentialsAuditRepository.class));
        bind(AccountsRepository.class).to(dynamicBinder.findBindingsFor(AccountsRepository.class));
        bind(ApplicationsRepository.class).to(dynamicBinder.findBindingsFor(ApplicationsRepository.class));
        bind(ApiKeysRepository.class).to(dynamicBinder.findBindingsFor(ApiKeysRepository.class));
        bind(PermissionsRepository.class).to(dynamicBinder.findBindingsFor(PermissionsRepository.class));
        bind(RolesRepository.class).to(dynamicBinder.findBindingsFor(RolesRepository.class));
        bind(AccountTokensRepository.class).to(dynamicBinder.findBindingsFor(AccountTokensRepository.class));
        bind(OtpRepository.class).to(dynamicBinder.findBindingsFor(OtpRepository.class));
    }
}
