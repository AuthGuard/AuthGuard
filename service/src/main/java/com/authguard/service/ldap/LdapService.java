package com.authguard.service.ldap;

import com.authguard.service.model.AccountBO;

public interface LdapService {
    AccountBO authenticate(String username, String password);
}
