package org.auther.rest;

import com.google.inject.AbstractModule;
import org.auther.service.*;
import org.mockito.Mockito;

public class MocksBinder extends AbstractModule {
    @Override
    protected void configure() {
        bind(AccountsService.class).toInstance(Mockito.mock(AccountsService.class));
        bind(AuthenticationService.class).toInstance(Mockito.mock(AuthenticationService.class));
        bind(AuthorizationService.class).toInstance(Mockito.mock(AuthorizationService.class));
        bind(CredentialsService.class).toInstance(Mockito.mock(CredentialsService.class));
        bind(PermissionsService.class).toInstance(Mockito.mock(PermissionsService.class));
        bind(RolesService.class).toInstance(Mockito.mock(RolesService.class));
    }
}
