package com.authguard.ldap.exchange;

import com.authguard.jwt.AccessTokenProvider;
import com.authguard.ldap.LdapService;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "accessToken")
public class LdapToAccessToken extends LdapExchange implements Exchange {
    @Inject
    public LdapToAccessToken(final LdapService ldapService, final AccessTokenProvider accessTokenProvider) {
        super(ldapService, accessTokenProvider);
    }
}
