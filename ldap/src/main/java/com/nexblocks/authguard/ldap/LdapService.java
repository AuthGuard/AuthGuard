package com.nexblocks.authguard.ldap;

import com.nexblocks.authguard.service.model.AccountBO;

public interface LdapService {
    AccountBO authenticate(String username, String password);
}
