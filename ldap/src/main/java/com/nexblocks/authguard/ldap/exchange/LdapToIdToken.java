package com.nexblocks.authguard.ldap.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.jwt.IdTokenProvider;
import com.nexblocks.authguard.ldap.UnboundLdapService;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;

@TokenExchange(from = "ldap", to = "idToken")
public class LdapToIdToken extends LdapExchange implements Exchange {
    @Inject
    public LdapToIdToken(final UnboundLdapService ldapService, final IdTokenProvider idTokenProvider) {
        super(ldapService, idTokenProvider);
    }
}
