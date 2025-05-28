package com.nexblocks.authguard.jwt.exchange;

import com.nexblocks.authguard.basic.passwordless.PasswordlessVerifier;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.google.inject.Inject;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

import io.smallrye.mutiny.Uni;

@TokenExchange(from = "passwordless", to = "accessToken")
public class PasswordlessToAccessToken implements Exchange {
    private final AccountsService accountsService;
    private final PasswordlessVerifier passwordlessVerifier;
    private final AccessTokenProvider accessTokenProvider;

    @Inject
    public PasswordlessToAccessToken(final AccountsService accountsService,
                                     final PasswordlessVerifier passwordlessVerifier,
                                     final AccessTokenProvider accessTokenProvider) {
        this.accountsService = accountsService;
        this.passwordlessVerifier = passwordlessVerifier;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        return passwordlessVerifier.verifyAccountTokenAsync(request)
                .flatMap(id -> accountsService.getById(id, request.getDomain()))
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(
                                new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "The account associated with that token does not exist"));
                    }

                    return generate(opt.get(), request);
                });
    }

    private Uni<AuthResponseBO> generate(final AccountBO account, final AuthRequestBO request) {
        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("passwordless")
                .userAgent(request.getUserAgent())
                .sourceIp(request.getSourceIp())
                .clientId(request.getClientId())
                .externalSessionId(request.getExternalSessionId())
                .deviceId(request.getDeviceId())
                .build();

        return accessTokenProvider.generateToken(account, options);
    }
}
