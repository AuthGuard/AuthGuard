package org.auther.rest.injectors;

import com.google.inject.AbstractModule;
import org.auther.dal.*;
import org.auther.dal.mock.*;
import org.auther.dal.model.*;

import java.util.List;
import java.util.Optional;

public class DalBinder extends AbstractModule {

    @Override
    public void configure() {
        bind(CredentialsRepository.class).to(MockCredentialsRepository.class);
        bind(CredentialsAuditRepository.class).to(MockCredentialsAuditRepository.class);
        bind(AccountsRepository.class).to(MockAccountsRepository.class);
        bind(PermissionsRepository.class).to(MockPermissionsRepository.class);
        bind(RolesRepository.class).to(MockRolesRepository.class);
        bind(AccountTokensRepository.class).to(MockAccountsTokensRepository.class);
    }
}
