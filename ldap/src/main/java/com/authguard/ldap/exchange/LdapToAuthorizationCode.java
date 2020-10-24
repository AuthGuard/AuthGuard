package com.authguard.ldap.exchange;

import com.authguard.jwt.oauth.AuthorizationCodeProvider;
import com.authguard.ldap.LdapService;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "authorizationCode")
public class LdapToAuthorizationCode extends LdapExchange implements Exchange {
    @Inject
    public LdapToAuthorizationCode(final LdapService ldapService,
                                   final AuthorizationCodeProvider authorizationCodeProvider) {
        super(ldapService, authorizationCodeProvider);
    }
}
