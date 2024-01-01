package com.nexblocks.authguard.ldap.exchange;

import com.nexblocks.authguard.ldap.LdapService;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.mappers.TokenOptionsMapper;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import java.util.concurrent.CompletableFuture;

public abstract class LdapExchange implements Exchange {
    private final LdapService ldapService;
    private final AuthProvider authProvider;

    protected LdapExchange(final LdapService ldapService, final AuthProvider authProvider) {
        this.ldapService = ldapService;
        this.authProvider = authProvider;
    }

    @Override
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        final AccountBO account = ldapService.authenticate(request.getIdentifier(), request.getPassword());
        final TokenOptionsBO tokenOptions = TokenOptionsMapper.fromAuthRequest(request);

        return CompletableFuture.completedFuture(authProvider.generateToken(account, tokenOptions));
    }
}
