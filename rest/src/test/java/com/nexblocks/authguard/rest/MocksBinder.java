package com.nexblocks.authguard.rest;

import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.emb.AutoSubscribers;
import com.nexblocks.authguard.service.*;
import com.google.inject.AbstractModule;
import com.nexblocks.authguard.service.*;
import org.mockito.Mockito;

public class MocksBinder extends AbstractModule {
    @Override
    protected void configure() {
        bind(AccountsService.class).toInstance(Mockito.mock(AccountsService.class));
        bind(ApplicationsService.class).toInstance(Mockito.mock(ApplicationsService.class));
        bind(AuthenticationService.class).toInstance(Mockito.mock(AuthenticationService.class));
        bind(OtpService.class).toInstance(Mockito.mock(OtpService.class));
        bind(PasswordlessService.class).toInstance(Mockito.mock(PasswordlessService.class));
        bind(ApiKeysService.class).toInstance(Mockito.mock(ApiKeysService.class));
        bind(CredentialsService.class).toInstance(Mockito.mock(CredentialsService.class));
        bind(PermissionsService.class).toInstance(Mockito.mock(PermissionsService.class));
        bind(RolesService.class).toInstance(Mockito.mock(RolesService.class));
        bind(VerificationService.class).toInstance(Mockito.mock(VerificationService.class));
        bind(ExchangeService.class).toInstance(Mockito.mock(ExchangeService.class));
        bind(BasicAuthProvider.class).toInstance(Mockito.mock(BasicAuthProvider.class));
        bind(AutoSubscribers.class).toInstance(Mockito.mock(AutoSubscribers.class));
        bind(IdempotencyService.class).toInstance(Mockito.mock(IdempotencyService.class));
        bind(ExchangeAttemptsService.class).toInstance(Mockito.mock(ExchangeAttemptsService.class));
        bind(AccountLocksService.class).toInstance(Mockito.mock(AccountLocksService.class));
    }
}
