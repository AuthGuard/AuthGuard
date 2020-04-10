package com.authguard.rest.injectors;

import com.authguard.config.ConfigContext;
import com.authguard.service.*;
import com.authguard.service.impl.*;
import com.authguard.service.passwords.SecurePassword;
import com.google.inject.AbstractModule;
import com.authguard.service.passwords.SCryptPassword;

public class ServicesBinder extends AbstractModule {
    private final ConfigContext configContext;

    public ServicesBinder(final ConfigContext configContext) {
        this.configContext = configContext;
    }

    @Override
    public void configure() {
        // essential bindings
        bind(CredentialsService.class).to(CredentialsServiceImpl.class);
        bind(AuthenticationService.class).to(AuthenticationServiceImpl.class);
        bind(ExchangeService.class).to(ExchangeServiceImpl.class);
        bind(AccountsService.class).to(AccountsServiceImpl.class);
        bind(ApplicationsService.class).to(ApplicationsServiceImpl.class);
        bind(ApiKeysService.class).to(ApiKeysServiceImpl.class);
        bind(PermissionsService.class).to(PermissionsServiceImpl.class);
        bind(RolesService.class).to(RolesServiceImpl.class);

        // should be conditional on property value
        bind(SecurePassword.class).to(SCryptPassword.class);

        // optional bindings
        if (configContext.get("verification") != null) {
            bind(VerificationMessageService.class).to(VerificationMessageServiceImpl.class);
            bind(VerificationService.class).to(VerificationServiceImpl.class);
        }

        if (configContext.get("otp") != null) {
            bind(OtpService.class).to(OtpServiceImpl.class);
        }
    }
}
