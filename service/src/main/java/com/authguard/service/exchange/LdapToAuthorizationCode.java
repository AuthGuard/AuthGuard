package com.authguard.service.exchange;

import com.authguard.ldap.exchange.LdapExchange;
import com.authguard.ldap.LdapService;
import com.authguard.jwt.oauth.AuthorizationCodeProvider;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "authorizationCode")
public class LdapToAuthorizationCode extends LdapExchange implements Exchange {
    @Inject
    public LdapToAuthorizationCode(final LdapService ldapService,
                                   final AuthorizationCodeProvider authorizationCodeProvider) {
        super(ldapService, authorizationCodeProvider);
    }
}
