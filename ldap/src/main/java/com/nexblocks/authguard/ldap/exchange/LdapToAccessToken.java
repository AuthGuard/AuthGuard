package com.nexblocks.authguard.ldap.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.ldap.UnboundLdapService;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;

@TokenExchange(from = "ldap", to = "accessToken")
public class LdapToAccessToken extends LdapExchange implements Exchange {
    @Inject
    public LdapToAccessToken(final UnboundLdapService ldapService, final AccessTokenProvider accessTokenProvider) {
        super(ldapService, accessTokenProvider);
    }
}
