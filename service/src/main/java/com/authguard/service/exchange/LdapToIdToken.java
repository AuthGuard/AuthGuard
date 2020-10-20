package com.authguard.service.exchange;

import com.authguard.jwt.IdTokenProvider;
import com.authguard.ldap.exchange.LdapExchange;
import com.authguard.ldap.LdapService;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "idToken")
public class LdapToIdToken extends LdapExchange implements Exchange {
    @Inject
    public LdapToIdToken(final LdapService ldapService, final IdTokenProvider idTokenProvider) {
        super(ldapService, idTokenProvider);
    }
}
