package com.authguard.rest.injectors;

import com.authguard.service.*;
import com.authguard.service.exchange.*;
import com.authguard.service.impl.*;
import com.authguard.service.passwords.SecurePassword;
import com.google.inject.AbstractModule;
import com.authguard.service.passwords.SCryptPassword;
import com.google.inject.Provides;

import java.util.Arrays;
import java.util.List;

public class ServicesBinder extends AbstractModule {

    @Override
    public void configure() {
        bind(CredentialsService.class).to(CredentialsServiceImpl.class);
        bind(AuthenticationService.class).to(AuthenticationServiceImpl.class);
        bind(ExchangeService.class).to(ExchangeServiceImpl.class);
        bind(AccountsService.class).to(AccountsServiceImpl.class);
        bind(ApplicationsService.class).to(ApplicationsServiceImpl.class);
        bind(ApiKeysService.class).to(ApiKeysServiceImpl.class);
        bind(PermissionsService.class).to(PermissionsServiceImpl.class);
        bind(RolesService.class).to(RolesServiceImpl.class);
        bind(VerificationMessageService.class).to(VerificationMessageServiceImpl.class);
        bind(VerificationService.class).to(VerificationServiceImpl.class);
        bind(OtpService.class).to(OtpServiceImpl.class);

        bind(SecurePassword.class).to(SCryptPassword.class);
    }

    @Provides
    List<Exchange> exchanges(final BasicToAccessToken basicToAccessToken,
                             final BasicToIdToken basicToIdToken,
                             final BasicToOtp basicToOtp,
                             final RefreshToAccessToken refreshToAccessToken) {
        return Arrays.asList(basicToAccessToken, basicToIdToken, basicToOtp, refreshToAccessToken);
    }

}
