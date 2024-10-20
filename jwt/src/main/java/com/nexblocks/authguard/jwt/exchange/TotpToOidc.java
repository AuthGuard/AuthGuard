package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.totp.TotpVerifier;
import com.nexblocks.authguard.jwt.OpenIdConnectTokenProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.*;

import java.util.concurrent.CompletableFuture;

@TokenExchange(from = "totp", to = "oidc")
public class TotpToOidc implements Exchange {
    private final AccountsService accountsService;
    private final TotpVerifier totpVerifier;
    private final OpenIdConnectTokenProvider openIdConnectTokenProvider;
    private final ServiceMapper serviceMapper;

    @Inject
    public TotpToOidc(final AccountsService accountsService,
                      final TotpVerifier totpVerifier,
                      final OpenIdConnectTokenProvider openIdConnectTokenProvider,
                      final ServiceMapper serviceMapper) {
        this.accountsService = accountsService;
        this.totpVerifier = totpVerifier;
        this.openIdConnectTokenProvider = openIdConnectTokenProvider;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        return totpVerifier.verifyAndGetAccountTokenAsync(request)
                .thenCompose(accountToken -> accountsService.getById(accountToken.getAssociatedAccountId(), request.getDomain())
                        .thenCompose(opt -> {
                            if (opt.isEmpty()) {
                                return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                                        "Failed to generate access token"));
                            }

                            return generateTokens(opt.get(),
                                    serviceMapper.toBO(accountToken.getTokenRestrictions()));
                        }));
    }

    private CompletableFuture<AuthResponseBO> generateTokens(final AccountBO account, final TokenRestrictionsBO restrictions) {
        final TokenOptionsBO options = TokenOptionsBO.builder()
                .source("totp")
                .build();

        return openIdConnectTokenProvider.generateToken(account, restrictions, options);
    }
}
