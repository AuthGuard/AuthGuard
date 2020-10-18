package com.authguard.service.exchange;

import com.authguard.jwt.AccessTokenProvider;
import com.authguard.ldap.exchange.LdapExchange;
import com.authguard.ldap.LdapService;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "accessToken")
public class LdapToAccessToken extends LdapExchange implements Exchange {
    @Inject
    public LdapToAccessToken(final LdapService ldapService, final AccessTokenProvider accessTokenProvider) {
        super(ldapService, accessTokenProvider);
    }
}
