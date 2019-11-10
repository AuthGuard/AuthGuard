package org.auther.api.injectors;

import com.google.inject.AbstractModule;
import org.auther.service.*;
import org.auther.service.impl.*;
import org.auther.service.impl.passwords.SCryptPassword;

public class ServicesBinder extends AbstractModule {

    @Override
    public void configure() {
        bind(CredentialsService.class).to(CredentialsServiceImpl.class);
        bind(AuthenticationService.class).to(AuthenticationServiceImpl.class);
        bind(AuthorizationService.class).to(AuthorizationServiceImpl.class);
        bind(AccountsService.class).to(AccountsServiceImpl.class);
        bind(PermissionsService.class).to(PermissionsServiceImpl.class);
        bind(RolesService.class).to(RolesServiceImpl.class);
        bind(SecurePassword.class).to(SCryptPassword.class);
    }
}
