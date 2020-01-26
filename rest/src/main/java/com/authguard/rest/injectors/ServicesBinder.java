package com.authguard.rest.injectors;

import com.authguard.config.ConfigContext;
import com.authguard.service.*;
import com.authguard.service.impl.*;
import com.google.inject.AbstractModule;
import com.authguard.service.*;
import com.authguard.service.impl.passwords.SCryptPassword;

public class ServicesBinder extends AbstractModule {

    private final ConfigContext rootConfig;

    public ServicesBinder(final ConfigContext rootConfig) {
        this.rootConfig = rootConfig;
    }

    @Override
    public void configure() {
        bind(CredentialsService.class).to(CredentialsServiceImpl.class);
        bind(AuthenticationService.class).to(AuthenticationServiceImpl.class);
        bind(AuthorizationService.class).to(AuthorizationServiceImpl.class);
        bind(AccountsService.class).to(AccountsServiceImpl.class);
        bind(ApplicationsService.class).to(ApplicationsServiceImpl.class);
        bind(ApiKeysService.class).to(ApiKeysServiceImpl.class);
        bind(PermissionsService.class).to(PermissionsServiceImpl.class);
        bind(RolesService.class).to(RolesServiceImpl.class);

        bind(SecurePassword.class).to(SCryptPassword.class);

        if (rootConfig.get("otp") != null) {
            bind(OtpService.class).to(OtpServiceImpl.class);
        }
    }
}
