package com.authguard.service.ldap;

import com.authguard.service.auth.AuthProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.helpers.TokensUtils;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.TokensBO;

import java.util.Optional;

public abstract class LdapExchange implements Exchange {
    private final LdapService ldapService;
    private final AuthProvider authProvider;

    protected LdapExchange(final LdapService ldapService, final AuthProvider authProvider) {
        this.ldapService = ldapService;
        this.authProvider = authProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String basicToken) {
        final String[] credentials = TokensUtils.decodeAndSplitBasic(basicToken);
        final AccountBO account = ldapService.authenticate(credentials[0], credentials[1]);

        return Optional.of(authProvider.generateToken(account));
    }
}
