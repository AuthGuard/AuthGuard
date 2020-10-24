package com.authguard.ldap.exchange;

import com.authguard.jwt.IdTokenProvider;
import com.authguard.ldap.LdapService;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "idToken")
public class LdapToIdToken extends LdapExchange implements Exchange {
    @Inject
    public LdapToIdToken(final LdapService ldapService, final IdTokenProvider idTokenProvider) {
        super(ldapService, idTokenProvider);
    }
}
